package icu.funkye.easy.tx.integration.feign;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import feign.Client;
import feign.Feign;

/**
 * @author xiaojing
 */
@ConditionalOnBean(name = {"easyTxProducer"})
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Client.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class SeataFeignClientAutoConfiguration {

	@Bean
	@Scope("prototype")
	@ConditionalOnClass(name = "com.netflix.hystrix.HystrixCommand")
	@ConditionalOnProperty(name = "feign.hystrix.enabled", havingValue = "true")
	Feign.Builder feignHystrixBuilder(BeanFactory beanFactory) {
		return SeataHystrixFeignBuilder.builder(beanFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	@Scope("prototype")
	Feign.Builder feignBuilder(BeanFactory beanFactory) {
		return SeataFeignBuilder.builder(beanFactory);
	}

	@Configuration(proxyBeanMethods = false)
	protected static class FeignBeanPostProcessorConfiguration {

		@Bean
		SeataBeanPostProcessor seataBeanPostProcessor(
				SeataFeignObjectWrapper seataFeignObjectWrapper) {
			return new SeataBeanPostProcessor(seataFeignObjectWrapper);
		}

		@Bean
		SeataContextBeanPostProcessor seataContextBeanPostProcessor(
				BeanFactory beanFactory) {
			return new SeataContextBeanPostProcessor(beanFactory);
		}

		@Bean SeataFeignObjectWrapper seataFeignObjectWrapper(BeanFactory beanFactory) {
			return new SeataFeignObjectWrapper(beanFactory);
		}

	}

}
