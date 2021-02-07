package com.github.sajjaadalipour.ratelimit;

import java.time.Instant;

/**
 * Represents a detail of rate limit in giving time for a requester.
 *
 * @author Sajjad Alipour
 */
public final class Rate {

    public static final Integer RATE_BLOCK_STATE = -2;
    public static final Integer RATE_EXCEED_STATE = -1;

    /**
     * Represents the key of rate limit.
     */
    private final String key;

    /**
     * The expiration time of the rate limit.
     */
    private final Instant expiration;

    /**
     * How many requests can be executed by the requester.
     */
    private final Integer remaining;

    public Rate(String key, Instant expiration, Integer remaining) {
        this.key = key;
        this.expiration = expiration;
        this.remaining = remaining;
    }

    public String getKey() {
        return key;
    }

    public Instant getExpiration() {
        return expiration;
    }

    public Integer getRemaining() {
        return remaining;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiration);
    }

    public boolean isExceed() {
        return remaining.equals(RATE_EXCEED_STATE);
    }

    public boolean isBlocked() {
        return remaining.equals(RATE_BLOCK_STATE);
    }

    public static Rate blocked(String key, Instant expiration) {
        return new Rate(key, expiration, RATE_BLOCK_STATE);
    }
}
