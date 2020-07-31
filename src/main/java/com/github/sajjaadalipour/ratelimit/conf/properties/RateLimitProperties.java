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

    public final static String PREFIX = "rate-limit";

    /**
     * Represents the limitation mechanism to be enabled or not.
     */
    private final Boolean enabled;

    /**
     * Represents which repository name to use to store rate limitation detail?
     */
    private final RateLimitRepository repository;

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
            RateLimitRepository repository,
            Set<Policy> policies,
            Set<KeyGenerator> keyGenerators) {
        this.enabled = enabled;
        this.repository = repository;
        this.policies = new ArrayList<>(policies);
        this.keyGenerators = keyGenerators;

        putKeyGeneratorsToMap(keyGenerators);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public RateLimitRepository getRepository() {
        return repository;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public Set<KeyGenerator> getKeyGenerators() {
        return keyGenerators;
    }

    /**
     * Returns a {@link KeyGenerator}.
     *
     * @param name Represents the key generator`s name.
     * @return Optional of key generator.
     */
    public Optional<KeyGenerator> getKeyGenerator(String name) {
        return Optional.ofNullable(this.keyGeneratorMap.get(name));
    }

    private void putKeyGeneratorsToMap(Set<KeyGenerator> keyGenerators) {
        for (KeyGenerator keyGenerator : keyGenerators) {
            this.keyGeneratorMap.put(keyGenerator.getName(), keyGenerator);
        }
    }

    /**
     * Encapsulates the key generator properties.
     */
    public final static class KeyGenerator {

        /**
         * The filter name that should be unique.
         */
        @NotBlank(message = "Rate limit key generator name is blank")
        @Size(max = 20, message = "Rate limit key generator name max size is {max}")
        private String name;

        /**
         * Passes to the {@link #generator} constructor, that use in key generation algorithm.
         * <b>Its optional.</p>
         */
        private Set<String> params;

        /**
         * Determines the key generator implementation to handle key generating.
         */
        @NotNull(message = "Rate limit key generators, the generator is null")
        private Class<RateLimitKeyGenerator> generator;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = trimAllWhitespace(name);
        }

        public Set<String> getParams() {
            return params;
        }

        public void setParams(Set<String> params) {
            this.params = params;
        }

        public Class<RateLimitKeyGenerator> getGenerator() {
            return generator;
        }

        public void setGenerator(Class<RateLimitKeyGenerator> generator) {
            this.generator = generator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            KeyGenerator that = (KeyGenerator) o;
            return name.equals(that.name) &&
                    generator.getName().equals(that.generator.getName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, generator.getName());
        }
    }

    /**
     * Encapsulates the policy properties detail.
     */
    public final static class Policy {

        /**
         * Determines the limited duration.
         */
        @NotNull(message = "Rate limit policy`s duration is null")
        private Duration duration;

        /**
         * The number of API calls, determines the limitation count for the presented duration.
         */
        @NotNull(message = "Rate limit policy`s count is null")
        private Integer count;

        /**
         * Represents the key generator name.
         */
        @NotBlank(message = "Rate limit policy`s key generator name is blank")
        @Size(max = 20, message = "Rate limit policy`s key generator name max size is {max}")
        private String keyGenerator;

        /**
         * Represents the list of routes that want to apply the limitation to those.
         */
        @NotEmpty(message = "Rate limit policy`s routes is empty")
        private Set<@Valid Route> routes;

        /**
         * Represents the blocking conditions.
         */
        @Valid
        private Block block;

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
        public final static class Block {

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
        public final static class Route {

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
