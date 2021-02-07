package com.github.sajjaadalipour.ratelimit.repositories;

import com.github.sajjaadalipour.ratelimit.Rate;
import com.github.sajjaadalipour.ratelimit.RatePolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static com.github.sajjaadalipour.ratelimit.Rate.RATE_BLOCK_STATE;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link InMemoryRateCache}.
 *
 * @author Sajjad Alipour
 */
class InMemoryRateCacheTest {

    @Test
    void consume_CreateRateForFirstTime_TheRateRemainingValueShouldBeEqualWIth2() {
        InMemoryRateCache inMemoryRateCache = new InMemoryRateCache();
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofDays(1), 3, null);
        Rate rate = inMemoryRateCache.consume(ratePolicy);

        assertEquals(2, rate.getRemaining());
        assertEquals("test", rate.getKey());
    }

    @Test
    void consume_WhenRateRecordExpired_ShouldCreateNewRate_TheRateRemainingValueShouldBeEqualWIth2() {
        InMemoryRateCache inMemoryRateCache = new InMemoryRateCache();
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofSeconds(1), 3, null);
        inMemoryRateCache.consume(ratePolicy);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(
                () -> {
                    Rate rate = inMemoryRateCache.consume(ratePolicy);

                    assertEquals(2, rate.getRemaining());
                    assertEquals("test", rate.getKey());
                }
        );
    }

    @Test
    void consume_WhenDoesNotExceedAndDoesNotBlocked_TheRateRemainingValueShouldBeEqualWIth1() {
        InMemoryRateCache inMemoryRateCache = new InMemoryRateCache();
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofDays(1), 3, null);
        inMemoryRateCache.consume(ratePolicy);
        Rate rate = inMemoryRateCache.consume(ratePolicy);

        assertEquals(1, rate.getRemaining());
        assertEquals("test", rate.getKey());
    }

    @Test
    void consume_WhenExceedAndBlocked_ShouldIncreaseExpiration2MinAndRateBeNegative2() {
        InMemoryRateCache inMemoryRateCache = new InMemoryRateCache();
        RatePolicy ratePolicy = new RatePolicy("test", Duration.ofMinutes(1), 1, Duration.ofMinutes(2));
        inMemoryRateCache.consume(ratePolicy);
        Rate rate = inMemoryRateCache.consume(ratePolicy);

        long expirationDiff = rate.getExpiration().getEpochSecond() - Instant.now().getEpochSecond();

        assertEquals(120, expirationDiff);
        assertEquals(RATE_BLOCK_STATE, rate.getRemaining());
    }
}
