package com.github.sajjaadalipour.ratelimit.servlet;

import com.github.sajjaadalipour.ratelimit.Limiter;
import com.github.sajjaadalipour.ratelimit.RateLimiterAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ServletApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServletApplication.class, args);
    }

    @Bean
    RateLimiterAspect rateLimiterAspect() {
        return new RateLimiterAspect();
    }


    @Bean
    RT rt() {
        return new RT();
    }

    static class RT {

        @Limiter(count = 1, duration = "", key = "")
        public void test() {

        }
    }
}
