package com.github.sajjaadalipour.ratelimit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

/**
 * Encapsulates the rate limit policy details to consume.
 *
 * @author Sajjad Alipour
 */
public final class RatePolicy {

    /**
     * Unique key that identifies a request.
     */
    private final String key;

    /**
     * The limitation duration.
     */
    private final Duration duration;

    /**
     * Represents the max number of calls.
     */
    private final Integer count;

    /**
     * Represents the duration of blocking.
     */
    private Duration blockDuration;

    public RatePolicy(@Nonnull String key, @Nonnull Duration duration, @Nonnull Integer count, @Nullable Duration blockDuration) {
        this.key = key;
        this.duration = duration;
        this.count = count;
        this.blockDuration = blockDuration;
    }

    public String getKey() {
        return key;
    }

    public Duration getDuration() {
        return duration;
    }

    public Integer getCount() {
        return count;
    }

    public Duration getBlockDuration() {
        return blockDuration;
    }
}
