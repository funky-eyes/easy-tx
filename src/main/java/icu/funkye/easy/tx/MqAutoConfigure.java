package icu.funkye.easy.tx;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import icu.funkye.easy.tx.listener.EasyMQConsumeMsgListenerProcessor;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.properties.RocketMqProperties;

/**
 * @author 陈健斌 funkye
 */
@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true",
    matchIfMissing = true)
@ConditionalOnClass(
        name = {"org.apache.rocketmq.client.producer.DefaultMQProducer"}
)
@ConditionalOnExpression("#{environment.getProperty('easy.tx.onlyUseMode').contains('easy')}")
@Configuration
public class MqAutoConfigure {

    @Autowired
    private RocketMqProperties prop;

    private static final Logger LOGGER = LoggerFactory.getLogger(MqAutoConfigure.class);

     @Bean
    public EasyMQConsumeMsgListenerProcessor easyMQConsumeMsgListenerProcessor() {
        return new EasyMQConsumeMsgListenerProcessor();
    }

    @Bean
    public DefaultMQProducer easyTxProducer() throws MQClientException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("easyTxProducer is creating");
        }
        DefaultMQProducer producer = new DefaultMQProducer(prop.getGroup());
        producer.setNamesrvAddr(prop.getNameServer());
        producer.setVipChannelEnabled(false);
        producer.start();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("rocketmq producer server open the success");
        }
        return producer;
    }

    @Bean
    public DefaultMQPushConsumer easyTxConsumer(EasyMQConsumeMsgListenerProcessor easyMQConsumeMsgListenerProcessor)
        throws MQClientException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("easyTxConsumer is creating");
        }
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(prop.getGroup());
        consumer.setNamesrvAddr(prop.getNameServer());
        // 设置监听
        consumer.registerMessageListener(easyMQConsumeMsgListenerProcessor);
        /**
         * 设置consumer第一次启动是从队列头部开始还是队列尾部开始 如果不是第一次启动，那么按照上次消费的位置继续消费
         */
        consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);
        /**
         * 设置消费模型，集群还是广播，默认为集群
         */
        consumer.setMessageModel(MessageModel.BROADCASTING);
        try {
            consumer.setVipChannelEnabled(false);
            consumer.subscribe(prop.getTopic(), "*");
            consumer.start();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("consumer creating a successful groupName={}, topics={}, namesrvAddr={}", prop.getGroup(),
                    prop.getTopic(), prop.getNameServer());
            }
        } catch (MQClientException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("consumer create a failure! error: {}", e.getMessage());
            }
        }
        return consumer;
    }

}
