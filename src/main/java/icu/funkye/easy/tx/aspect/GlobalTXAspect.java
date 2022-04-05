package icu.funkye.easy.tx.aspect;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import icu.funkye.easy.tx.config.EasyTxMode;
import icu.funkye.easy.tx.config.SagaContext;
import icu.funkye.easy.tx.config.annotation.GlobalTransaction;
import icu.funkye.easy.tx.entity.GlobalTransactionDO;
import icu.funkye.easy.tx.entity.SagaBranchTransaction;
import icu.funkye.easy.tx.util.SpringProxyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.properties.RocketMqProperties;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.SetParams;

import static icu.funkye.easy.tx.constant.EasyTxConstant.PREFIX_EASY;
import static icu.funkye.easy.tx.constant.EasyTxConstant.PREFIX_SAGA_TX;
import static icu.funkye.easy.tx.constant.EasyTxConstant.PREFIX_TX;

/**
 * @author chenjianbin
 * @version 1.0.0
 */
@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true",
    matchIfMissing = true)
@Order(value = Ordered.HIGHEST_PRECEDENCE)
@Aspect
@Component
public class GlobalTXAspect {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired(required = false)
    private RocketMqProperties prop;

    @Autowired(required = false)
    private DefaultMQProducer easyTxProducer;

    @Autowired
    ApplicationContext applicationContext;

    @Resource
    JedisPool jedisEasyTxPool;

    ThreadFactoryBuilder threadFactoryBuilder =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("easy-tx-pool-%d");

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder.build());

    public GlobalTXAspect() {
        executor.scheduleAtFixedRate(() -> {
            boolean result;
            String key = PREFIX_EASY + "LOCK";
            String owner = UUID.randomUUID().toString();
            try (Jedis jedis = jedisEasyTxPool.getResource()) {
                result = StringUtils.equalsIgnoreCase(jedis.set(key, owner, SetParams.setParams().nx().ex(60)), "OK");
            }
            try {
                if (result) {
                    Set<String> txs;
                    try (Jedis jedis = jedisEasyTxPool.getResource()) {
                        txs = jedis.keys(PREFIX_TX + "*");
                    }
                    txs.parallelStream().forEach(tx -> {
                        try (Jedis jedis = jedisEasyTxPool.getResource()) {
                            GlobalTransactionDO globalTx =
                                JSONObject.parseObject(jedis.get(tx), GlobalTransactionDO.class);
                            if (!globalTx.getMode().equalsIgnoreCase(EasyTxMode.SAGA.mode())) {
                                long createTime = globalTx.getCreateTime().getTime();
                                int timeout = globalTx.getTimeout();
                                Boolean status = globalTx.getStatus();
                                if (status == null) {
                                    if (System.currentTimeMillis() - createTime > timeout) {
                                        globalTx.setStatus(Boolean.FALSE);
                                        jedis.set(tx, JSONObject.toJSONString(globalTx));
                                    }
                                } else {
                                    String globalKey = PREFIX_SAGA_TX + globalTx.getXid();
                                    if (status) {
                                        try (Pipeline pipeline = jedis.pipelined()) {
                                            pipeline.del(globalKey);
                                            pipeline.del(tx);
                                        }
                                        // 只有向前重试才处理,向后回滚由branch进行
                                    } else if (globalTx.getRetry()) {
                                        Map<String/*branchid*/, String/*saga branch*/> branchMap =
                                            jedis.hgetAll(globalKey);
                                        // 绑定需要补偿的xid
                                        RootContext.bind(globalTx.getXid());
                                        if (branchMap == null || branchMap.isEmpty()) {
                                            jedis.del(tx);
                                        } else {
                                            try {
                                                branchMap.forEach((k, v) -> {
                                                    SagaBranchTransaction sagaBranchTransaction =
                                                        JSONObject.parseObject(v, SagaBranchTransaction.class);
                                                    // 往thread local中加入所有branch,后续rm被调用将通过hashcode判断是新分支,还是已注册的分支
                                                    SagaContext.addBranch(sagaBranchTransaction.hashCode(),
                                                        sagaBranchTransaction);
                                                });
                                                Object bean = applicationContext.getBean(globalTx.getBean());
                                                Method retryMethod = SpringProxyUtils.findTargetClass(bean)
                                                    .getMethod(globalTx.getMethod(), globalTx.getParameterTypes());
                                                RootContext.bindRetryThread();
                                                retryMethod.invoke(bean, globalTx.getArgs());
                                                // 执行向前重试成功
                                                jedis.del(globalKey, tx);
                                            } catch (Exception e) {
                                                logger.error(e.getMessage(), e);
                                            } finally {
                                                RootContext.unbind();
                                                SagaContext.clear();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            } finally {
                try (Jedis jedis = jedisEasyTxPool.getResource()) {
                    String currentOwner = jedis.get(key);
                    if (StringUtils.equalsIgnoreCase(currentOwner, owner)) {
                        jedis.del(key);
                    }
                }
            }
        }, 10, 1, TimeUnit.SECONDS);
    }

    @Pointcut("@annotation(icu.funkye.easy.tx.config.annotation.GlobalTransaction)")
    public void annotationPointCut() {}

    @Around("annotationPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature joinPointObject = (MethodSignature)joinPoint.getSignature();
        Method method = joinPointObject.getMethod();
        Object o;
        String xid = RootContext.getXID();
        GlobalTransaction globalTransaction = method.getAnnotation(GlobalTransaction.class);
        if (StringUtils.isBlank(xid)) {
            xid = UUID.randomUUID().toString();
            RootContext.bind(xid);
            RootContext.bindRetry(String.valueOf(globalTransaction.retry()));
        } else {
            // 说明这个是参与者,直接运行即可
            return joinPoint.proceed();
        }
        JSONObject object = new JSONObject();
        object.put(RootContext.KEY_XID, RootContext.getXID());
        String txKey = PREFIX_TX + xid;
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setArgs(joinPoint.getArgs());
        globalTransactionDO.setXid(xid);
        globalTransactionDO.setBean(method.getDeclaringClass());
        globalTransactionDO.setMode(globalTransaction.mode().mode());
        globalTransactionDO.setMethod(method.getName());
        globalTransactionDO.setParameterTypes(method.getParameterTypes());
        globalTransactionDO.setRetry(globalTransaction.retry());
        globalTransactionDO.setTimeout(globalTransaction.timeout());
        try (Jedis jedis = jedisEasyTxPool.getResource()) {
            jedis.set(txKey, JSONObject.toJSONString(globalTransactionDO), SetParams.setParams().ex(24 * 60 * 60));
        }
        try {
            o = joinPoint.proceed();
            globalTransactionDO.setStatus(Boolean.TRUE);
            globalTransactionDO.setModifyTime(new Date());
            try (Jedis jedis = jedisEasyTxPool.getResource()) {
                jedis.set(txKey, JSONObject.toJSONString(globalTransactionDO), SetParams.setParams().ex(24 * 60 * 60));
            }
            object.put(RootContext.XID_STATUS, 1);
            return o;
        } catch (Throwable e) {
            object.put(RootContext.XID_STATUS, 0);
            globalTransactionDO.setModifyTime(new Date());
            globalTransactionDO.setStatus(Boolean.FALSE);
            try (Jedis jedis = jedisEasyTxPool.getResource()) {
                jedis.set(txKey, JSONObject.toJSONString(globalTransactionDO), SetParams.setParams().ex(24 * 60 * 60));
            }
            throw e;
        } finally {
            RootContext.unbind();
            if (easyTxProducer != null) {
                Message sendMsg = new Message(prop.getTopic(), object.toJSONString().getBytes());
                easyTxProducer.send(sendMsg);
            }

        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

}
