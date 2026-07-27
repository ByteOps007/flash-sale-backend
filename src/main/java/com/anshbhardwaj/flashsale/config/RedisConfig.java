package com.anshbhardwaj.flashsale.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {

    // StringRedisTemplate is enough for our use case: stock counts are
    // just integers stored as strings under keys like "stock:<productId>".
    // Redis's DECR command is atomic even under heavy concurrent access,
    // which is exactly what we need for the flash-sale fast-path check.
    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
