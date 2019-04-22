package top.infra.web.servlet.handler;

import com.google.common.collect.Lists;

import java.util.List;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("interceptor.rate-limiter")
@Data
public class RateLimiterProperties {

    private List<String> patterns = Lists.newArrayList();
}
