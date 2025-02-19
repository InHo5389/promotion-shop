package couponservice.repository.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.entity.Coupon;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CouponRedisRepository {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_STATE_KEY = "coupon:state:%s";

    /**
     * 쿠폰 상태를 Redis에 저장
     * @param coupon 상태를 저장할 쿠폰
     */
    public void updateCouponState(Coupon coupon) {
        String stateKey = generateStateKey(coupon.getId());
        try {
            String couponJson = objectMapper.writeValueAsString(coupon);
            RBucket<String> bucket = redissonClient.getBucket(stateKey);
            bucket.set(couponJson);
            log.info("Updated coupon state in Redis: {}", coupon.getId());
        } catch (JsonProcessingException e) {
            log.error("Error updating coupon state: {}", e.getMessage());
            throw new CustomGlobalException(ErrorType.COUPON_NOT_TRANSFER);
        }
    }

    /**
     * 쿠폰 상태를 Redis에서 가져옴
     * @param couponId 상태를 가져올 쿠폰 ID
     * @return 쿠폰 상태, 없으면 null
     */
    public Optional<Coupon> getCouponState(Long couponId) {
        String stateKey = generateStateKey(couponId);

        RBucket<String> bucket = redissonClient.getBucket(stateKey);
        String couponJson = bucket.get();

        if (couponJson == null) {
            return Optional.empty();
        }

        try {
            Coupon coupon = objectMapper.readValue(couponJson, Coupon.class);
            return Optional.of(coupon);
        } catch (JsonProcessingException e) {
            log.error("Error parsing coupon JSON: {}", e.getMessage());
            throw new CustomGlobalException(ErrorType.COUPON_NOT_TRANSFER);
        }
    }
    private String generateStateKey(Long couponId) {
        return String.format(COUPON_STATE_KEY, couponId);
    }
}
