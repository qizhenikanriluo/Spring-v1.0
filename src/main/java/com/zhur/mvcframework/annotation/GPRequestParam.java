package com.zhur.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @program: spring-1.0
 * @description: 自定义注入类
 * @author: zhur
 * @date: 2019-11-29 20:01
 **/
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GPRequestParam {
    String value() default "";

}
