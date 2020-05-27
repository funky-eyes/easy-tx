package icu.funkye.easy.tx.integration.feign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;

import feign.Client;

/**
 * @author xiaojing
 */
public class SeataFeignObjectWrapper {

	private static final Log LOG = LogFactory.getLog(SeataFeignObjectWrapper.class);

	private final BeanFactory beanFactory;

	private CachingSpringLoadBalancerFactory cachingSpringLoadBalancerFactory;

	private SpringClientFactory springClientFactory;

	SeataFeignObjectWrapper(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	Object wrap(Object bean) {
		if (bean instanceof Client && !(bean instanceof SeataFeignClient)) {
			return new SeataFeignClient(this.beanFactory, (Client) bean);
		}
		return bean;
	}

	CachingSpringLoadBalancerFactory factory() {
		if (this.cachingSpringLoadBalancerFactory == null) {
			this.cachingSpringLoadBalancerFactory = this.beanFactory
					.getBean(CachingSpringLoadBalancerFactory.class);
		}
		return this.cachingSpringLoadBalancerFactory;
	}

	SpringClientFactory clientFactory() {
		if (this.springClientFactory == null) {
			this.springClientFactory = this.beanFactory
					.getBean(SpringClientFactory.class);
		}
		return this.springClientFactory;
	}

}
