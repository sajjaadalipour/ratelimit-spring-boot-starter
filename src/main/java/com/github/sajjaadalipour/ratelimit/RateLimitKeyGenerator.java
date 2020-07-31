package com.github.sajjaadalipour.ratelimit;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;

import javax.servlet.http.HttpServletRequest;

/**
 * Defines a contract to generate a key from the Http servlet request.
 *
 * @author Sajjad Alipour
 */
public interface RateLimitKeyGenerator {

    /**
     * Returns the generated key.
     *
     * @param servletRequest Encapsulates the http servlet request.
     * @param policy         Encapsulates the rate limit policy properties.
     * @return Generated key.
     */
    String generateKey(HttpServletRequest servletRequest, Policy policy);
}
