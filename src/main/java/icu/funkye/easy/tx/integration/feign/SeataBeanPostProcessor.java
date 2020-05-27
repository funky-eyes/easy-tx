package icu.funkye.easy.tx.integration.feign;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author xiaojing
 */
public class SeataBeanPostProcessor implements BeanPostProcessor {

    private final SeataFeignObjectWrapper seataFeignObjectWrapper;

    SeataBeanPostProcessor(SeataFeignObjectWrapper seataFeignObjectWrapper) {
        this.seataFeignObjectWrapper = seataFeignObjectWrapper;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return this.seataFeignObjectWrapper.wrap(bean);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
