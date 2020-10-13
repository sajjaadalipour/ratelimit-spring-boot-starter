package com.github.sajjaadalipour.ratelimit.conf.properties;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.KeyGenerator;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitRepositoryKey.REDIS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link RateLimitProperties}.
 *
 * @author Sajjad Alipour
 */
class RateLimitPropertiesTest {

    @Test
    void isRepositoryNotNullWhenEnabled_WhenEnabledIsFalse_ShouldReturnFalse() {
        RateLimitProperties properties = new RateLimitProperties(false, 0, null, Collections.emptySet(), null);

        assertFalse(properties.isRepositoryNotNullWhenEnabled());
    }

    @Test
    void isRepositoryNotNullWhenEnabled_WhenEnabledIsTrueAndRepositoryIsEmpty_ShouldReturnFalse() {
        RateLimitProperties properties = new RateLimitProperties(true, 0, null, Collections.emptySet(), null);

        assertFalse(properties.isRepositoryNotNullWhenEnabled());
    }

    @Test
    void isRepositoryNotNullWhenEnabled_WhenEnabledIsTrueAndRepositoryIsNotEmpty_ShouldReturnTrue() {
        RateLimitProperties properties = new RateLimitProperties(true, 0, REDIS, Collections.emptySet(), null);

        assertTrue(properties.isRepositoryNotNullWhenEnabled());
    }

    @Test
    void isValidPolicyKeyGenerator_WhenGivenInvalidGeneratorName_ShouldReturnFalse() {
        Set<Policy> policies = Set.of(new Policy(Duration.ZERO, 1, "INVALID", Collections.emptySet(), null));

        RateLimitProperties properties = new RateLimitProperties(true, 0, REDIS, policies, Collections.emptySet());

        assertFalse(properties.isValidPolicyKeyGenerator());
    }

    @Test
    void isValidPolicyKeyGenerator_WhenExistsGenerator_ShouldReturnTrue() {
        Set<Policy> policies = Set.of(new Policy(Duration.ZERO, 1, "BY_IP", Collections.emptySet(), null));
        KeyGenerator keyGenerator = new KeyGenerator();
        keyGenerator.setName("BY_IP");
        RateLimitProperties properties = new RateLimitProperties(true, 0, REDIS, policies, Set.of(keyGenerator));

        assertTrue(properties.isValidPolicyKeyGenerator());
    }
}