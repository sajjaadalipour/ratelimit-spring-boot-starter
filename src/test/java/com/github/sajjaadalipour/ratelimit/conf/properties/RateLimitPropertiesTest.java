package com.github.sajjaadalipour.ratelimit.conf.properties;

import com.github.sajjaadalipour.ratelimit.RateLimitKeyGenerator;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.KeyGenerator;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

import static com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitRepositoryKey.REDIS;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

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
        assertTrue(properties.isEnabled());
    }

    @Test
    void isRepositoryNotNullWhenEnabled_WhenEnabledIsTrueAndRepositoryIsNotEmpty_ShouldReturnTrue() {
        RateLimitProperties properties = new RateLimitProperties(true, 0, REDIS, Collections.emptySet(), null);

        assertTrue(properties.isRepositoryNotNullWhenEnabled());
    }

    @Test
    void isValidPolicyKeyGenerator_WhenGivenInvalidGeneratorName_ShouldReturnFalse() {
        Set<Policy> policies = Collections.singleton(new Policy(Duration.ZERO, 1, "INVALID", Collections.emptySet(), null, null));

        RateLimitProperties properties = new RateLimitProperties(true, 0, REDIS, policies, Collections.emptySet());

        assertFalse(properties.isValidPolicyKeyGenerator());
    }

    @Test
    void isValidPolicyKeyGenerator_WhenExistsGenerator_ShouldReturnTrue() {
        Set<Policy> policies = Collections.singleton(new Policy(Duration.ZERO, 1, "BY_IP", Collections.emptySet(), null, null));
        KeyGenerator keyGenerator = new KeyGenerator("BY_IP", null, null);
        RateLimitProperties properties = new RateLimitProperties(true, 0, REDIS, policies, Collections.singleton(keyGenerator));

        assertTrue(properties.isValidPolicyKeyGenerator());
    }

    @Test
    void checkKeyGeneratorEquality_ShouldNotEqual() throws ClassNotFoundException {
        Class<RateLimitKeyGenerator> classType = (Class<RateLimitKeyGenerator>) Class.forName("com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator");
        KeyGenerator keyGenerator1 = new KeyGenerator("a", classType, null);
        KeyGenerator keyGenerator2 = new KeyGenerator("ab", classType, null);

        assertNotEquals(keyGenerator1, keyGenerator2);
    }

    @Test
    void checkKeyGeneratorEquality_ShouldBeEqual() throws ClassNotFoundException {
        Class<RateLimitKeyGenerator> classType = (Class<RateLimitKeyGenerator>) Class.forName("com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator");
        KeyGenerator keyGenerator1 = new KeyGenerator("a", classType, null);
        KeyGenerator keyGenerator2 = new KeyGenerator("a", classType, null);

        assertEquals(keyGenerator1, keyGenerator2);
    }

    @Test
    void checkPolicyEquality1_ShouldNotBeEqual() {
        String generator = "com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator";
        Policy policy1 = new Policy(Duration.ZERO, 1, generator, null, null, null);
        Policy policy2 = new Policy(Duration.ofDays(1), 1, generator, null, null, null);

        assertNotEquals(policy1, policy2);
    }

    @Test
    void checkPolicyEquality2_ShouldNotBeEqual() {
        String generator = "com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator";
        Policy policy1 = new Policy(Duration.ZERO, 1, generator, null, null, null);
        Policy policy2 = new Policy(Duration.ZERO, 2, generator, null, null, null);

        assertNotEquals(policy1, policy2);
    }

    @Test
    void checkPolicyEquality_ShouldBeEqual() {
        String generator = "com.github.sajjaadalipour.ratelimit.generators.HeaderBasedKeyGenerator";
        Policy policy1 = new Policy(Duration.ZERO, 1, generator, null, null, null);
        Policy policy2 = new Policy(Duration.ZERO, 1, generator, null, null, null);

        assertEquals(policy1, policy2);
    }

    @Test
    void checkRouteEquality1_ShouldNotEqual() {
        Policy.Route route1 = new Policy.Route("/test", GET);
        Policy.Route route2 = new Policy.Route("/test", POST);

        Assertions.assertNotEquals(route1, route2);
    }

    @Test
    void checkRouteEquality2_ShouldNotEqual() {
        Policy.Route route1 = new Policy.Route("/test1", GET);
        Policy.Route route2 = new Policy.Route("/test2", GET);

        Assertions.assertNotEquals(route1, route2);
    }

    @Test
    void checkRouteEquality_ShouldEqual() {
        Policy.Route route1 = new Policy.Route("/test", GET);
        Policy.Route route2 = new Policy.Route("/test", GET);

        Assertions.assertEquals(route1, route2);
    }
}
