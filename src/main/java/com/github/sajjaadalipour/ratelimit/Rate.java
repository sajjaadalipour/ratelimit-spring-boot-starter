package com.github.sajjaadalipour.ratelimit;

import java.time.Instant;

/**
 * Represents a detail of rate limit in giving time for a requester.
 *
 * @author Sajjad Alipour
 */
public final class Rate {

    /**
     * Represents the key of rate limit.
     */
    private final String key;

    /**
     * The expiration time of the rate limit.
     */
    private Instant expiration;

    /**
     * How many requests can be executed by the requester.
     */
    private Integer remaining;

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

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
    }

    /**
     * Decrease the {@link #remaining}.
     */
    public void decrease() {
        this.remaining--;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiration);
    }

    public boolean isExceed() {
        return remaining < 0;
    }
}
