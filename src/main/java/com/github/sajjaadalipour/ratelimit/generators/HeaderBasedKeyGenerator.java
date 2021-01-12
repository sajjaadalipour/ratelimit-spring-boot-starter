package com.github.sajjaadalipour.ratelimit.generators;

import com.github.sajjaadalipour.ratelimit.RateLimitKeyGenerator;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.Policy;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.StringJoiner;

/**
 * An implementation of {@link RateLimitKeyGenerator} to generate a identity key from the requester
 * based on HTTP Request Header parameters.
 *
 * @author Sajjad Alipour
 * @author Mehran Behnam
 */
public class HeaderBasedKeyGenerator implements RateLimitKeyGenerator {

    /**
     * Represents the defined params in the property file,
     * that used as HTTP headers keys in {@link #generateKey(Policy, Object...)}.
     */
    private final Set<String> params;

    public HeaderBasedKeyGenerator(Set<String> params) {
        this.params = params;
    }

    /**
     * Gets the request headers by the {@link #params} and return a string value
     * from combination of given header parameters.
     * <p>
     * Makes a key by Http servlet request method and request URI.
     * <p>
     * Gets Http header parameters by given {@link #params} from the {@code servletRequest}.
     * <p>If any value of {@link #params} does not exists in Http request, throw exception.
     *
     * @param policy Encapsulates the rate limit policy properties.
     * @param params Encapsulates the http servlet request.
     * @return Generated code.
     * @throws HeaderNotPresentedException If not present any item of the given {@link #params} from the Http request header.
     */
    @Override
    public Object generateKey(Policy policy, Object... params) {
        return generateKey(policy, (HttpServletRequest) params[0]);
    }

    private String generateKey(Policy policy, HttpServletRequest servletRequest) {
        StringJoiner key = new StringJoiner("_")
                .add(servletRequest.getRequestURI())
                .add(servletRequest.getMethod())
                .add(policy.getDuration().toString())
                .add(String.valueOf(policy.getCount()));

        for (String param : params) {
            String header = servletRequest.getHeader(param);
            if (header == null)
                throw new HeaderNotPresentedException(param, "The header's param is not presented in the request header's parameters.");

            key.add(header);
        }

        return key.toString();
    }
}