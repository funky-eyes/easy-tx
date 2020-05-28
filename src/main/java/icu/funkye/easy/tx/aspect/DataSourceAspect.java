package icu.funkye.easy.tx.aspect;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.proxy.ConnectionFactory;
import icu.funkye.easy.tx.proxy.ConnectionProxy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * -动态拦截数据源
 *
 * @author chenjianbin
 * @version 1.0.0
 */

@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true",
    matchIfMissing = true)
@Aspect
@Component
public class DataSourceAspect {

    @Around("execution(* javax.sql.DataSource.getConnection(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        List<ConnectionProxy> list = ConnectionFactory.getConcurrentHashMap().get(RootContext.getXID());
        if (list == null) {
            list = new ArrayList<>();
        }
        ConnectionProxy connectionProxy = new ConnectionProxy((Connection)joinPoint.proceed());
        list.add(connectionProxy);
        ConnectionFactory.getConcurrentHashMap().put(RootContext.getXID(), list);
        return connectionProxy;
    }

}
