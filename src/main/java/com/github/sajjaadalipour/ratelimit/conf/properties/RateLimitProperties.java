package com.github.sajjaadalipour.ratelimit.conf.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.sajjaadalipour.ratelimit.RateLimitKeyGenerator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.time.Duration;
import java.util.*;

import static org.springframework.util.StringUtils.trimAllWhitespace;

/**
 * Configuration properties to configure the rate limit mechanism.
 *
 * @author Sajjad Alipour
 */
@Validated
@ConstructorBinding
@ConfigurationProperties(RateLimitProperties.PREFIX)
public class RateLimitProperties {

    public static final String PREFIX = "rate-limit";

    /**
     * Represents the limitation mechanism to be enabled or not.
     */
    private final Boolean enabled;

    /**
     * Determines the {@link com.github.sajjaadalipour.ratelimit.conf.filter.RateLimitFilter} order.
     */
    private final int filterOrder;

    /**
     * Represents which repository name to use to store rate limitation detail?
     */
    private final RateLimitRepositoryKey repository;

    /**
     * The list of policy that should be applied.
     */
    @NestedConfigurationProperty
    @NotEmpty(message = "Rate limit key policies is empty")
    private final List<@Valid Policy> policies;

    /**
     * Represents a list of key generators that can be add in policies.
     */
    @NestedConfigurationProperty
    @NotEmpty(message = "Rate limit key generators is empty")
    private final Set<@Valid KeyGenerator> keyGenerators;

    /**
     * Keeps a map of {@link #keyGenerators} by key generators names.
     */
    @JsonIgnore
    private final Map<String, KeyGenerator> keyGeneratorMap = new HashMap<>();

    @AssertTrue(message = "Rate limit repository is null")
    public boolean isRepositoryNotNullWhenEnabled() {
        return enabled && !StringUtils.isEmpty(repository);
    }

    /**
     * Checks the policies items key generator name is valid.
     *
     * @return false if does not exist key generator name inf {@link #keyGenerators}.
     */
    @AssertTrue(message = "Rate limit policy`s key generator invalid")
    public boolean isValidPolicyKeyGenerator() {
        for (Policy policy : policies) {
            boolean exist = keyGenerators.stream().anyMatch(it -> it.name.equals(policy.keyGenerator));
            if (!exist) {
                return false;
            }
        }
        return true;
    }

    public RateLimitProperties(
            Boolean enabled,
            int filterOrder,
            RateLimitRepositoryKey repository,
            Set<Policy> policies,
            Set<KeyGenerator> keyGenerators) {
        this.enabled = enabled;
        this.repository = repository;
        this.policies = new ArrayList<>(policies);
        this.keyGenerators = keyGenerators;
        this.filterOrder = filterOrder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getFilterOrder() {
        return filterOrder;
    }

    public RateLimitRepositoryKey getRepository() {
        return repository;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public Set<KeyGenerator> getKeyGenerators() {
        return keyGenerators;
    }

    /**
     * Encapsulates the key generator properties.
     */
    public static final class KeyGenerator {

        /**
         * The filter name that should be unique.
         */
        @NotBlank(message = "Rate limit key generator name is blank")
        @Size(max = 20, message = "Rate limit key generator name max size is {max}")
        private final String name;

        /**
         * Determines the key generator implementation to handle key generating.
         */
        @NotNull(message = "Rate limit key generators, the generator is null")
        private final Class<RateLimitKeyGenerator> generator;

        /**
         * Passes to the {@link #generator} constructor, that use in key generation algorithm.
         * <b>Its optional.</p>
         */
        private final Set<String> params;

        public KeyGenerator(String name, Class<RateLimitKeyGenerator> generator, Set<String> params) {
            this.name = name;
            this.generator = generator;
            this.params = params == null ? new HashSet<>() : params;
        }

        public String getName() {
            return name;
        }

        public Class<RateLimitKeyGenerator> getGenerator() {
            return generator;
        }

        public Set<String> getParams() {
            return params;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyGenerator that = (KeyGenerator) o;
            return name.equals(that.name) && generator.isAssignableFrom(that.generator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, generator.getName());
        }
    }

    /**
     * Encapsulates the policy properties detail.
     */
    public static final class Policy {

        /**
         * Determines the limited duration.
         */
        @NotNull(message = "Rate limit policy`s duration is null")
        private final Duration duration;

        /**
         * The number of API calls, determines the limitation count for the presented duration.
         */
        @NotNull(message = "Rate limit policy`s count is null")
        private final Integer count;

        /**
         * Represents the key generator name.
         */
        @NotBlank(message = "Rate limit policy`s key generator name is blank")
        @Size(max = 20, message = "Rate limit policy`s key generator name max size is {max}")
        private final String keyGenerator;

        /**
         * Represents the list of routes that want to apply the limitation to those.
         */
        @NotEmpty(message = "Rate limit policy`s routes is empty")
        private final Set<@Valid Route> routes;

        /**
         * Represents the blocking conditions.
         */
        @Valid
        private final Block block;

        public Policy(Duration duration,
                      Integer count,
                      String keyGenerator,
                      Set<Route> routes,
                      Block block) {
            this.duration = duration;
            this.count = count;
            this.keyGenerator = trimAllWhitespace(keyGenerator);
            this.routes = routes;
            this.block = block;
        }

        public Duration getDuration() {
            return duration;
        }

        public Integer getCount() {
            return count;
        }

        public String getKeyGenerator() {
            return keyGenerator;
        }

        public Set<Route> getRoutes() {
            return routes;
        }

        public Block getBlock() {
            return block;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Policy policy = (Policy) o;
            return duration.equals(policy.duration) &&
                    count.equals(policy.count) &&
                    keyGenerator.equals(policy.keyGenerator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(duration, count, keyGenerator);
        }

        /**
         * Encapsulates the block condition details.
         */
        public static final class Block {

            /**
             * Determines the blocking duration.
             */
            @NotNull(message = "Rate limit policy`s block duration is null")
            private Duration duration;

            public void setDuration(Duration duration) {
                this.duration = duration;
            }

            public Duration getDuration() {
                return duration;
            }
        }

        /**
         * Encapsulates the routes details.
         */
        public static final class Route {

            /**
             * Represents a rate limit policy uri.
             */
            @NotBlank(message = "Rate limit request route uri is blank")
            private final String uri;

            /**
             * Determines the request method. It's optional and the null value means all HTTP methods.
             */
            private final HttpMethod method;

            public Route(String uri, HttpMethod method) {
                this.uri = uri;
                this.method = method;
            }

            public String getUri() {
                return uri;
            }

            public HttpMethod getMethod() {
                return method;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Route route = (Route) o;
                return uri.equals(route.uri) &&
                        method == route.method;
            }

            @Override
            public int hashCode() {
                return Objects.hash(uri, method);
            }
        }
    }
}
