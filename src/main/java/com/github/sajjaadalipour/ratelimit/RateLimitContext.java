package com.github.sajjaadalipour.ratelimit;

import com.github.sajjaadalipour.ratelimit.conf.error.TooManyRequestErrorHandler;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

/**
 * @author Mehran Behnam
 */
public class RateLimitContext implements Context {

    /**
     * A utility class to path matching.
     */
    private final PathMatcher pathMatcher = new AntPathMatcher();
    /**
     * Used to handle too many request error.
     */
    private final TooManyRequestErrorHandler tooManyRequestErrorHandler;
    /**
     * Encapsulates the rate limit properties.
     */
    private final RateLimitProperties rateLimitProperties;
    /**
     * Used to rate limiting.
     */
    private final RateLimiter rateLimiter;

    /**
     * Provides a map of key generators.
     */
    private final Map<String, RateLimitKeyGenerator> keyGenerators;

    public RateLimitContext(TooManyRequestErrorHandler tooManyRequestErrorHandler,
                            RateLimitProperties rateLimitProperties,
                            RateLimiter rateLimiter,
                            Map<String, RateLimitKeyGenerator> keyGenerators) {
        this.tooManyRequestErrorHandler = tooManyRequestErrorHandler;
        this.rateLimitProperties = rateLimitProperties;
        this.rateLimiter = rateLimiter;
        this.keyGenerators = keyGenerators;
    }

    @Override
    public Rate consume(RatePolicy ratePolicy) {
        return rateLimiter.consume(ratePolicy);
    }

    @Override
    public boolean isMatch(String uri, RateLimitProperties.Policy.Route route) {
        return pathMatcher.match(route.getUri(), uri);
    }

    @Override
    public RatePolicy getRatePolicy(HttpServletRequest httpServletRequest, RateLimitProperties.Policy policy) {
        final RateLimitKeyGenerator rateLimitKeyGenerator = getKeyGenerator(policy);
        final String generatedKey = (String) rateLimitKeyGenerator.generateKey(policy, httpServletRequest);
        return new RatePolicy(
                generatedKey,
                policy.getDuration(),
                policy.getCount(),
                (policy.getBlock() != null) ? policy.getBlock().getDuration() : null);
    }

    @Override
    public RateLimitProperties getRateLimitProperties() {
        return rateLimitProperties;
    }

    @Override
    public List<RateLimitProperties.Policy> getMatchedPolicies(String uri, String method) {
        return getPolicies()
                .stream()
                .filter(it -> it.getRoutes().stream().anyMatch(route -> {
                    if (route.getMethod() == null)
                        return isMatch(uri, route);

                    return isMatch(uri, route) && route.getMethod().name().equals(method);
                })).sorted(comparing(RateLimitProperties.Policy::getDuration)).collect(toList());
    }

    private List<RateLimitProperties.Policy> getPolicies() {
        return rateLimitProperties.getPolicies();
    }

    @Override
    public int getFilterOrder() {
        return rateLimitProperties.getFilterOrder();
    }

    @Override
    public void handle(HttpServletResponse httpServletResponse, Rate rate) throws IOException {
        tooManyRequestErrorHandler.handle(httpServletResponse, rate);
    }

    private RateLimitKeyGenerator getKeyGenerator(RateLimitProperties.Policy policy) {
        return keyGenerators.get(policy.getKeyGenerator());
    }

    @Override
    public List<RateLimitProperties.Policy> getMatchedPolicies(HttpServletRequest httpServletRequest) {
        return getMatchedPolicies(httpServletRequest.getRequestURI(),httpServletRequest.getMethod());
    }

//    @Override
//    public RateLimitProperties.Policy getPolicy(long block, int count, long duration, String key, String keyGenerator) {
//getPolicies().stream().filter(policy -> policy.)
//    }
}