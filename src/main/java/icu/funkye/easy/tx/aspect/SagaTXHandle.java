package icu.funkye.easy.tx.aspect;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import icu.funkye.easy.tx.config.EasyTxMode;
import icu.funkye.easy.tx.util.SpringProxyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.config.annotation.SagaTransaction;
import icu.funkye.easy.tx.entity.SagaBranchTransaction;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.SetParams;

import static icu.funkye.easy.tx.constant.EasyTxConstant.PREFIX_SAGA_TX;
import static icu.funkye.easy.tx.constant.EasyTxConstant.PREFIX_SAGA_TASK;
import static icu.funkye.easy.tx.constant.EasyTxConstant.PREFIX_TX;

/**
 * @author chenjianbin
 * @version 1.0.0
 */
@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true",
    matchIfMissing = true)
@Order(value = Ordered.HIGHEST_PRECEDENCE + 1)
@Aspect
@Component
public class SagaTXHandle implements ApplicationContextAware {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    JedisPool jedisEasyTxPool;

    ApplicationContext applicationContext;

    @Resource
    EasyTxProperties easyTxProperties;

    ThreadFactoryBuilder threadFactoryBuilder =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("saga-pool-%d");

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder.build());

    public SagaTXHandle() {
        executor.scheduleAtFixedRate(() -> {
            try {
                Set<String> txs;
                try (Jedis jedis = jedisEasyTxPool.getResource()) {
                    txs = jedis.keys(PREFIX_SAGA_TX + "*");
                }
                if (CollectionUtils.isNotEmpty(txs)) {
                    txs.parallelStream().forEach(key -> {
                        try (Jedis jedis = jedisEasyTxPool.getResource()) {
                            Map<String, String> branchs = jedis.hgetAll(key);
                            if (!branchs.isEmpty()) {
                                Map<String, Map<String, String>> globalTXs = new ConcurrentHashMap<>();
                                // 排序
                                List<SagaBranchTransaction> branchList = branchs.values().parallelStream()
                                    .map(i -> JSONObject.parseObject(i, SagaBranchTransaction.class))
                                    .sorted(Comparator.comparingLong(i -> i.getCreateTime().getTime()))
                                    .collect(Collectors.toList());
                                for (SagaBranchTransaction branchTransaction : branchList) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("开始补偿SAGA事务: {}", key);
                                    }
                                    String txKey = PREFIX_TX + branchTransaction.getXid();
                                    Map<String, String> globalTX = globalTXs.computeIfAbsent(branchTransaction.getXid(),
                                        k -> jedis.hgetAll(txKey));
                                    boolean status = Boolean.parseBoolean(globalTX.get("status"));
                                    if (!status) {
                                        // 只补偿自身
                                        if (branchTransaction.getClientId()
                                            .equalsIgnoreCase(easyTxProperties.getClientId())) {
                                            String taskKey = (PREFIX_SAGA_TASK + branchTransaction.getBranchId());
                                            String owner = UUID.randomUUID().toString();
                                            boolean result = StringUtils.equalsIgnoreCase(
                                                jedis.set(taskKey, owner, SetParams.setParams().nx().ex(60)), "OK");
                                            try {
                                                // 抢到锁,得到此事务的补偿权利
                                                if (result) {
                                                    // 分支状态为false进行补偿
                                                    if (!branchTransaction.isStatus()) {
                                                        // 判断分支事务是否已经超时
                                                        if (System.currentTimeMillis()
                                                            - branchTransaction.getModifyTime()
                                                                .getTime() > branchTransaction.getRetryInterval()) {
                                                            try {
                                                                // 获取confirm的bean
                                                                Object confirmBean = applicationContext
                                                                    .getBean(branchTransaction.getConfirmBeanName());
                                                                Method confirmMethod = SpringProxyUtils
                                                                    .findTargetClass(applicationContext.getBean(
                                                                        branchTransaction.getConfirmBeanName()))
                                                                    .getMethod(branchTransaction.getConfirm(),
                                                                        branchTransaction.getParameterTypes());
                                                                // 读取注解进行相应补偿行为
                                                                SagaTransaction sagaTransaction =
                                                                    confirmMethod.getAnnotation(SagaTransaction.class);
                                                                boolean retry =
                                                                    Boolean.parseBoolean(globalTX.get("retry"));
                                                                if (retry) {
                                                                    // 重试confirm
                                                                    confirmMethod.invoke(confirmBean,
                                                                        branchTransaction.getArgs());
                                                                    if (logger.isDebugEnabled()) {
                                                                        logger.debug("重试SAGA分支事务: {},成功",
                                                                            branchTransaction.getBranchId());
                                                                    }
                                                                } else {
                                                                    // 反向补偿
                                                                    Object cancelBean = applicationContext
                                                                        .getBean(sagaTransaction.clazz());
                                                                    Method cancelMethod =
                                                                        SpringProxyUtils.findTargetClass(cancelBean)
                                                                            .getMethod(sagaTransaction.cancel(),
                                                                                branchTransaction.getParameterTypes());
                                                                    cancelMethod.invoke(cancelBean,
                                                                        branchTransaction.getArgs());
                                                                    if (logger.isDebugEnabled()) {
                                                                        logger.debug("补偿SAGA分支事务: {},成功",
                                                                            branchTransaction.getBranchId());
                                                                    }
                                                                }
                                                                // 如无异常删除此分支事务即可
                                                                jedis.hdel(key, branchTransaction.getBranchId());
                                                            } catch (Exception e) {
                                                                logger.error("恢复saga事务出现异常,请反馈至github issue: {}",
                                                                    branchTransaction.getBranchId());
                                                            }
                                                        }
                                                    }
                                                }
                                            } finally {
                                                if (result) {
                                                    String currentOwner = jedis.get(taskKey);
                                                    if (StringUtils.equalsIgnoreCase(currentOwner, owner)) {
                                                        jedis.del(taskKey);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        try (Pipeline pipeline = jedis.pipelined()) {
                                            pipeline.del(key);
                                            pipeline.del(txKey);
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }, 10, 1, TimeUnit.SECONDS);

    }

    @Pointcut("@annotation(icu.funkye.easy.tx.config.annotation.SagaTransaction)")
    public void annotationPoinCut() {}

    @Around("annotationPoinCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature joinPointObject = (MethodSignature)joinPoint.getSignature();
        Method method = joinPointObject.getMethod();
        Object o;
        String xid = RootContext.getXID();
        SagaTransaction sagaTransaction = method.getAnnotation(SagaTransaction.class);
        // 业务开始之前先记录SAGA分支事务
        SagaBranchTransaction branchTransaction = new SagaBranchTransaction();
        branchTransaction.setClientId(easyTxProperties.getClientId());
        branchTransaction.setXid(xid);
        branchTransaction.setBranchId(UUID.randomUUID().toString());
        branchTransaction.setConfirm(
            StringUtils.isNoneBlank(sagaTransaction.confirm()) ? sagaTransaction.confirm() : method.getName());
        branchTransaction.setRetryInterval(sagaTransaction.retryInterval());
        branchTransaction.setConfirmBeanName(method.getDeclaringClass());
        branchTransaction.setArgs(joinPoint.getArgs());
        branchTransaction.setParameterTypes(method.getParameterTypes());
        branchTransaction.setModifyTime(new Date());
        RootContext.bindMode(EasyTxMode.SAGA);
        String key = PREFIX_SAGA_TX + xid;
        try (Jedis jedis = jedisEasyTxPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
            pipeline.hset(key, branchTransaction.getBranchId(), JSONObject.toJSONString(branchTransaction));
            pipeline.expire(key, 24 * 60 * 60);
        }
        boolean success = Boolean.FALSE;
        try {
            o = joinPoint.proceed();
            success = Boolean.TRUE;
            return o;
        } catch (Throwable e) {
            String txKey = PREFIX_TX + xid;
            try (Jedis jedis = jedisEasyTxPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
                pipeline.hset(txKey, "status", String.valueOf(Boolean.FALSE));
                pipeline.expire(txKey, 24 * 60 * 60);
            }
            // 如果不需要重试,那么进行反向补偿
            if (!Boolean.parseBoolean(RootContext.getRetry())) {
                Object object = applicationContext.getBean(sagaTransaction.clazz());
                if (object != null) {
                    Method cancelMethod =
                        object.getClass().getMethod(sagaTransaction.cancel(), joinPointObject.getParameterTypes());
                    try {
                        o = cancelMethod.invoke(object, joinPoint.getArgs());
                        success = Boolean.TRUE;
                        return o;
                    } catch (Exception exception) {
                        throw new RuntimeException("将在异步中重试此分支", exception);
                    }
                }
            }
            throw new RuntimeException("将在异步中重试此分支", e);
        } finally {
            // 记录SAGA分支事务
            branchTransaction.setModifyTime(new Date());
            branchTransaction.setStatus(success);
            try (Jedis jedis = jedisEasyTxPool.getResource()) {
                jedis.hset(key, branchTransaction.getBranchId(), JSONObject.toJSONString(branchTransaction));
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
