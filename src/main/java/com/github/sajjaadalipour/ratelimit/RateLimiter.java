package com.github.sajjaadalipour.ratelimit;

import javax.annotation.Nonnull;

/**
 * Contract to cache the rate limit detail.
 *
 * @author Sajjad Alipour
 */
public interface RateLimiter {

    /**
     * @param ratePolicy Encapsulates the rate limit policy details.
     * @return Details of a requester`s rate limit.
     */
    Rate consume(@Nonnull RatePolicy ratePolicy);
}
