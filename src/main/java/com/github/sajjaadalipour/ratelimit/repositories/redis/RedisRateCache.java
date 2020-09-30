package com.github.sajjaadalipour.ratelimit.repositories.redis;

import com.github.sajjaadalipour.ratelimit.Rate;
import com.github.sajjaadalipour.ratelimit.RateLimiter;
import com.github.sajjaadalipour.ratelimit.RatePolicy;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Optional;

/**
 * An implementation of {@link RateLimiter} to cache the rate limit data in redis.
 *
 * @author Sajjad Alipour
 */
public class RedisRateCache implements RateLimiter {

    /**
     * Used to persist and retrieve from to redis.
     */
    private final RedisRepository redisRepository;

    public RedisRateCache(RedisRepository redisRepository) {
        this.redisRepository = redisRepository;
    }

    /**
     * Finds the rate record from Redis by the given {@code key}, if does not exists then creates a new record
     * else checks the rate remaining value is greater than 0, decrease rate`s remaining and update item.
     *
     * @param ratePolicy Encapsulates the rate limit policy details.
     * @return Encapsulated rate details.
     */
    @Override
    public synchronized Rate consume(@Nonnull RatePolicy ratePolicy) {
        Optional<RateHash> optionalRate = redisRepository.findById(ratePolicy.getKey());

        if (optionalRate.isEmpty()) {
            return createRateForFirstTime(ratePolicy);
        }

        RateHash rateHash = optionalRate.get();
        Rate rate = new Rate(
                rateHash.getKey(),
                rateHash.getExpiration(),
                rateHash.getRemaining()
        );

        if (!rate.isExceed()) {
            rate.decrease();
            rateHash.setRemaining(rate.getRemaining());

            if (rate.isExceed() && ratePolicy.getBlockDuration() != null) {
                final Instant blockedExpiration = Instant.now().plusSeconds(ratePolicy.getBlockDuration().getSeconds());
                rateHash.setExpiration(blockedExpiration);
            }

            redisRepository.save(rateHash);
        }

        return rate;
    }

    private Rate createRateForFirstTime(RatePolicy ratePolicy) {
        Instant expiration = Instant.now().plusSeconds(ratePolicy.getDuration().getSeconds());
        RateHash rateHash = new RateHash(ratePolicy.getKey(), expiration, ratePolicy.getCount() - 1);
        redisRepository.save(rateHash);

        return new Rate(ratePolicy.getKey(), expiration, rateHash.getRemaining());
    }
}
