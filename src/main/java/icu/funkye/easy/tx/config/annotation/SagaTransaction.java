package icu.funkye.easy.tx.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SagaTransaction {

    /**
     * 提交方法
     */
    String confirm() default "";

    /**
     * 失败补偿的bean
     */
    Class<?> clazz();

    /**
     * 失败补偿的方法
     */
    String cancel() default "";

    /**
     * 10s 重试一次
     */
    int retryInterval() default 10000;

}
