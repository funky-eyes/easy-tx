package icu.funkye.easy.tx.aspect;

import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.properties.RocketMqProperties;

/**
 * @author chenjianbin
 * @version 1.0.0
 */
@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true", matchIfMissing = true)
@Order(value = 100)
@Aspect
@Component
public class GlobalTXAspect {

    @Autowired
    private RocketMqProperties prop;

    @Resource
    private DefaultMQProducer easyTxProducer;

    @Pointcut("@annotation(icu.funkye.easy.tx.config.annotation.GlobalTransaction)")
    public void annotationPoinCut() {}

    @Around("annotationPoinCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        String xid = RootContext.getXID();
        boolean sponsor = false;
        if (StringUtils.isBlank(xid)) {
            xid = UUID.randomUUID().toString();
            sponsor = true;
            RootContext.bind(xid);
        }
        Object o = null;
        JSONObject object = new JSONObject();
        object.put(RootContext.KEY_XID, RootContext.getXID());
        try {
            o = joinPoint.proceed();
            object.put(RootContext.XID_STATUS, 1);
        } catch (Throwable e) {
            object.put(RootContext.XID_STATUS, 0);
            throw e;
        } finally {
            if (sponsor) {
                Message sendMsg = new Message(prop.getTopic(), object.toJSONString().getBytes());
                easyTxProducer.send(sendMsg);
            }
        }
        return o;
    }
}
