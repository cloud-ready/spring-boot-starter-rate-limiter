package top.infra.web.servlet.handler;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import top.infra.cloudready.boot.RedissonSentinelAutoConfiguration;
import top.infra.cloudready.boot.RedissonStandaloneAutoConfiguration;


@AutoConfigureAfter({RedissonSentinelAutoConfiguration.class, RedissonStandaloneAutoConfiguration.class})
@ConditionalOnProperty(prefix = "interceptor.rate-limiter", name = "enabled", havingValue = "true")
@Configuration
@EnableConfigurationProperties({RateLimiterProperties.class})
class RateLimiterAutoConfiguration extends OrderedHandlerInterceptorWebMvcConfigureAdapter {

    @Autowired
    private RateLimiterProperties properties;

    @Override
    public List<OrderedHandlerInterceptor<?>> addOrderedInterceptors() {
        final List<OrderedHandlerInterceptor<?>> interceptors = new ArrayList<>();

        final String[] patterns = this.properties.getPatterns().toArray(new String[0]);
        interceptors.add(new OrderedHandlerInterceptor<>(this.rateLimiterInterceptor(), patterns));

        return interceptors;
    }

    @Bean
    public RateLimiterInterceptor rateLimiterInterceptor() {
        return new RateLimiterInterceptor();
    }
}
