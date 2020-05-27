package icu.funkye.easy.tx.integration.feign;

import org.springframework.beans.factory.BeanFactory;

import feign.Feign;

/**
 * @author xiaojing
 */
final class SeataFeignBuilder {

    private SeataFeignBuilder() {}

    static Feign.Builder builder(BeanFactory beanFactory) {
        return Feign.builder().client(new SeataFeignClient(beanFactory));
    }

}
