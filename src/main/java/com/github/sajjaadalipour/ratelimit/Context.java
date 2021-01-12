package com.github.sajjaadalipour.ratelimit;

import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface Context {
    Rate consume(RatePolicy ratePolicy);

    boolean isMatch(String uri, RateLimitProperties.Policy.Route route);

    RatePolicy getRatePolicy(HttpServletRequest httpServletRequest, RateLimitProperties.Policy policy);

    RateLimitProperties getRateLimitProperties();

    List<RateLimitProperties.Policy> getMatchedPolicies(String uri, String method);

    int getFilterOrder();

    void handle(HttpServletResponse httpServletResponse, Rate rate) throws IOException;

    List<RateLimitProperties.Policy> getMatchedPolicies(HttpServletRequest httpServletRequest);
}