package com.github.sajjaadalipour.ratelimit.repositories.redis;

import com.github.sajjaadalipour.ratelimit.RatePolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link RedisRateCache}.
 *
 * @author Sajjad Alipour
 */
@DataRedisTest
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(RedisRateCache.class)
class RedisRateCacheIT {

    @Autowired
    private RedisRepository redisRepository;

    @Autowired
    private RedisRateCache redisRateCache;

    @BeforeEach
    void flushRedis() {
        redisRepository.deleteAll();
    }

    @Test
    void consume_ShouldCreateRateForFirstTime_ShouldCachedRateRemainingValueBeEqualWith2() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofDays(1), 3, null);
        redisRateCache.consume(ratePolicy);
        Optional<RateHash> rateHash = redisRepository.findById("test");
        assertTrue(rateHash.isPresent());
        assertEquals(2, rateHash.get().getRemaining());

        long hoursDiff = Duration.between(Instant.now(), rateHash.get().getExpiration()).toHours();
        assertTrue(hoursDiff >= 23);
    }

    @Test
    void consume_WhenDoesNotExceed_TheRateRemainingValueShouldBeEqualWIth1() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofDays(1), 3, null);
        redisRateCache.consume(ratePolicy);
        redisRateCache.consume(ratePolicy);

        Optional<RateHash> rateHash = redisRepository.findById("test");
        assertTrue(rateHash.isPresent());

        assertEquals(1, rateHash.get().getRemaining());
        assertEquals("test", rateHash.get().getKey());
    }

    @Test
    void consume_WhenExceedWithoutBlocking_ShouldIncreaseExpiration2MinAndRateBe0() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofMinutes(1), 1, null);
        redisRateCache.consume(ratePolicy);
        redisRateCache.consume(ratePolicy);

        Optional<RateHash> rateHash = redisRepository.findById("test");
        assertTrue(rateHash.isPresent());

        assertEquals(-1, rateHash.get().getRemaining());
    }

    @Test
    void consume_WhenExceed_ShouldIncreaseExpiration2MinAndRateBe0() {
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofMinutes(1), 1, Duration.ofMinutes(2));
        redisRateCache.consume(ratePolicy);
        redisRateCache.consume(ratePolicy);

        Optional<RateHash> rateHash = redisRepository.findById("test");
        assertTrue(rateHash.isPresent());

        assertEquals(-1, rateHash.get().getRemaining());
    }
}