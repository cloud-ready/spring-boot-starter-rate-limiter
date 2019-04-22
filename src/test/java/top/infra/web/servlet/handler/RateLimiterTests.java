package top.infra.web.servlet.handler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.BatchResult;
import org.redisson.api.RBatch;
import org.redisson.api.RMapCacheAsync;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.GenericContainer;

import top.infra.test.containers.GenericContainerInitializer;
import top.infra.test.containers.InitializerCallbacks;

@RunWith(SpringRunner.class)
@Slf4j
@SpringBootTest( //
    classes = RateLimiterTests.RateLimiterTestApplication.class, //
    properties = "spring.main.web-application-type=servlet", //
    webEnvironment = RANDOM_PORT
)
@ContextConfiguration(initializers = GenericContainerInitializer.class)
public class RateLimiterTests {

    @SpringBootApplication
    @RestController
    public static class RateLimiterTestApplication {

        @RateLimiter(application = "rate-limiter-test", key = "/echo/{str}", limitation = 2, expire = 10, timeUnit = SECONDS)
        @GetMapping(path = "/echo/{str}", produces = TEXT_PLAIN_VALUE)
        public String echo(@PathVariable("str") final String str) {
            return str;
        }

        public static void main(final String... args) {
            SpringApplication.run(RateLimiterTestApplication.class, args);
        }
    }

    @ClassRule
    public static final GenericContainer container = new GenericContainer("redis:3.0.2")
        .withExposedPorts(6379);

    static {
        GenericContainerInitializer.onInitialize(container, InitializerCallbacks.SPRING_DATA_REDIS);
    }

    @Autowired
    private RateLimiterInterceptor rateLimiterInterceptor;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final String cacheName = RateLimiterInterceptor.cacheName("rate-limiter-test");
    private final String key = "/echo/{str}";

    @Before
    public void setUp() {
        final RBatch batch = this.rateLimiterInterceptor.batch();
        final RMapCacheAsync<String, Integer> cache = batch.getMapCache(this.cacheName);
        cache.fastRemoveAsync(this.key);
        cache.getAsync(this.key);
        final BatchResult batchResult = batch.execute();

        log.info("setUp {}: {}", this.key, batchResult.getResponses().get(1));
    }

    @After
    public void clean() {
        final RBatch batch = this.rateLimiterInterceptor.batch();
        final RMapCacheAsync<String, Integer> cache = batch.getMapCache(this.cacheName);
        cache.getAsync(this.key);
        cache.fastRemoveAsync(this.key);
        final BatchResult batchResult = batch.execute();

        log.info("clean {}: {}", this.key, batchResult.getResponses().get(0));
    }

    @Test
    public void testRateLimiter() throws IOException {
        final ResponseEntity<String> re1 = this.restTemplate.getForEntity("/echo/{str}", String.class, "1");
        assertEquals("1", re1.getBody());
        assertEquals(HttpStatus.OK, re1.getStatusCode());

        final ResponseEntity<String> re2 = this.restTemplate.getForEntity("/echo/{str}", String.class, "2");
        assertEquals("2", re2.getBody());
        assertEquals(HttpStatus.OK, re2.getStatusCode());

        final ResponseEntity<String> re3 = this.restTemplate.getForEntity("/echo/{str}", String.class, "3");
        final Map<String, String> error = this.objectMapper.readValue(re3.getBody(), new TypeReference<Map<String, String>>() {
        });
        assertEquals("Rate limit exceeded", error.get("message"));
        assertEquals(HttpStatus.FORBIDDEN, re3.getStatusCode());
    }
}
