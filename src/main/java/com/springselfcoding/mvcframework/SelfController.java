package com.springselfcoding.mvcframework;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SelfController {

    String value() default "";
}