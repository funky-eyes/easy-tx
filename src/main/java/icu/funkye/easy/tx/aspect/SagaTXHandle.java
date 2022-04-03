package icu.funkye.easy.tx.aspect;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Resource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import icu.funkye.easy.tx.config.EasyTxMode;
import icu.funkye.easy.tx.util.SpringProxyUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.config.annotation.SagaTransaction;
import icu.funkye.easy.tx.entity.SagaBranchTransaction;
import icu.funkye.easy.tx.properties.EasyTxProperties;

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

    private static final String PIX = "easy_saga_tx_";

    private static final String PIX_TX = "easy_tx_";

    private static final String PIX_TASK = "saga_tx_task";

    @Resource
    RedisTemplate<String, Object> redisEasyTxTemplate;

    ApplicationContext applicationContext;

    @Resource
    EasyTxProperties easyTxProperties;

    ThreadFactoryBuilder threadFactoryBuilder =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("saga-pool-%d");

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder.build());

    public SagaTXHandle() {
        executor.scheduleAtFixedRate(() -> {
            Set<String> txs = redisEasyTxTemplate.keys(PIX + "*");
            txs.parallelStream().forEach(key -> {
                String owner = UUID.randomUUID().toString();
                Map<Object, Object> branchs = redisEasyTxTemplate.opsForHash().entries(key);
                Map<String, Boolean> retryMap = new ConcurrentHashMap<>();
                branchs.values().parallelStream().forEach(v -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("开始补偿SAGA事务: {}", key);
                    }
                    SagaBranchTransaction branchTransaction =
                        JSONObject.parseObject(String.valueOf(v), SagaBranchTransaction.class);
                    boolean retry = retryMap.computeIfAbsent(branchTransaction.getXid(), k -> {
                        String str =
                            (String)redisEasyTxTemplate.opsForHash().get(PIX_TX + branchTransaction.getXid(), "status");
                        // 默认不重试
                        return StringUtils.isNotBlank(str) && Boolean.parseBoolean(str);
                    });
                    // 只补偿自身
                    if (branchTransaction.getClientId().equalsIgnoreCase(easyTxProperties.getClientId())) {
                        String taskKey = PIX_TASK + branchTransaction.getBranchId();
                        Boolean result =
                            redisEasyTxTemplate.opsForValue().setIfPresent(taskKey, owner, 60, TimeUnit.SECONDS);
                        try {
                            // 抢到锁,得到此事务的补偿权利
                            if (result) {
                                // 分支状态为false进行补偿
                                if (!branchTransaction.isStatus()) {
                                    // 判断分支事务是否已经超时
                                    if (System.currentTimeMillis() - branchTransaction.getModifyTime()
                                        .getTime() > branchTransaction.getRetryInterval()) {
                                        try {
                                            // 获取confirm的bean
                                            Object confirmBean =
                                                applicationContext.getBean(branchTransaction.getConfirmBeanName());
                                            Method confirmMethod = SpringProxyUtils
                                                .findTargetClass(
                                                    applicationContext.getBean(branchTransaction.getConfirmBeanName()))
                                                .getMethod(branchTransaction.getConfirm(),
                                                    branchTransaction.getParameterTypes());
                                            // 读取注解进行相应补偿行为
                                            SagaTransaction sagaTransaction =
                                                confirmMethod.getAnnotation(SagaTransaction.class);
                                            if (retry) {
                                                // 重试confirm
                                                confirmMethod.invoke(confirmBean, branchTransaction.getArgs());
                                                if (logger.isDebugEnabled()) {
                                                    logger.debug("重试SAGA分支事务: {},成功", branchTransaction.getBranchId());
                                                }
                                            } else {
                                                // 反向补偿
                                                Object cancelBean =
                                                    applicationContext.getBean(sagaTransaction.getClass());
                                                Method cancelMethod = SpringProxyUtils.findTargetClass(cancelBean)
                                                    .getMethod(sagaTransaction.cancel(),
                                                        branchTransaction.getParameterTypes());
                                                cancelMethod.invoke(cancelBean, branchTransaction.getArgs());
                                                if (logger.isDebugEnabled()) {
                                                    logger.debug("补偿SAGA分支事务: {},成功", branchTransaction.getBranchId());
                                                }
                                            }
                                            // 如无异常删除此分支事务即可
                                            redisEasyTxTemplate.opsForHash().delete(key,
                                                branchTransaction.getBranchId());
                                        } catch (Exception e) {
                                            logger.error("恢复saga事务出现异常,请反馈至github issue: {}",
                                                branchTransaction.getBranchId());
                                        }
                                    }
                                }
                            }
                        } finally {
                            if (result) {
                                String currentOwner = (String)redisEasyTxTemplate.opsForValue().get(taskKey);
                                // 当前持有者为自身才有删除权利
                                if (StringUtils.equalsIgnoreCase(owner, currentOwner)) {
                                    redisEasyTxTemplate.delete(taskKey);
                                }
                            }
                        }
                    }
                });
            });
        }, 60, 1, TimeUnit.SECONDS);
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
        branchTransaction.setConfirm(sagaTransaction.confirm());
        branchTransaction.setRetryInterval(sagaTransaction.retryInterval());
        branchTransaction.setConfirmBeanName(SpringProxyUtils.findTargetClass(method.getClass()).getName());
        branchTransaction.setArgs(joinPoint.getArgs());
        branchTransaction.setParameterTypes(method.getParameterTypes());
        branchTransaction.setModifyTime(new Date());
        RootContext.bindMode(EasyTxMode.SAGA);
        String key = PIX + xid;
        String txKey = PIX_TX + xid;
        redisEasyTxTemplate.multi();
        redisEasyTxTemplate.opsForHash().put(key, branchTransaction.getBranchId(),
            JSONObject.toJSONString(branchTransaction));
        redisEasyTxTemplate.opsForHash().put(txKey, "retry", RootContext.getRetry());
        redisEasyTxTemplate.expire(key, 24, TimeUnit.HOURS);
        redisEasyTxTemplate.expire(txKey, 24, TimeUnit.HOURS);
        redisEasyTxTemplate.exec();
        boolean success = Boolean.FALSE;
        try {
            o = joinPoint.proceed();
            success = Boolean.TRUE;
            return o;
        } catch (Throwable e) {
            // 如果不需要重试,那么进行反向补偿
            if (!Boolean.parseBoolean(RootContext.getRetry())) {
                Object object = applicationContext.getBean(sagaTransaction.clazz());
                if (object != null) {
                    Method cancelMethod =
                        object.getClass().getMethod(sagaTransaction.cancel(), joinPointObject.getParameterTypes());
                    o = cancelMethod.invoke(object, joinPoint.getArgs());
                    success = Boolean.TRUE;
                    return o;
                }
            }
            throw new RuntimeException("将在异步中重试此分支", e);
        } finally {
            // 业务成功后记录SAGA分支事务为提交
            branchTransaction.setModifyTime(new Date());
            branchTransaction.setStatus(success);
            redisEasyTxTemplate.opsForHash().put("saga_tx_" + xid, branchTransaction.getBranchId(),
                JSONObject.toJSONString(branchTransaction));
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
