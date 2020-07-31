package com.github.sajjaadalipour.ratelimit.repositories.redis;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import javax.annotation.Nonnull;
import java.time.Instant;

/**
 * Encapsulates the rate limit detail to sore in redis.
 *
 * @author Sajjad Alipour
 */
@RedisHash("rates")
public class RateHash {

    @Id
    private final String key;

    /**
     * The expiration time of the rate limit.
     */
    private Instant expiration;

    /**
     * How many requests can be executed by the requester.
     */
    private Integer remaining;

    @TimeToLive
    private Long ttl;

    public RateHash(@Nonnull String key, @Nonnull Instant expiration, @Nonnull Integer remaining) {
        this.key = key;
        setExpiration(expiration);
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

    public void setRemaining(Integer remaining) {
        this.remaining = remaining;
    }

    public void setExpiration(Instant expiration) {
        this.expiration = expiration;
        this.setTtl(expiration.getEpochSecond() - Instant.now().getEpochSecond());
    }

    public void setTtl(Long ttl) {
        this.ttl = ttl;
    }
}
