package icu.funkye.easy.tx.aspect;

import java.sql.Connection;
import java.util.ArrayList;
import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.properties.EasyTxProperties;
import icu.funkye.easy.tx.proxy.ConnectionFactory;
import icu.funkye.easy.tx.proxy.ConnectionProxy;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * -动态拦截数据源
 *
 * @author chenjianbin
 */

@ConditionalOnProperty(prefix = EasyTxProperties.EASY_TX_PREFIX, name = {"enable"}, havingValue = "true",
    matchIfMissing = true)
@Aspect
@Component
public class DataSourceAspect {

    @Autowired
    ObjectFactory<ConnectionProxy> bean;

    @Around("execution(* javax.sql.DataSource.getConnection(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Connection conn = (Connection)joinPoint.proceed();
        if (StringUtils.isNotBlank(RootContext.getXID()) && !RootContext.isSaga()) {
            ConnectionProxy connectionProxy = bean.getObject();
            connectionProxy.setConnection(conn);
            ConnectionFactory.getConcurrentHashMap().computeIfAbsent(RootContext.getXID(), k -> new ArrayList<>())
                .add(connectionProxy);
            return connectionProxy;
        }
        return conn;
    }

}
