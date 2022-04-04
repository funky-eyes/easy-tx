package icu.funkye.easy.tx.config.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;
import icu.funkye.easy.tx.config.EasyTxMode;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GlobalTransaction {

    String name() default "";

    EasyTxMode mode() default EasyTxMode.EASY ;

    int timeout() default 5000;

    /**
     * 是否重试
     */
    boolean retry() default false;

}
