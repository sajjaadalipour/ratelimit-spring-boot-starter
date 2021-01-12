package com.github.sajjaadalipour.ratelimit;

import java.lang.annotation.*;


@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    String key() default "";

    long block() default 0;

    long duration() default 0;

    int count() default 0;

    String keyGenerator() default "";

}