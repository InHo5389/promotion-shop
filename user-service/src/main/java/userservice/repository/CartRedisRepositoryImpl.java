package userservice.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.service.CartRepository;
import userservice.service.domain.CartItem;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class CartRedisRepositoryImpl implements CartRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_FORMAT = "cart::%d";


    @Override
    public Map<String, CartItem> getCart(Long userId) {
        String key = generateKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, CartItem> result = new HashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            try {
                CartItem cartItem = objectMapper.readValue(entry.getValue().toString(), CartItem.class);
                result.put(entry.getKey().toString(), cartItem);
            } catch (JsonProcessingException e) {
                throw new CustomGlobalException(ErrorType.JSON_PARSING_FAILED);
            }
        }

        return result;
    }

    @Override
    public void saveCartItem(Long userId, Long productId, Long optionId, CartItem item) {
        String key = generateKey(userId);
        String itemKey = generateItemKey(productId, optionId);

        try {
            String itemJson = objectMapper.writeValueAsString(item);
            redisTemplate.opsForHash().put(key, itemKey, itemJson);
        } catch (JsonProcessingException e) {
            throw new CustomGlobalException(ErrorType.JSON_PARSING_FAILED);
        }
    }

    @Override
    public void removeCartItem(Long userId, Long productId, Long optionId) {
        String key = generateKey(userId);
        String itemKey = generateItemKey(productId, optionId);
        redisTemplate.opsForHash().delete(key, itemKey);
    }

    @Override
    public void setCartExpire(Long userId, long timeout, TimeUnit unit) {
        String key = generateKey(userId);
        redisTemplate.expire(key, timeout, unit);
    }

    private String generateKey(Long userId) {
        return KEY_FORMAT.formatted(userId);
    }

    private String generateItemKey(Long productId, Long optionId) {
        return productId + "::" + optionId;
    }
}