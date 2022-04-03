package icu.funkye.easy.tx;

import java.time.Duration;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.properties.RocketMqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(EasyTxAutoConfigure.class);

    @Autowired
    private RedisProperties redisProperties;

    @Bean(name = "jedisEasyTxConnectionFactory")
    public JedisConnectionFactory jedisEasyTxConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration
            .setHostName(null == redisProperties.getHost() || redisProperties.getHost().length() <= 0 ? "127.0.0.1"
                : redisProperties.getHost());
        redisStandaloneConfiguration.setPort(redisProperties.getPort() <= 0 ? 6379 : redisProperties.getPort());
        redisStandaloneConfiguration
            .setDatabase(redisProperties.getDatabase() <= 0 ? 0 : redisProperties.getDatabase());
        if (redisProperties.getPassword() != null && redisProperties.getPassword().length() > 0) {
            redisStandaloneConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }
        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfiguration =
            JedisClientConfiguration.builder();
        jedisClientConfiguration.connectTimeout(redisProperties.getTimeout());// connection
        // timeout
        JedisConnectionFactory factory =
            new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration.build());
        return factory;
    }

    @Bean("redisEasyTxTemplate")
    public RedisTemplate<String, Object> redisEasyTxTemplate(JedisConnectionFactory jedisEasyTxConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisEasyTxConnectionFactory);
        redisTemplate.setEnableTransactionSupport(true);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashKeySerializer(redisTemplate.getKeySerializer());
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
