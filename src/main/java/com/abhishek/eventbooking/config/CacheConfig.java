package com.abhishek.eventbooking.config;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;

import org.springframework.cache.annotation.EnableCaching;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableCaching
public class CacheConfig {

    // =========================================================
    // DEFAULT REDIS CACHE CONFIGURATION
    // =========================================================

    @Bean
    public RedisCacheConfiguration redisCacheConfiguration() {

        return RedisCacheConfiguration
                .defaultCacheConfig()

                /*
                 * Redis keys will be stored as readable Strings.
                 */
                .serializeKeysWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(
                                        RedisSerializer.string()
                                )
                )

                /*
                 * Spring Data Redis 4 uses Jackson 3 here.
                 *
                 * This means our DTOs do NOT need to implement
                 * java.io.Serializable.
                 */
                .serializeValuesWith(
                        RedisSerializationContext
                                .SerializationPair
                                .fromSerializer(
                                        RedisSerializer.json()
                                )
                )

                /*
                 * Never store null results.
                 */
                .disableCachingNullValues()

                /*
                 * Make keys easy to identify in Redis.
                 *
                 * Example:
                 *
                 * eventbooking::event-details::5
                 */
                .computePrefixWith(
                        cacheName ->
                                "eventbooking::"
                                        + cacheName
                                        + "::"
                )

                /*
                 * Default TTL.
                 *
                 * Individual caches override this below.
                 */
                .entryTtl(
                        Duration.ofMinutes(5)
                );
    }


    // =========================================================
    // PER-CACHE TTL CONFIGURATION
    // =========================================================

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration defaultConfiguration) {

        return builder -> builder

                /*
                 * Cache changes performed within transactions
                 * should cooperate with Spring transaction
                 * completion.
                 */
                .transactionAware()

                // ---------------------------------------------
                // EVENT DETAILS
                //
                // Relatively stable data.
                // ---------------------------------------------

                .withCacheConfiguration(
                        CacheNames.EVENT_DETAILS,

                        defaultConfiguration
                                .entryTtl(
                                        Duration.ofMinutes(10)
                                )
                )


                // ---------------------------------------------
                // EVENT ANALYTICS
                //
                // Changes more often, so short TTL.
                // ---------------------------------------------

                .withCacheConfiguration(
                        CacheNames.EVENT_ANALYTICS,

                        defaultConfiguration
                                .entryTtl(
                                        Duration.ofSeconds(30)
                                )
                );
    }
}