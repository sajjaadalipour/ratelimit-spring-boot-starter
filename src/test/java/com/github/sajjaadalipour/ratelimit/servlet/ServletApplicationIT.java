package com.github.sajjaadalipour.ratelimit.servlet;

import com.github.sajjaadalipour.ratelimit.repositories.redis.RateHash;
import com.github.sajjaadalipour.ratelimit.repositories.redis.RedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpMethod.*;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ServletApplicationIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RedisRepository redisRepository;

    @BeforeEach
    void clearRedis() {
        redisRepository.deleteAll();
    }

    @Test
    void requestForFirstTimeWithDeviceId_ShouldRecordARateInRedis() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test", GET, entity, Void.class);

        Iterable<RateHash> cachedRates = redisRepository.findAll();

        assertTrue(cachedRates.iterator().hasNext());
        assertEquals(0, cachedRates.iterator().next().getRemaining());
    }

    @Test
    void requestWith2DifferentMethod_ShouldCacheRatePerMethodType() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test", GET, entity, Void.class);
        restTemplate.exchange("/test", POST, entity, Void.class);

        Iterator<RateHash> cachedRates = redisRepository.findAll().iterator();

        assertTrue(cachedRates.hasNext());
        RateHash rateHash = cachedRates.next();
        assertEquals(0, rateHash.getRemaining());

        assertTrue(cachedRates.hasNext());
        rateHash = cachedRates.next();
        assertEquals(0, rateHash.getRemaining());
    }

    @Test
    void whenRateExceed_ShouldReturnHttpResponseStatus429() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Device-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/test", GET, entity, Void.class);

        assertEquals(429, exchange.getStatusCodeValue());

        Iterator<RateHash> cachedRates = redisRepository.findAll().iterator();

        assertTrue(cachedRates.hasNext());
        RateHash rateHash = cachedRates.next();
        assertEquals(-1, rateHash.getRemaining());
        assertEquals("/test_GET_PT5S_1_123", rateHash.getKey());
    }

    @Test
    void whenPolicyMethodTypeIsNull_ShouldCreatePerHttpMethodARateRecord() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Id", "123");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/testx", GET, entity, Void.class);
        restTemplate.exchange("/testx", POST, entity, Void.class);
        restTemplate.exchange("/testx", PUT, entity, Void.class);

        Iterator<RateHash> cachedRates = redisRepository.findAll().iterator();

        assertTrue(cachedRates.hasNext());
        RateHash rateHash = cachedRates.next();
        assertEquals(0, rateHash.getRemaining());

        assertTrue(cachedRates.hasNext());
        rateHash = cachedRates.next();
        assertEquals(0, rateHash.getRemaining());

        assertTrue(cachedRates.hasNext());
        rateHash = cachedRates.next();
        assertEquals(0, rateHash.getRemaining());
    }

    @Test
    void whenGivesTooManyRequest_ShouldBlockRequester() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-For", "0.0.0.0");
        HttpEntity<String> entity = new HttpEntity<>("body", headers);

        restTemplate.exchange("/test-block", GET, entity, Void.class);
        ResponseEntity<Void> exchange = restTemplate.exchange("/test-block", GET, entity, Void.class);

        assertEquals(429, exchange.getStatusCodeValue());
        assertNotNull(exchange.getHeaders().get(RETRY_AFTER));
        Iterator<RateHash> cachedRates = redisRepository.findAll().iterator();

        assertTrue(cachedRates.hasNext());
        RateHash rateHash = cachedRates.next();
        assertEquals(-1, rateHash.getRemaining());
        assertEquals("/test-block_GET_PT1S_1_0.0.0.0", rateHash.getKey());
    }
}
