package icu.funkye.easy.tx;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import javax.annotation.Resource;
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
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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

    @ConditionalOnClass(name = "org.springframework.boot.autoconfigure.data.redis.RedisProperties")
    @Bean("jedisEasyTxPool")
    public JedisPool jedisEasyTxPool(RedisProperties redisProperties) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMinIdle(10);
        jedisPoolConfig.setMaxIdle(30);
        jedisPoolConfig.setMaxTotal(30);
        JedisPool jedisPool = new JedisPool(jedisPoolConfig, redisProperties.getHost(), redisProperties.getPort(),
            60000, redisProperties.getPassword(), 10);
        return jedisPool;
    }

}
