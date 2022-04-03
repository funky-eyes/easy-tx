package icu.funkye.easy.tx.aspect;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.properties.RocketMqProperties;


import static icu.funkye.easy.tx.aspect.SagaTXHandle.PIX_TX;

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
    RedisTemplate<String, Object> redisEasyTxTemplate;

    @Pointcut("@annotation(icu.funkye.easy.tx.config.annotation.GlobalTransaction)")
    public void annotationPoinCut() {}

    @Around("annotationPoinCut()")
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
        String txKey = PIX_TX + xid;
        redisEasyTxTemplate.multi();
        redisEasyTxTemplate.opsForHash().put(txKey, "retry", globalTransaction.retry());
        redisEasyTxTemplate.opsForHash().put(txKey, "timeout", globalTransaction.timeout());
        redisEasyTxTemplate.exec();
        try {
            o = joinPoint.proceed();
            redisEasyTxTemplate.multi();
            redisEasyTxTemplate.opsForHash().put(txKey, "status", Boolean.TRUE);
            redisEasyTxTemplate.expire(txKey, 24, TimeUnit.HOURS);
            redisEasyTxTemplate.exec();
            object.put(RootContext.XID_STATUS, 1);
        } catch (Throwable e) {
            object.put(RootContext.XID_STATUS, 0);
            redisEasyTxTemplate.multi();
            redisEasyTxTemplate.opsForHash().put(txKey, "status", Boolean.FALSE);
            redisEasyTxTemplate.expire(txKey, 24, TimeUnit.HOURS);
            redisEasyTxTemplate.exec();
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
