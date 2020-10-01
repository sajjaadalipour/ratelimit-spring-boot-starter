package com.github.sajjaadalipour.ratelimit.repositories;

import com.github.sajjaadalipour.ratelimit.Rate;
import com.github.sajjaadalipour.ratelimit.RateLimiter;
import com.github.sajjaadalipour.ratelimit.RatePolicy;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An implementation of {@link RateLimiter} to cache the rate limit data in memory.
 *
 * @author Sajjad Alipour
 */
public class InMemoryRateCache implements RateLimiter {

    private final ConcurrentHashMap<String, Rate> cache = new ConcurrentHashMap<>();

    /**
     * Gets the rate limit by the given {@code key} from the {@link #cache} hash map,
     * then if result is {@code null}, put the new rate to {@link #cache}, else if rate expired
     * then remove the item from the {@link #cache}. If the rate remaining value be grater than 0,
     * decrease rate`s remaining and update item.
     *
     * @return Encapsulated rate details.
     */
    @Override
    public synchronized Rate consume(@Nonnull RatePolicy ratePolicy) {
        Optional<Rate> rateOptional = Optional.ofNullable(cache.get(ratePolicy.getKey()));

        if (rateOptional.isEmpty()) {
            return createRateForFirstTime(ratePolicy);
        }

        Rate rate = rateOptional.get();
        if (rate.isExpired()) {
            cache.remove(ratePolicy.getKey());
            return createRateForFirstTime(ratePolicy);
        }

        if (!rate.isExceed()) {
            rate.decrease();

            if (rate.isExceed() && ratePolicy.getBlockDuration() != null) {
                final Instant blockedExpiration = Instant.now().plusSeconds(ratePolicy.getBlockDuration().getSeconds());
                rate.setExpiration(blockedExpiration);
            }

            cache.put(ratePolicy.getKey(), rate);
        }

        return rate;
    }

    private Rate createRateForFirstTime(RatePolicy ratePolicy) {
        Instant expiration = Instant.now().plusSeconds(ratePolicy.getDuration().getSeconds());
        Rate rate = new Rate(ratePolicy.getKey(), expiration, ratePolicy.getCount());
        rate.decrease();

        cache.put(ratePolicy.getKey(), rate);
        return rate;
    }
}
