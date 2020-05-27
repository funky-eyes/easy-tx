package icu.funkye.easy.tx.integration.feign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.util.StringUtils;

import feign.Client;
import feign.Request;
import feign.Response;
import icu.funkye.easy.tx.config.RootContext;

/**
 * @author xiaojing
 */
public class SeataFeignClient implements Client {

    private final Client delegate;

    private final BeanFactory beanFactory;

    private static final int MAP_SIZE = 16;

    SeataFeignClient(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        this.delegate = new Default(null, null);
    }

    SeataFeignClient(BeanFactory beanFactory, Client delegate) {
        this.delegate = delegate;
        this.beanFactory = beanFactory;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {

        Request modifiedRequest = getModifyRequest(request);
        return this.delegate.execute(modifiedRequest, options);
    }

    private Request getModifyRequest(Request request) {

        String xid = RootContext.getXID();

        if (StringUtils.isEmpty(xid)) {
            return request;
        }

        Map<String, Collection<String>> headers = new HashMap<>(MAP_SIZE);
        headers.putAll(request.headers());

        List<String> seataXid = new ArrayList<>();
        seataXid.add(xid);
        headers.put(RootContext.KEY_XID, seataXid);

        return Request.create(request.method(), request.url(), headers, request.body(), request.charset());
    }

}
