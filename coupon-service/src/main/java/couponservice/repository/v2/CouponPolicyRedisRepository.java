package couponservice.repository.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.entity.CouponPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CouponPolicyRedisRepository {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    private static final String COUPON_QUANTITY_KEY = "coupon:quantity:%s";
    private static final String COUPON_POLICY_KEY = "coupon:policy:%s";

    public void saveCouponPolicy(CouponPolicy couponPolicy) {
        String policyKey = generatePolicyKey(couponPolicy.getId());
        try {
            String policyJson = objectMapper.writeValueAsString(couponPolicy);
            RBucket<String> bucket = redissonClient.getBucket(policyKey);
            bucket.set(policyJson);
            log.info("Saved coupon policy to Redis: {}", couponPolicy.getId());
        } catch (JsonProcessingException e) {
            log.error("Error saving coupon policy: {}", e.getMessage());
            throw new CustomGlobalException(ErrorType.COUPON_NOT_TRANSFER);
        }
    }

    public void initializeQuantity(Long policyId, Integer totalQuantity) {
        String quantityKey = generateQuantityKey(policyId);
        RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
        atomicQuantity.set(totalQuantity);
        log.info("Initialized coupon quantity for policy {}: {}", policyId, totalQuantity);
    }

    public Optional<CouponPolicy> getCouponPolicy(Long policyId) {
        String policyKey = generatePolicyKey(policyId);

        RBucket<String> bucket = redissonClient.getBucket(policyKey);
        String policyJson = bucket.get();

        if (policyJson == null) {
            return Optional.empty();
        }

        try {
            CouponPolicy policy = objectMapper.readValue(policyJson, CouponPolicy.class);
            return Optional.of(policy);
        } catch (JsonProcessingException e) {
            log.error("Error parsing coupon policy JSON: {}", e.getMessage());
            throw new CustomGlobalException(ErrorType.COUPON_NOT_TRANSFER);
        }
    }

    public boolean decrementQuantity(Long policyId) {
        String quantityKey = generateQuantityKey(policyId);
        RAtomicLong atomicQuantity = redissonClient.getAtomicLong(quantityKey);
        long remainingQuantity = atomicQuantity.decrementAndGet();
        log.info("Decremented quantity for policy {}, remaining: {}", policyId, remainingQuantity);
        return remainingQuantity >= 0;
    }

    private String generatePolicyKey(Long policyId) {
        return String.format(COUPON_POLICY_KEY, policyId);
    }

    private String generateQuantityKey(Long policyId) {
        return String.format(COUPON_QUANTITY_KEY, policyId);
    }
}
