package com.encrypt.mysql.base.annotation;

import java.lang.annotation.*;

/**
 * 是否对实体类加解密
 */
@Documented
@Inherited
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptClass {
}
