package com.github.sajjaadalipour.ratelimit.conf.filter;

import com.github.sajjaadalipour.ratelimit.Context;
import com.github.sajjaadalipour.ratelimit.Rate;
import com.github.sajjaadalipour.ratelimit.RatePolicy;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;
import org.springframework.boot.web.servlet.filter.OrderedFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.annotation.Nonnull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * A servlet filter to filtering requests to handle rate limiting.
 *
 * @author Sajjad Alipour
 * @author Mehran Behnam
 */
public class RateLimitFilter extends OncePerRequestFilter implements OrderedFilter {

    private final Context context;

    public RateLimitFilter(Context context) {
        this.context = context;
    }

    /**
     * First for all, get matched policies from the {@code httpServletRequest} by http method and request uri,
     * iterates on matched policies then, first get the policy`s key generator to generate an identity key,
     * now inits a {@link RatePolicy} and pass to rate limiter to consume, if after consuming the rate result exceeded
     * return too many request error.
     *
     * @param httpServletRequest  The request to process.
     * @param httpServletResponse The response associated with the request.
     * @param filterChain         Provides access to the next filter in the chain for this
     *                            filter to pass the request and response to for further
     *                            processing.
     * @throws ServletException If the processing fails for any other reason.
     * @throws IOException      If an I/O error occurs during this filter's processing of the request.
     */
    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest httpServletRequest,
            @Nonnull HttpServletResponse httpServletResponse,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

        List<Policy> matchedPolicies = context.getMatchedPolicies(httpServletRequest);
        boolean doFilterChain = true;
        for (Policy policy : matchedPolicies) {
            final RatePolicy ratePolicy = context.getRatePolicy(httpServletRequest, policy);
            Rate rate = context.consume(ratePolicy);
            if (rate.isExceed()) {
                context.handle(httpServletResponse, rate);
                doFilterChain = false;
                break;
            }
        }
        if (doFilterChain) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }
    }

    @Override
    public int getOrder() {
        return context.getFilterOrder();
    }
}