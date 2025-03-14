package pointservice.repository.v2;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;
import pointservice.service.v2.PointRedisRepository;

@Repository
@RequiredArgsConstructor
public class PointRedisRepositoryImpl implements PointRedisRepository {

    private final RedissonClient redissonClient;

    private static final String POINT_BALANCE_MAP = "point:balance";

    @Override
    public Long getBalanceFormCache(Long userId) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        return balanceMap.get(String.valueOf(userId));
    }

    @Override
    public void updateBalanceCache(Long userId, Long balance) {
        RMap<String, Long> balanceMap = redissonClient.getMap(POINT_BALANCE_MAP);
        balanceMap.fastPut(String.valueOf(userId), balance);
    }
}
