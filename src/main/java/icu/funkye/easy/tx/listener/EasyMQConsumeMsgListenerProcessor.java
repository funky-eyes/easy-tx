package icu.funkye.easy.tx.listener;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.proxy.ConnectionFactory;
import icu.funkye.easy.tx.proxy.ConnectionProxy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author 陈健斌 funkye
 */
@Component
public class EasyMQConsumeMsgListenerProcessor implements MessageListenerConcurrently {
    public static final Logger LOGGER = LoggerFactory.getLogger(EasyMQConsumeMsgListenerProcessor.class);

    /**
     * @param msgList
     * @param consumeConcurrentlyContext
     * @return
     */
    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgList,
        ConsumeConcurrentlyContext consumeConcurrentlyContext) {
        if (CollectionUtils.isEmpty(msgList)) {
            LOGGER.info("MQ接收消息为空，直接返回成功");
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
        MessageExt messageExt = msgList.get(0);
        try {
            String topic = messageExt.getTopic();
            String tags = messageExt.getTags();
            String body = new String(messageExt.getBody(), "utf-8");
            JSONObject object = JSONObject.parseObject(body);
            String xid = object.get(RootContext.KEY_XID).toString();
            LOGGER.info("MQ消息topic={}, tags={}, 消息内容={}", topic, tags, body);
            List<ConnectionProxy> list = ConnectionFactory.getConcurrentHashMap().get(xid);
            if (list != null) {
                try {
                    Integer status = Integer.valueOf(object.get(RootContext.XID_STATUS).toString());
                    list.forEach(i -> {
                        i.notify(status);
                    });
                } finally {
                    ConnectionFactory.getConcurrentHashMap().remove(xid);
                }
            }
        } catch (Exception e) {
            LOGGER.error("获取MQ消息内容异常{}", e);
        }
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

}
