package icu.funkye.easy.tx.integration.feign;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.openfeign.FeignContext;

import feign.Client;

/**
 * @author xiaojing
 */
public class SeataFeignContext extends FeignContext {

    private final SeataFeignObjectWrapper seataFeignObjectWrapper;

    private final FeignContext delegate;

    SeataFeignContext(SeataFeignObjectWrapper seataFeignObjectWrapper, FeignContext delegate) {
        this.seataFeignObjectWrapper = seataFeignObjectWrapper;
        this.delegate = delegate;
    }

    @Override
    public <T> T getInstance(String name, Class<T> type) {
        T object = this.delegate.getInstance(name, type);
        if (object instanceof Client) {
            return object;
        }
        return (T)this.seataFeignObjectWrapper.wrap(object);
    }

    @Override
    public <T> Map<String, T> getInstances(String name, Class<T> type) {
        Map<String, T> instances = this.delegate.getInstances(name, type);
        if (instances == null) {
            return null;
        }
        Map<String, T> convertedInstances = new HashMap<>();
        for (Map.Entry<String, T> entry : instances.entrySet()) {
            if (entry.getValue() instanceof Client) {
                convertedInstances.put(entry.getKey(), entry.getValue());
            } else {
                convertedInstances.put(entry.getKey(), (T)this.seataFeignObjectWrapper.wrap(entry.getValue()));
            }
        }
        return convertedInstances;
    }

}
