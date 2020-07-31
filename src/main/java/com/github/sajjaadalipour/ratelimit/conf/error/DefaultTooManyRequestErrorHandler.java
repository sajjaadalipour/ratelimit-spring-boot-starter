package com.github.sajjaadalipour.ratelimit.conf.error;

import com.github.sajjaadalipour.ratelimit.Rate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Default implementation of {@link TooManyRequestErrorHandler} to handle too many request error response.
 *
 * @author Sajjad Alipour
 */
public class DefaultTooManyRequestErrorHandler implements TooManyRequestErrorHandler {

    private static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * Sets the response status code with 429 with {@code RETRY_AFTER_HEADER} header and the response body empty.
     *
     * @param httpServletResponse Encapsulates the http server response detail.
     * @param rate                Encapsulates the rate limit details.
     * @throws IOException When write to response.
     */
    @Override
    public void handle(HttpServletResponse httpServletResponse, Rate rate) throws IOException {
        httpServletResponse.setStatus(429);
        httpServletResponse.setHeader(RETRY_AFTER_HEADER, String.valueOf(rate.getExpiration().getEpochSecond()));
        httpServletResponse.getWriter().append("");
    }
}
