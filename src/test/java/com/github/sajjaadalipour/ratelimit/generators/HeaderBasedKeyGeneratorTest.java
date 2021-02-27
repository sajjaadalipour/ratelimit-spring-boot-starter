package com.github.sajjaadalipour.ratelimit.generators;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link HeaderBasedKeyGenerator}.
 *
 * @author Sajjad Alipour
 */
class HeaderBasedKeyGeneratorTest {

    @Test
    void generateKey_GivenNullHeader_ShouldThrownHeaderNotPresentedException() {
        HeaderBasedKeyGenerator keyGenerator = new HeaderBasedKeyGenerator(Collections.singleton("X-Forwarded-For"));
        HttpServletRequest httpServletRequestMock = Mockito.mock(HttpServletRequest.class);

        Policy policy = new Policy(Duration.ofHours(1), 3, "TEST", null, null, null);

        Assertions.assertThrows(HeaderNotPresentedException.class, () -> keyGenerator.generateKey(httpServletRequestMock, policy));
    }

    @Test
    void generateKey_GivenOneParam_ShouldReturnAKeyWithCombinationOf5Things() {
        HeaderBasedKeyGenerator keyGenerator = new HeaderBasedKeyGenerator(Collections.singleton("X-Forwarded-For"));
        HttpServletRequest httpServletRequestMock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpServletRequestMock.getRequestURI()).thenReturn("/test");
        Mockito.when(httpServletRequestMock.getMethod()).thenReturn("GET");
        Mockito.when(httpServletRequestMock.getHeader(ArgumentMatchers.eq("X-Forwarded-For"))).thenReturn("0.0.0.0");

        Policy policy = new Policy(Duration.ofHours(1), 3, "TEST", null, null, null);

        String generatedKey = keyGenerator.generateKey(httpServletRequestMock, policy);

        assertEquals("/test_GET_PT1H_3_0.0.0.0", generatedKey);
        Assertions.assertEquals(5, generatedKey.split("_").length);
    }

    @Test
    void generateKey_GivenTwoParam_ShouldReturnAKey_WithCombinationOf6Things() {
        HeaderBasedKeyGenerator keyGenerator = new HeaderBasedKeyGenerator(new HashSet<>(Arrays.asList("X-Forwarded-For", "User-Id")));
        HttpServletRequest httpServletRequestMock = Mockito.mock(HttpServletRequest.class);
        Mockito.when(httpServletRequestMock.getRequestURI()).thenReturn("/test");
        Mockito.when(httpServletRequestMock.getMethod()).thenReturn("GET");
        Mockito.when(httpServletRequestMock.getHeader(ArgumentMatchers.eq("X-Forwarded-For"))).thenReturn("0.0.0.0");
        Mockito.when(httpServletRequestMock.getHeader(ArgumentMatchers.eq("User-Id"))).thenReturn("1234");

        Policy policy = new Policy(Duration.ofHours(1), 3, "TEST", null, null, null);

        String generatedKey = keyGenerator.generateKey(httpServletRequestMock, policy);

        Assertions.assertEquals(6, generatedKey.split("_").length);
    }
}