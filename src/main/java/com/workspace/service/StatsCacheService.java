// StatsCacheService.java
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StatsCacheService {

    private final StringRedisTemplate redisTemplate;

    public void incrementDailyStudyTime(String userEmail, String date, long minutes) {
        String key = "stats:daily:" + userEmail + ":" + date;
        // Redis 카운터 증가 로직
        redisTemplate.opsForValue().increment(key, minutes);
        // TTL 설정 (예: 7일 유지)
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }
}