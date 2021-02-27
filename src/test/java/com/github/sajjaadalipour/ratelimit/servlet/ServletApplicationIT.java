package com.github.sajjaadalipour.ratelimit.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static com.github.sajjaadalipour.ratelimit.repositories.redis.RedisRateCache.REDIS_KEY_GROUP;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpMethod.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ServletApplicationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void flushRedis() {
        stringRedisTemplate.getRequiredConnectionFactory().getConnection().flushDb();
    }

    @Test
    void requestForFirstTimeWithDeviceId_ShouldRecordARateInRedis() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test", GET, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");

        assert keys != null;
        assertEquals(keys.size(), 3);
        assertEquals("0", stringRedisTemplate.opsForValue().get(keys.iterator().next()));
    }

    @Test
    void requestWith2DifferentMethod_ShouldCacheRatePerMethodType() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test", GET, entity, Void.class);
        restTemplate.exchange("/test", POST, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assert keys != null;
        assertEquals(keys.size(), 5);

        Iterator<String> keysIterator = keys.iterator();

        String remaining = stringRedisTemplate.opsForValue().get(keysIterator.next());
        assertEquals("3", remaining);
    }

    @Test
    void whenRateExceed_ShouldReturnHttpResponseStatus429() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/testx", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/testx", GET, entity, Void.class);

        assertEquals(429, exchange.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assertNotNull(keys);

        assertTrue(keys.iterator().hasNext());
        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());
        assertEquals("-1", remaining);
    }

    @Test
    void whenPolicyMethodTypeIsNull_ShouldCreatePerHttpMethodARateRecord() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/testx", GET, entity, Void.class);
        restTemplate.exchange("/testx", POST, entity, Void.class);
        restTemplate.exchange("/testx", PUT, entity, Void.class);

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assertNotNull(keys);

        Iterator<String> keysIterator = keys.iterator();
        assertTrue(keysIterator.hasNext());

        String remaining = stringRedisTemplate.opsForValue().get(keysIterator.next());
        assertNotNull(remaining);
        assertEquals("0", remaining);

        remaining = stringRedisTemplate.opsForValue().get(keysIterator.next());
        assertNotNull(remaining);
        assertEquals("0", remaining);

        remaining = stringRedisTemplate.opsForValue().get(keysIterator.next());
        assertNotNull(remaining);
        assertEquals("0", remaining);
    }

    @Test
    void whenGivesTooManyRequest_ShouldBlockRequester() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "0.0.0.0");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test-block", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/test-block", GET, entity, Void.class);

        assertEquals(429, exchange.getStatusCodeValue());
        assertNotNull(exchange.getHeaders().get(RETRY_AFTER));
        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assertNotNull(keys);

        assertTrue(keys.iterator().hasNext());
        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());

        assertEquals("-2", remaining);
    }

    @Test
    void whenRateNotExceed_ShouldGivenOk200StatusCode() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        ResponseEntity<Void> exchange = restTemplate.exchange("/test", GET, entity, Void.class);

        assertEquals(200, exchange.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assertNotNull(keys);

        assertTrue(keys.iterator().hasNext());
        String remaining = stringRedisTemplate.opsForValue().get(keys.iterator().next());

        assertEquals("0", remaining);
    }

    @Test
    void whenRateExceedState_ShouldGivenTooManyRequest429StatusCode() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange("/test", GET, entity, Void.class);
        restTemplate.exchange("/test", GET, entity, Void.class);
        ResponseEntity<Void> thirdResponse = restTemplate.exchange("/test", GET, entity, Void.class);

        assertEquals(429, thirdResponse.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assertNotNull(keys);
        assertEquals("0", stringRedisTemplate.opsForValue().get(keys.iterator().next()));
    }

    @Test
    void whenRateExceedState_ShouldGivenTooManyRequest429StatusCodeAndBlock() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "127.0.0.1");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange("/test-block", GET, entity, Void.class);
        ResponseEntity<Void> secondResponse = restTemplate.exchange("/test-block", GET, entity, Void.class);

        assertEquals(429, secondResponse.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assertNotNull(keys);
        assertEquals("-2", stringRedisTemplate.opsForValue().get(keys.iterator().next()));
    }

    @Test
    void whenRateConsistTwoIdenticalDuration_ShouldConsiderMinCount() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test/test/x", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/test/test/x", GET, entity, Void.class);

        assertEquals(200, exchange.getStatusCodeValue());

        Set<String> keys = stringRedisTemplate.keys(REDIS_KEY_GROUP + "*");
        assert keys != null;
        assertEquals(keys.size(), 4);

        Map<String, String> keyMap = new HashMap<>();
        keyMap.put("RATE_LIMITER_RATES:/test/test/x_GET_PT4M_2_123", "1");
        keyMap.put("RATE_LIMITER_RATES:/test/test/x_GET_PT1M_2_123", "1");
        keyMap.put("RATE_LIMITER_RATES:/test/test/x_GET_PT2S_4_123", "3");
        keyMap.put("RATE_LIMITER_RATES:/test/test/x_GET_PT1S_1_123", "-1");

        keyMap.forEach((key, value) -> assertEquals(value, stringRedisTemplate.opsForValue().get(key)));

    }

}
