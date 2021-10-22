package com.springselfcoding.mvcframework;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SelfRequestParam {

    String value() default "";

}