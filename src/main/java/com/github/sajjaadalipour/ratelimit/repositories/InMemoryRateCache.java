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
        Rate currentRate;

        if (!rateOptional.isPresent()) {
            Instant expiration = Instant.now().plusSeconds(ratePolicy.getDuration().getSeconds());
            currentRate = new Rate(ratePolicy.getKey(), expiration, ratePolicy.getCount());
        } else {
            currentRate = rateOptional.get();
        }

        if (currentRate.isExpired()) {
            cache.remove(ratePolicy.getKey());
            return currentRate;
        }

        Rate newRate = currentRate;
        if (!currentRate.isExceed()) {
            newRate = new Rate(currentRate.getKey(), currentRate.getExpiration(), currentRate.getRemaining() - 1);
        }

        if (newRate.isExceed() && ratePolicy.getBlockDuration() != null) {
            Instant blockedExpiration = Instant.now().plusSeconds(ratePolicy.getBlockDuration().getSeconds());
            newRate = Rate.blocked(currentRate.getKey(), blockedExpiration);
        }

        cache.put(ratePolicy.getKey(), newRate);
        return newRate;
    }
}
