package icu.funkye.easy.tx.aspect;

import java.sql.Connection;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import icu.funkye.easy.tx.config.RootContext;
import icu.funkye.easy.tx.proxy.ConnectionFactory;
import icu.funkye.easy.tx.proxy.ConnectionProxy;

/**
 * -动态拦截数据源
 *
 * @author chenjianbin
 * @version 1.0.0
 */

@ConditionalOnBean(name = {"easyTxConsumer"})
@Aspect
@Component
public class DataSourceAspect {
    @Autowired
    ConnectionProxy connectionProxy;

    @Around("execution(* javax.sql.DataSource.getConnection(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        connectionProxy.setConnection((Connection)joinPoint.proceed());
        ConnectionFactory.getConcurrentHashMap().put(RootContext.getXID(), connectionProxy);
        return connectionProxy;
    }

}
