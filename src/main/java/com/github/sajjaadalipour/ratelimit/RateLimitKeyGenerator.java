package com.github.sajjaadalipour.ratelimit;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;

/**
 * Defines a contract to generate a key from the Http servlet request.
 *
 * @author Sajjad Alipour
 * @author Mehran Behnam
 */
public interface RateLimitKeyGenerator {

    /**
     * Returns the generated key.
     *
     * @param object Encapsulates the http servlet request.
     * @param policy Encapsulates the rate limit policy properties.
     * @return Generated key.
     */
    Object generateKey(Policy policy, Object... object);
}