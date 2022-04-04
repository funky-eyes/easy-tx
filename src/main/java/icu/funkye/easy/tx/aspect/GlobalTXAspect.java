package icu.funkye.easy.tx.aspect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import icu.funkye.easy.tx.config.annotation.GlobalTransaction;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

    @Autowired(required = false)
    private RocketMqProperties prop;

    @Autowired(required = false)
    private DefaultMQProducer easyTxProducer;

    @Resource
    JedisPool jedisEasyTxPool;

    ThreadFactoryBuilder threadFactoryBuilder =
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("easy-tx-pool-%d");

    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, threadFactoryBuilder.build());

    public GlobalTXAspect() {
        executor.scheduleAtFixedRate(() -> {
            try (Jedis jedis = jedisEasyTxPool.getResource()) {
                String key = PREFIX_TX + "LOCK";
                String owner = UUID.randomUUID().toString();
                boolean result =
                    StringUtils.equalsIgnoreCase(jedis.set(key, owner, SetParams.setParams().nx().ex(60)), "OK");
                if (result) {
                    try {
                        Set<String> txs = jedis.keys(PREFIX_TX + "*");
                        for (String tx : txs) {
                            Response<String> createTimeResponse;
                            Response<String> tiemoutResponse;
                            try (Pipeline pipeline = jedis.pipelined()) {
                                createTimeResponse = pipeline.hget(tx, "createTime");
                                tiemoutResponse = pipeline.hget(tx, "timeout");
                            }
                            long createTime = Long.parseLong(createTimeResponse.get());
                            long timeout = Long.parseLong(tiemoutResponse.get());
                            if (System.currentTimeMillis() - createTime > timeout) {
                                jedis.hset(tx, "status", String.valueOf(Boolean.FALSE));
                            }
                        }
                    } finally {
                        String currentOwner = jedis.get(key);
                        if (StringUtils.equalsIgnoreCase(currentOwner, owner)) {
                            jedis.del(key);
                        }
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
        boolean sponsor = false;
        GlobalTransaction globalTransaction = method.getAnnotation(GlobalTransaction.class);
        if (StringUtils.isBlank(xid)) {
            xid = UUID.randomUUID().toString();
            sponsor = true;
            RootContext.bind(xid);
            RootContext.bindRetry(String.valueOf(globalTransaction.retry()));
        }
        JSONObject object = new JSONObject();
        object.put(RootContext.KEY_XID, RootContext.getXID());
        String txKey = PREFIX_TX + xid;
        try (Jedis jedis = jedisEasyTxPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
            pipeline.hset(txKey, "retry", String.valueOf(globalTransaction.retry()));
            pipeline.hset(txKey, "timeout", String.valueOf(globalTransaction.timeout()));
            pipeline.hset(txKey, "createTime", String.valueOf(System.currentTimeMillis()));
            pipeline.expire(txKey, 24 * 60 * 60);
        }
        try {
            o = joinPoint.proceed();
            try (Jedis jedis = jedisEasyTxPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
                pipeline.hset(txKey, "status", String.valueOf(Boolean.TRUE));
                pipeline.expire(txKey, 24 * 60 * 60);
            }
            object.put(RootContext.XID_STATUS, 1);
        } catch (Throwable e) {
            object.put(RootContext.XID_STATUS, 0);
            try (Jedis jedis = jedisEasyTxPool.getResource(); Pipeline pipeline = jedis.pipelined()) {
                pipeline.hset(txKey, "status", String.valueOf(Boolean.FALSE));
                pipeline.expire(txKey, 24 * 60 * 60);
            }
            throw e;
        } finally {
            if (sponsor) {
                if (easyTxProducer != null) {
                    Message sendMsg = new Message(prop.getTopic(), object.toJSONString().getBytes());
                    easyTxProducer.send(sendMsg);
                }
                RootContext.unbind();
            }
        }
        return o;
    }

}
