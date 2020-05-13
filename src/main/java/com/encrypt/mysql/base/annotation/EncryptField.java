package com.encrypt.mysql.base.annotation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * 是否对字段加解密（包括map）
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Inherited
public @interface EncryptField {

    /**
     * 对哪些map值加密
     */
    String[] encrypts() default {};

    String value() default "";

}
