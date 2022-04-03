package icu.funkye.easy.tx;

import java.time.Duration;
import icu.funkye.easy.tx.listener.EasyMQConsumeMsgListenerProcessor;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.properties.RocketMqProperties;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author 陈健斌 funkye
 */
@ComponentScan(basePackages = {"icu.funkye.easy.tx.config", "icu.funkye.easy.tx.listener", "icu.funkye.easy.tx.aspect",
    "icu.funkye.easy.tx.proxy", "icu.funkye.easy.tx.integration", "icu.funkye.easy.tx.integration.dubbo",
    "icu.funkye.easy.tx.integration.http", "icu.funkye.easy.tx.integration.feign", "icu.funkye.easy.tx.properties"})
@EnableConfigurationProperties({RocketMqProperties.class, EasyTxProperties.class})
@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true",
    matchIfMissing = true)
@Configuration
public class EasyTxAutoConfigure {

    @Autowired
    private RocketMqProperties prop;

    private static final Logger LOGGER = LoggerFactory.getLogger(EasyTxAutoConfigure.class);

    @ConditionalOnExpression("#{easyTxProperties.getOnlyUseMode().contains('easy')}")
    @Bean
    public EasyMQConsumeMsgListenerProcessor easyMQConsumeMsgListenerProcessor() {
        return new EasyMQConsumeMsgListenerProcessor();
    }

    @ConditionalOnExpression("#{easyTxProperties.getOnlyUseMode().contains('easy')}")
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

    @ConditionalOnExpression("#{easyTxProperties.getOnlyUseMode().contains('easy')}")
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

    @ConditionalOnExpression("#{easyTxProperties.getOnlyUseMode().contains('saga')}")
    @Bean("redisEasyTxTemplate")
    public RedisTemplate<String, Object> redisEasyTxTemplate(JedisConnectionFactory jedisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(redisTemplate.getKeySerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
