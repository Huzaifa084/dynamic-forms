package com.apex.payroll.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisConfig {

    // ---------- ObjectMapper for cache ----------
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(new Jdk8Module())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);

        // Restrict polymorphic typing to safe types you actually use
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.apex.payroll.")          // your DTOs / wrappers
                .allowIfSubType("java.util.")                   // List/Set/Map impls (e.g., Unmodifiable*, ArrayList...)
                .allowIfSubType("java.lang.")                   // String, etc. (for scalar roots if ever cached)
                .allowIfBaseType(Collection.class)
                .allowIfBaseType(Map.class)
                .allowIfBaseType(TemporalAccessor.class)        // Java time types
                .build();

        // Apply type info when declared type is Object (which is true for Redis cache values)
        mapper.activateDefaultTyping(ptv,
                ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT,
                JsonTypeInfo.As.PROPERTY);

        return mapper;
    }

    // ---------- Serializer that forces @class at the ROOT ----------
    private GenericJackson2JsonRedisSerializer forcingRootTypeSerializer(ObjectMapper mapper) {
        return new GenericJackson2JsonRedisSerializer(mapper) {
            @Override
            @NonNull
            public byte[] serialize(Object source) throws SerializationException {
                if (source == null) {
                    return new byte[0];
                }
                try {
                    // Force the root to be treated as Object ⇒ Jackson includes type info at root
                    return mapper.writerFor(Object.class).writeValueAsBytes(source);
                } catch (JsonProcessingException e) {
                    throw new SerializationException("Could not write JSON: " + e.getMessage(), e);
                }
            }
        };
    }

    // ---------- CacheManager ----------
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper om = createRedisObjectMapper();
        GenericJackson2JsonRedisSerializer generic = forcingRootTypeSerializer(om);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(generic))
                .computePrefixWith(name -> "whd::" + name + "::")
                .entryTtl(Duration.ofMinutes(30))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> caches = new HashMap<>();

        // If you have caches that store a SINGLE String value (pure scalar),
        // you can uncomment the lines below to serialize them as plain strings:
        // SerializationPair<?> stringVal = SerializationPair.fromSerializer(new StringRedisSerializer());
        // caches.put("fcm.user.tokens.latest", defaultConfig.serializeValuesWith(stringVal).entryTtl(Duration.ofMinutes(40)));
        // caches.put("fcm.lm.tokens.latest",   defaultConfig.serializeValuesWith(stringVal).entryTtl(Duration.ofMinutes(40)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(caches)
                .transactionAware()
                .build();
    }

    // ---------- RedisTemplate ----------
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory cf) {
        ObjectMapper om = createRedisObjectMapper();
        GenericJackson2JsonRedisSerializer generic = forcingRootTypeSerializer(om);

        RedisTemplate<String, Object> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setHashKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(generic);
        tpl.setHashValueSerializer(generic);
        tpl.afterPropertiesSet();
        return tpl;
    }
}