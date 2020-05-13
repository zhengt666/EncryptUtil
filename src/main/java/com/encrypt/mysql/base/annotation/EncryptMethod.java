package com.encrypt.mysql.base.annotation;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.annotation.*;

/**
 * @Description: 是否对方法解密
 * @Author: zhengt
 * @CreateDate: 2020/4/20 15:35
 * @UpdateUser: zhengt
 * @UpdateDate: 2020/4/20 15:35
 * @UpdateRemark: 修改内容
 * @Version: 1.0
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Order(Ordered.HIGHEST_PRECEDENCE)
public @interface EncryptMethod {

    String[] decrypts() default {};
}
