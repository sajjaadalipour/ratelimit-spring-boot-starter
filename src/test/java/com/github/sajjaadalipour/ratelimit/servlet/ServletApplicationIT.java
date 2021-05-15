package com.github.sajjaadalipour.ratelimit.servlet;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static com.github.sajjaadalipour.ratelimit.Rate.RATE_BLOCK_STATE;
import static com.github.sajjaadalipour.ratelimit.Rate.RATE_EXCEED_STATE;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ServletApplicationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RateLimitProperties rateLimitProperties;

    @BeforeEach
    void flushRedis() {
        stringRedisTemplate.getRequiredConnectionFactory().getConnection().flushDb();
    }

    @Test
    void noMatchPolicy_ShouldNotLimit() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/noLimit", GET, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);
        assertTrue(keys.isEmpty());
    }

    @Test
    void requestForFirstTime_WhenDuration5sCount1_ShouldRecordARateInRedisWith0Remaining() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/firstTime", GET, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);

        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());
        assertEquals("0", remaining);
    }

    @Test
    void requestWithDifferentMethod_ShouldCacheRatePerMethodType() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/diffMethod", GET, entity, Void.class);
        restTemplate.exchange("/diffMethod", POST, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);
        assertTrue(keys.stream().anyMatch(it -> it.contains("/diffMethod_GET") || it.contains("/diffMethod_POST")));
    }

    @Test
    void whenRateExceed_Call2Request_ShouldReturnHttpResponseStatus429AndRemainingBeNegative1() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/exceed", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/exceed", GET, entity, Void.class);

        assertEquals(TOO_MANY_REQUESTS.value(), exchange.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);

        assertTrue(keys.iterator().hasNext());
        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());
        assertEquals(RATE_EXCEED_STATE.toString(), remaining);
    }

    @Test
    void whenRateNotExceed_ShouldGivenOk200StatusCode() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        ResponseEntity<Void> exchange = restTemplate.exchange("/notExceed", GET, entity, Void.class);

        assertEquals(200, exchange.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);

        assertTrue(keys.iterator().hasNext());
        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());

        assertEquals("0", remaining);
    }

    @Test
    void whenHaveBlockPolicy_AfterExceed_ShouldBlockRequesterAndRemainingBeNegative2() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "0.0.0.0");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/block", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/block", GET, entity, Void.class);

        assertEquals(TOO_MANY_REQUESTS.value(), exchange.getStatusCodeValue());
        assertNotNull(exchange.getHeaders().get(RETRY_AFTER));
        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);

        assertTrue(keys.iterator().hasNext());
        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());

        assertEquals(RATE_BLOCK_STATE.toString(), remaining);
    }

    @Test
    void whenRequestMatchesWIth2DiffPolicy_ShouldRecordRatePerPolicy() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/diffPolicy", GET, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);
        assertEquals(2, keys.size());

        String remaining1 = stringRedisTemplate.opsForValue().get(keys.iterator().next());
        assertEquals("0", remaining1);

        String remaining2 = stringRedisTemplate.opsForValue().get(keys.iterator().next());
        assertEquals("0", remaining2);
    }

    @Test
    void whenDefinedAGlobalPolicyWithAExcludeRoutes_ShouldIgnoreLimitingTheDefinedExcludeRoutes() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/global/policy1", GET, entity, Void.class);
        restTemplate.exchange("/global/excluded", GET, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(rateLimitProperties.getKeyPrefix() + "*");
        assertNotNull(keys);
        assertEquals(1, keys.size());

        String key = keys.iterator().next();
        assertTrue(key.contains("policy1_GET"));

        String remaining1 = stringRedisTemplate.opsForValue().get(key);
        assertEquals("0", remaining1);
    }
}
