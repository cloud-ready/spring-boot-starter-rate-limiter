package top.infra.cloudready.boot;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import lombok.Setter;

import org.redisson.api.BatchOptions;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import top.infra.core.ExpectedExceptionWithCode;
import top.infra.localization.BuildInErrorCodes;
import top.infra.web.servlet.handler.OrderedHandlerInterceptorAdapter;

/**
 * Created by zhuowan on 2018/5/15 13:55.
 * Description:
 */
@Component
@ConditionalOnProperty(prefix = "interceptor.rate-limiter", name = "enabled", havingValue = "true")
public class RateLimiterInterceptor extends OrderedHandlerInterceptorAdapter {

  @Autowired
  @Setter
  private RedissonClient redissonClient;

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object o) throws Exception {
    HandlerMethod handlerMethod = (HandlerMethod) o;
    RateLimiter rateLimiter = handlerMethod.getMethod().getAnnotation(RateLimiter.class);
    if (this.isReachedLimitation(rateLimiter)) {
      throw new ExpectedExceptionWithCode(BuildInErrorCodes.RATE_LIMITATION_REACHED);
    }

    return true;
  }

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
      final RBatch batch = this.redissonClient.createBatch(batchOptions());
      String cacheKey = isNotBlank(applicationId) ? "QPS:" + applicationId : "QPS";
      batch.getMapCache(cacheKey).fastPutIfAbsentAsync(key, 0, expire, expTimeUnit, expire, expTimeUnit);
      batch.getMapCache(cacheKey).addAndGetAsync(key, 1);
      final BatchResult batchResult = batch.execute();

      return (Integer) batchResult.getResponses().get(1) > limitation;
    }

    return false;
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
}
