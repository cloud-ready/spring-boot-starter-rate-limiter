package top.infra.web.servlet.handler;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.Setter;

import org.redisson.api.BatchOptions;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RMapCacheAsync;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Created by zhuowan on 2018/5/15 13:55.
 * Description:
 */
public class RateLimiterInterceptor extends OrderedHandlerInterceptorAdapter {

    /**
     * 1. Switches batch to atomic mode. Redis atomically executes all commands of this batch as a single command.
     * 2. Inform Redis not to send reply back to client. This allows to save network traffic for commands with batch with big .skipResult()
     * 3. Synchronize write operations execution across defined amount of Redis slave nodes
     * 4. Response timeout
     * 5. Retry interval for each attempt to send Redis commands batch
     *
     * @return BatchOptions
     */
    private static BatchOptions batchOptions() {
        return BatchOptions.defaults() //
            .atomic() //
            .responseTimeout(2, TimeUnit.SECONDS) //
            .retryInterval(2, TimeUnit.SECONDS);
    }

    static String cacheName(final String applicationId) {
        return isNotBlank(applicationId) ? "QPS:" + applicationId : "QPS";
    }


    @Autowired
    @Setter
    private RedissonClient redissonClient;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean preHandle(final HttpServletRequest req, final HttpServletResponse resp, final Object o) throws Exception {
        final RateLimiter rateLimiter;

        if (HandlerMethod.class.isAssignableFrom(o.getClass())) {
            // spring-boot 1.x
            final HandlerMethod handlerMethod = (HandlerMethod) o;
            rateLimiter = handlerMethod.getMethod().getAnnotation(RateLimiter.class);
        } else if (ResourceHttpRequestHandler.class.isAssignableFrom(o.getClass())) {
            // static resources or spring-boot 2.x
            // final ResourceHttpRequestHandler requestHandler = (ResourceHttpRequestHandler) o;
            return true;
        } else {
            throw new RuntimeException("unknown handler");
        }

        if (this.isReachedLimitation(rateLimiter)) {
            // TODO better error object
            throw new ResponseStatusException(FORBIDDEN, "Rate limit exceeded");
        } else {
            return true;
        }
    }

    /**
     * Is rate limitation reached.
     *
     * @param applicationId applicationId
     * @param key           key
     * @param limitation    limitation
     * @param expire        expire
     * @param expTimeUnit   expTimeUnit
     * @return if true, request will be forbidden because of reaching the maximum value of qps limitation
     * if false, can continue dispatching request. return false if any illegal parameters.
     */
    public boolean isReachedLimitation(
        final String applicationId, //
        final String key, //
        final int limitation, //
        final int expire, //
        final TimeUnit expTimeUnit //
    ) {
        if (isNotBlank(key) && expire > 0 && expTimeUnit != null) {
            final String cacheName = RateLimiterInterceptor.cacheName(applicationId);

            final RBatch batch = this.batch();
            final RMapCacheAsync<String, Integer> cache = batch.getMapCache(cacheName);
            cache.fastPutIfAbsentAsync(key, 0, expire, expTimeUnit, expire, expTimeUnit);
            cache.addAndGetAsync(key, 1);
            final BatchResult batchResult = batch.execute();

            return (Integer) batchResult.getResponses().get(1) > limitation;
        } else {
            return false;
        }
    }

    /**
     * default TimeUnit is SECONDS.
     *
     * @param applicationId applicationId
     * @param key           key
     * @param limitation    limitation
     * @param expire        expire (default TimeUnit is SECONDS)
     * @return if true, request will be forbidden because of reaching the maximum value of qps limitation
     * if false, can continue dispatching request. return false if any illegal parameters.
     */
    public boolean isReachedLimitationInSeconds(
        final String applicationId, //
        final String key, //
        final int limitation, //
        final int expire //
    ) {
        return isReachedLimitation(applicationId, key, limitation, expire, TimeUnit.SECONDS);
    }

    public boolean isReachedLimitation(final RateLimiter rateLimiter) {
        return rateLimiter != null &&
            isReachedLimitation( //
                rateLimiter.application(), //
                rateLimiter.key(), //
                rateLimiter.limitation(), //
                rateLimiter.expire(), //
                rateLimiter.timeUnit());
    }

    RBatch batch() {
        return this.redissonClient.createBatch(batchOptions());
    }
}
