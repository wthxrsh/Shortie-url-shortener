package url_shortener.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Save URL with a specific TTL
    public void saveUrl(
            String shortCode,
            String originalUrl,
            Duration ttl) {

        redisTemplate.opsForValue()
                .set(shortCode, originalUrl, ttl);
    }

    // Save URL with default 1-hour TTL
    public void saveUrl(
            String shortCode,
            String originalUrl) {

        saveUrl(
                shortCode,
                originalUrl,
                Duration.ofHours(1)
        );
    }

    public String getUrl(String shortCode) {
        return redisTemplate.opsForValue()
                .get(shortCode);
    }

    public void deleteUrl(String shortCode) {
        redisTemplate.delete(shortCode);
    }
}