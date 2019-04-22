package top.infra.web.servlet.handler;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {

    /**
     * @return application id of applied project
     */
    String application() default "";

    /**
     * @return key to distinguish rate limiter
     */
    String key();

    /**
     * @return rate limiter in queries per timeUnit
     */
    int limitation();

    /**
     * @return expire time
     */
    int expire();

    /**
     * @return expire time unit
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
