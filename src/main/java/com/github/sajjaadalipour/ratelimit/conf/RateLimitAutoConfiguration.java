package com.github.sajjaadalipour.ratelimit.conf;

import com.github.sajjaadalipour.ratelimit.*;
import com.github.sajjaadalipour.ratelimit.conf.error.DefaultTooManyRequestErrorHandler;
import com.github.sajjaadalipour.ratelimit.conf.error.TooManyRequestErrorHandler;
import com.github.sajjaadalipour.ratelimit.conf.filter.RateLimitFilter;
import com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties;
import com.github.sajjaadalipour.ratelimit.repositories.InMemoryRateCache;
import com.github.sajjaadalipour.ratelimit.repositories.redis.RedisRateCache;
import com.github.sajjaadalipour.ratelimit.repositories.redis.RedisRepository;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import java.util.Map;

import static com.github.sajjaadalipour.ratelimit.conf.properties.RateLimitProperties.PREFIX;

/**
 * Auto-configuration responsible for registering a {@link RateLimiter} and {@link RateLimitKeyGenerator}
 * filled with builtin, custom and default fallback {@link RateLimitProperties.KeyGenerator}s.
 * <p>
 * Defaults implementations of {@link RateLimiter} are base of memory and redis
 * that is configurable in the properties file.
 * <p>
 * Custom Rate Limiter
 * In order to provide your own custom {@link RateLimiter} implementation,
 * just implement {@link RateLimiter} interface and register it as Spring Bean.
 * <p>
 * Custom Rate Limit Key Generator
 * You can also provide your own custom {@link RateLimitKeyGenerator} implementations.
 * Just implement the {@link RateLimitKeyGenerator} interface and add to properties file.
 *
 * @author Sajjad Alipour
 * @author Mehran Behnam
 * @see RateLimitKeyGenerator
 * @see RateLimiter
 */
@Configuration
@ConditionalOnWebApplication
@EnableConfigurationProperties(RateLimitProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true")
public class RateLimitAutoConfiguration {

    public RateLimitAutoConfiguration(ApplicationContext context, RateLimitProperties rateLimitProperties) {
        registerKeyGeneratorsBeans(context, rateLimitProperties);
    }

    /**
     * Registers a bean of {@link RateLimiter} if set `IN_MEMORY` value on `repository` property.
     *
     * @return The expected {@link InMemoryRateCache}.
     */
    @Bean
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnProperty(prefix = PREFIX, name = "repository", havingValue = "IN_MEMORY")
    public RateLimiter imMemoryRateLimiter() {
        return new InMemoryRateCache();
    }

    /**
     * Encapsulates the redis based rate limiter auto-configuration to register bean of {@link RedisRateCache}
     * if set `REDIS` value on `repository` property and exist {@link StringRedisTemplate}.
     */
    @Configuration
    @ConditionalOnMissingBean(RateLimiter.class)
    @ConditionalOnClass(StringRedisTemplate.class)
    @EnableRedisRepositories(basePackageClasses = RedisRateCache.class)
    @ConditionalOnProperty(prefix = PREFIX, name = "repository", havingValue = "REDIS")
    public static class RedisConfiguration {

        /**
         * Registers a bean of {@link RateLimiter} to cache rate limit detail into Redis.
         *
         * @param redisRepository Provides redis repository to persist and retrieve from/to Redis.
         * @return Expected {@link RedisRateCache}.
         */
        @Bean
        public RateLimiter redisRateLimiter(RedisRepository redisRepository) {
            return new RedisRateCache(redisRepository);
        }
    }

    /**
     * Registers a bean of {@link TooManyRequestErrorHandler} to handle too many request error.
     *
     * @return Expected {@link DefaultTooManyRequestErrorHandler}.
     */
    @Bean
    @ConditionalOnMissingBean(TooManyRequestErrorHandler.class)
    public TooManyRequestErrorHandler tooManyRequestErrorHandler() {
        return new DefaultTooManyRequestErrorHandler();
    }


    @Bean
    @ConditionalOnMissingBean(RateLimitFilter.class)
    public RateLimitFilter rateLimitFilter(Context context) {
        return new RateLimitFilter(context);
    }

    /**
     * Registers a bean of {@link RateLimitFilter} servlet filter.
     *
     * @param rateLimitProperties        Encapsulates the rate limit properties.
     * @param rateLimiter                The registered implemented {@link RateLimiter} bean.
     * @param keyGenerators              A map of {@link RateLimitKeyGenerator}s beans.
     * @param tooManyRequestErrorHandler The registered implemented {@link TooManyRequestErrorHandler} bean.
     * @return Expected {@link RateLimitFilter}.
     */
    @Bean
    public Context context(RateLimitProperties rateLimitProperties,
                           RateLimiter rateLimiter,
                           Map<String, RateLimitKeyGenerator> keyGenerators,
                           TooManyRequestErrorHandler tooManyRequestErrorHandler) {
        return new RateLimitContext(tooManyRequestErrorHandler, rateLimitProperties, rateLimiter, keyGenerators);
    }

    @Bean
    public RateLimitAnnotationParser getAllPolices(Context context) {
        return new RateLimitAnnotationParser(context);
    }

    /**
     * Registers all key generators that define in the configuration properties file to Spring context as a bean.
     */
    private void registerKeyGeneratorsBeans(ApplicationContext context, RateLimitProperties rateLimitProperties) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getAutowireCapableBeanFactory();

        for (RateLimitProperties.KeyGenerator keyGenerator : rateLimitProperties.getKeyGenerators()) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder
                    .rootBeanDefinition(keyGenerator.getGenerator())
                    .addConstructorArgValue(keyGenerator.getParams());

            registry.registerBeanDefinition(keyGenerator.getName(), beanDefinitionBuilder.getBeanDefinition());
        }
    }
}