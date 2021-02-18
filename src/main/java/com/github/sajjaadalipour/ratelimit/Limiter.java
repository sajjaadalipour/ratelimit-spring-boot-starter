package com.github.sajjaadalipour.ratelimit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Sajjad Alipour
 */
@Target(METHOD)
@Retention(RUNTIME)
public @interface Limiter {

    /**
     * The number of API calls, determines the limitation count for the presented duration.
     */
    int count();

    /**
     * Determines the duration of limiting.
     */
    String duration();

    /**
     * Spring Expression Language (SpEL) expression for computing the key dynamically.
     **/
    String key();

    /**
     * Determines the blocking duration.
     */
    String blockDuration() default "";
}
