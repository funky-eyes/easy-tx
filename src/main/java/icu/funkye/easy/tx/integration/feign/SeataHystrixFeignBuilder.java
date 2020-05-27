package icu.funkye.easy.tx.integration.feign;

import org.springframework.beans.factory.BeanFactory;

import feign.Feign;
import feign.Retryer;
import feign.hystrix.HystrixFeign;

/**
 * @author xiaojing
 */
final class SeataHystrixFeignBuilder {

	private SeataHystrixFeignBuilder() {
	}

	static Feign.Builder builder(BeanFactory beanFactory) {
		return HystrixFeign.builder().retryer(Retryer.NEVER_RETRY)
				.client(new SeataFeignClient(beanFactory));
	}

}
