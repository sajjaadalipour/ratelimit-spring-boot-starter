package com.github.sajjaadalipour.ratelimit.repositories.redis;

import com.github.sajjaadalipour.ratelimit.Rate;
import com.github.sajjaadalipour.ratelimit.RateLimiter;
import com.github.sajjaadalipour.ratelimit.RatePolicy;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static com.github.sajjaadalipour.ratelimit.Rate.RATE_BLOCK_STATE;
import static com.github.sajjaadalipour.ratelimit.Rate.RATE_EXCEED_STATE;

/**
 * An implementation of {@link RateLimiter} to cache the rate limit data in redis.
 *
 * @author Sajjad Alipour
 */
public class RedisRateCache implements RateLimiter {

    private final String redisKeyGroup;

    /**
     * Used to persist and retrieve from to redis.
     */
    private final StringRedisTemplate stringRedisTemplate;

    public RedisRateCache(StringRedisTemplate stringRedisTemplate, String redisKeyGroup) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisKeyGroup = redisKeyGroup;
    }

    /**
     * Finds the rate record from Redis by the given {@code key}, if does not exists then creates a new record
     * else checks the rate remaining value is greater than 0, decrease rate`s remaining and update item.
     *
     * @param ratePolicy Encapsulates the rate limit policy details.
     * @return Encapsulated rate details.
     */
    @Override
    public Rate consume(@Nonnull RatePolicy ratePolicy) {
        String redisKey = assignPrefixKey(ratePolicy.getKey());
        Optional<String> optionalRate = Optional.ofNullable(stringRedisTemplate.opsForValue().get(redisKey));

        if (!optionalRate.isPresent()) {
            return createRateForFirstTime(ratePolicy);
        }

        int rateRemaining = Integer.parseInt(optionalRate.get());

        Instant expiration = Instant.now().plusSeconds(ratePolicy.getDuration().getSeconds());

        if (rateRemaining > RATE_EXCEED_STATE) {
            rateRemaining--;
            stringRedisTemplate.opsForValue().decrement(redisKey);
        }

        if (ratePolicy.getBlockDuration() != null && rateRemaining == RATE_EXCEED_STATE) {
            rateRemaining = RATE_BLOCK_STATE;
            createRedisRecord(redisKey, rateRemaining, ratePolicy.getBlockDuration());
        }

        if (rateRemaining == RATE_BLOCK_STATE && ratePolicy.getBlockDuration() != null) {
            expiration = Instant.now().plusSeconds(ratePolicy.getBlockDuration().getSeconds());
        }

        return new Rate(ratePolicy.getKey(), expiration, rateRemaining);
    }

    private Rate createRateForFirstTime(RatePolicy ratePolicy) {
        Instant expiration = Instant.now().plusSeconds(ratePolicy.getDuration().getSeconds());
        int remaining = ratePolicy.getCount() - 1;

        createRedisRecord(assignPrefixKey(ratePolicy.getKey()), remaining, ratePolicy.getDuration());

        return new Rate(ratePolicy.getKey(), expiration, remaining);
    }

    private void createRedisRecord(String key, int remaining, Duration expiration) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(remaining), expiration);
    }

    private String assignPrefixKey(String key) {
        return redisKeyGroup + ":" + key;
    }
}
