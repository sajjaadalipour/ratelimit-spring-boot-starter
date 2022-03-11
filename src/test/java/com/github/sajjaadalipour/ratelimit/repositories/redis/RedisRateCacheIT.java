package com.github.sajjaadalipour.ratelimit.repositories.redis;

import com.github.sajjaadalipour.ratelimit.Rate;
import com.github.sajjaadalipour.ratelimit.RateLimiter;
import com.github.sajjaadalipour.ratelimit.RatePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link RedisRateCache}.
 *
 * @author Sajjad Alipour
 */
@DataRedisTest
@SpringBootConfiguration
@EnableAutoConfiguration
class RedisRateCacheIT {

    private static final String KEY_PREFIX = "REDIS_KEY_PREFIX";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RateLimiter redisRateCache;

    @BeforeEach
    void flushRedis() {
        stringRedisTemplate.getRequiredConnectionFactory().getConnection().flushDb();
    }

    @Test
    void consume_ShouldCreateRateForFirstTime_ShouldCachedRateRemainingValueBeEqualWith2() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofDays(1), 3, null);
        redisRateCache.consume(ratePolicy);
        Optional<String> remaining = getValue("test");
        assertTrue(remaining.isPresent());
        assertEquals("2", remaining.get());
    }

    @Test
    void consume_WhenDoesNotExceed_TheRateRemainingValueShouldBeEqualWIth1() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofDays(1), 3, null);
        redisRateCache.consume(ratePolicy);
        redisRateCache.consume(ratePolicy);

        Optional<String> remaining = getValue("test");
        assertTrue(remaining.isPresent());

        assertEquals("1", remaining.get());
    }

    @Test
    void consume_WhenExceed_ShouldDecreaseRateRemainingBe0() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofMinutes(1), 1, null);
        redisRateCache.consume(ratePolicy);
        Rate rate = redisRateCache.consume(ratePolicy);

        Optional<String> remaining = getValue("test");
        assertTrue(remaining.isPresent());

        assertEquals("" + rate.getRemaining(), remaining.get());
    }

    @Test
    void consume_WhenExceedAndSetBlockPolicy_ShouldIncreaseExpiration2MinAndRateBeNegative1() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofMinutes(1), 1, Duration.ofMinutes(2));
        redisRateCache.consume(ratePolicy);
        Rate rate = redisRateCache.consume(ratePolicy);

        Optional<String> remaining = getValue("test");
        assertTrue(remaining.isPresent());

        assertEquals("" + rate.getRemaining(), remaining.get());
    }

    @Test
    void consume_WhenExceedAndExpire_ShouldRecordDeletedFromRedis() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofSeconds(1), 1, null);
        redisRateCache.consume(ratePolicy);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertFalse(getValue("test").isPresent()));
    }

    @Test
    void consume_WhenExceedAndNotExpire_ShouldReturnRateWith0Remaining() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofSeconds(10), 1, null);
        redisRateCache.consume(ratePolicy);
        Optional<String> remaining = getValue("test");
        assertTrue(remaining.isPresent());
        assertEquals("0", remaining.get());
    }

    private Optional<String> getValue(String key) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(KEY_PREFIX + ":" + key));
    }

    @TestConfiguration
    static class RateLimitTestConfig {
        @Bean
        public RateLimiter redisRateCache(StringRedisTemplate stringRedisTemplate) {
            return new RedisRateCache(stringRedisTemplate, KEY_PREFIX);
        }
    }
}