package com.duong.post.configuration;

import com.duong.post.dto.response.UserProfileResponse;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Bean("redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        // KHÔNG bật activateDefaultTyping
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory cf,
            @Qualifier("redisObjectMapper") ObjectMapper om
    ) {
        // Base: key = String, value = Generic (fallback)
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(om)))
                .entryTtl(Duration.ofMinutes(10));

        // Serializer riêng cho từng cache
        // 1) profile:by-id -> biết rõ type
        Jackson2JsonRedisSerializer<UserProfileResponse> profileSer =
                new Jackson2JsonRedisSerializer<>(com.duong.post.dto.response.UserProfileResponse.class);
        profileSer.setObjectMapper(om);

        // 2) following:by-me -> list id (array string)
        Jackson2JsonRedisSerializer<List> followingSer =
                new Jackson2JsonRedisSerializer<>(List.class);
        followingSer.setObjectMapper(om);

        Map<String, RedisCacheConfiguration> caches = new HashMap<>();
        caches.put("profile:by-id",
                base.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(profileSer))
                        .entryTtl(Duration.ofMinutes(30)));

        caches.put("following:by-me",
                base.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(followingSer))
                        .entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(caches)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory cf,
            @Qualifier("redisObjectMapper") ObjectMapper om
    ) {
        RedisTemplate<String, Object> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);
        t.setKeySerializer(new StringRedisSerializer());
        t.setValueSerializer(new GenericJackson2JsonRedisSerializer(om));
        t.afterPropertiesSet();
        return t;
    }
}
