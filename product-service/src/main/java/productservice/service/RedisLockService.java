package productservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisLockService {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean tryLock(String key, String value) {
        Boolean result = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, Duration.ofSeconds(30));
        return Boolean.TRUE.equals(result);
    }

    public void releaseLock(String key) {
        stringRedisTemplate.delete(key);
    }
}
