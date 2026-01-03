package orderservice.service.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.DiscountType;
import orderservice.client.dto.ProductOptionDto;
import orderservice.client.dto.ProductResponse;
import orderservice.client.serviceclient.CouponServiceClient;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.CartItemRedis;
import orderservice.client.dto.CouponValidationResponse;
import orderservice.service.dto.response.CartCouponApplyResponse;
import orderservice.service.dto.response.CartItemResponse;
import orderservice.service.dto.response.CartResponse;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductServiceClient productClient;
    private final CouponServiceClient couponClient;
    private final ObjectMapper objectMapper;

    private static final String CART_KEY_PREFIX = "cart::";
    private static final long CART_EXPIRATION_DAYS = 7;

    /**
     * 장바구니에 상품 추가
     */
    public void addToCart(Long userId, Long productId, Long productOptionId, Integer quantity) {
        log.info("장바구니 추가 - userId: {}, productId: {}, optionId: {}, quantity: {}",
                userId, productId, productOptionId, quantity);

        ProductOptionDto option = productClient.getProductOption(productOptionId);

        if (option.getStock() < quantity) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
        }

        String cartKey = getCartKey(userId);
        String fieldKey = getFieldKey(productId, productOptionId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        Object existingItem = hashOps.get(cartKey, fieldKey);

        CartItemRedis cartItem;
        if (existingItem != null) {
            cartItem = objectMapper.convertValue(existingItem, CartItemRedis.class);
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
        } else {
            cartItem = CartItemRedis.create(productId, productOptionId, quantity);
        }

        hashOps.put(cartKey, fieldKey, cartItem);
        redisTemplate.expire(cartKey, CART_EXPIRATION_DAYS, TimeUnit.DAYS);

        log.info("장바구니 추가 완료 - userId: {}, fieldKey: {}", userId, fieldKey);
    }

    public CartCouponApplyResponse applyCouponToCartItem(
            Long userId,
            Long productId,
            Long productOptionId,
            Long couponId
    ) {
        log.info("장바구니 쿠폰 적용 - userId: {}, productId: {}, couponId: {}",
                userId, productId, couponId);

        String cartKey = getCartKey(userId);
        String fieldKey = getFieldKey(productId, productOptionId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        Object existingItem = hashOps.get(cartKey, fieldKey);
        if (existingItem == null) {
            throw new CustomGlobalException(ErrorType.CART_ITEM_NOT_FOUND);
        }

        CartItemRedis cartItem = objectMapper.convertValue(existingItem, CartItemRedis.class);

        CouponValidationResponse validation = couponClient.validateCoupon(couponId, userId);

        if (!validation.isValid()) {
            throw new CustomGlobalException(ErrorType.COUPON_NOT_AVAILABLE);
        }

        ProductResponse product = productClient.read(productId);
        ProductOptionDto option = productClient.getProductOption(productOptionId);

        int itemPrice = product.getPrice().intValue() + option.getAdditionalPrice().intValue();
        log.info("******** product.getPrice().intValue() : {}  + -**** option.getAdditionalPrice().intValue(): {}", product.getPrice().intValue(), option.getAdditionalPrice().intValue());
        int totalPrice = itemPrice * cartItem.getQuantity();

        int discountAmount = calculateDiscount(validation.getCouponPolicy(), totalPrice);
        log.info("*******************discountAmount : {}", discountAmount);

        cartItem.applyCoupon(couponId, discountAmount);
        hashOps.put(cartKey, fieldKey, cartItem);
        redisTemplate.expire(cartKey, CART_EXPIRATION_DAYS, TimeUnit.DAYS);

        log.info("장바구니 쿠폰 적용 완료 - userId: {}, couponId: {}, discount: {}",
                userId, couponId, discountAmount);

        return CartCouponApplyResponse.builder()
                .couponId(couponId)
                .discountAmount(discountAmount)
                .originalPrice(totalPrice)
                .finalPrice(totalPrice - discountAmount)
                .build();
    }

    public void removeCouponFromCartItem(Long userId, Long productId, Long productOptionId) {
        log.info("장바구니 쿠폰 제거 - userId: {}, productId: {}, optionId: {}",
                userId, productId, productOptionId);

        String cartKey = getCartKey(userId);
        String fieldKey = getFieldKey(productId, productOptionId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        Object existingItem = hashOps.get(cartKey, fieldKey);
        if (existingItem == null) {
            throw new CustomGlobalException(ErrorType.CART_ITEM_NOT_FOUND);
        }

        CartItemRedis cartItem = objectMapper.convertValue(existingItem, CartItemRedis.class);
        cartItem.removeCoupon();

        hashOps.put(cartKey, fieldKey, cartItem);

        log.info("장바구니 쿠폰 제거 완료 - userId: {}, fieldKey: {}", userId, fieldKey);
    }

    public CartResponse getCartWithDiscounts(Long userId) {
        log.info("장바구니 조회 (할인 포함) - userId: {}", userId);

        String cartKey = getCartKey(userId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
        Map<String, Object> cartItemsMap = hashOps.entries(cartKey);

        List<CartItemResponse> items = new ArrayList<>();
        int totalAmount = 0;
        int totalDiscount = 0;

        for (Object value : cartItemsMap.values()) {
            CartItemRedis cartItem = objectMapper.convertValue(value, CartItemRedis.class);

            ProductResponse product = productClient.read(cartItem.getProductId());
            ProductOptionDto option = productClient.getProductOption(cartItem.getProductOptionId());

            int itemPrice = product.getPrice().intValue() + option.getAdditionalPrice().intValue();
            int itemTotalPrice = itemPrice * cartItem.getQuantity();

            int itemDiscount = 0;

            if (cartItem.getAppliedCouponId() != null) {
                try {
                    CouponValidationResponse validation = couponClient.validateCoupon(
                            cartItem.getAppliedCouponId(), userId);

                    if (validation.isValid()) {
                        itemDiscount = calculateDiscount(validation.getCouponPolicy(), itemTotalPrice);
                    } else {
                        log.warn("유효하지 않은 쿠폰 자동 제거 - userId: {}, couponId: {}",
                                userId, cartItem.getAppliedCouponId());
                        cartItem.removeCoupon();
                        hashOps.put(cartKey, cartItem.getFieldKey(), cartItem);
                    }
                } catch (Exception e) {
                    log.error("쿠폰 검증 실패 - couponId: {}", cartItem.getAppliedCouponId(), e);
                    cartItem.removeCoupon();
                    hashOps.put(cartKey, cartItem.getFieldKey(), cartItem);
                }
            }

            totalAmount += itemTotalPrice;
            totalDiscount += itemDiscount;

            CartItemResponse itemResponse = CartItemResponse.builder()
                    .productId(cartItem.getProductId())
                    .productName(product.getName())
                    .productOptionId(cartItem.getProductOptionId())
                    .quantity(cartItem.getQuantity())
                    .size(option.getSize())
                    .color(option.getColor())
                    .price(itemPrice)
                    .totalPrice(itemTotalPrice)
                    .appliedCouponId(cartItem.getAppliedCouponId())
                    .discountAmount(itemDiscount)
                    .finalPrice(itemTotalPrice - itemDiscount)
                    .build();

            items.add(itemResponse);
        }

        return CartResponse.builder()
                .items(items)
                .totalAmount(totalAmount)
                .totalDiscount(totalDiscount)
                .finalAmount(totalAmount - totalDiscount)
                .build();
    }

    public List<CartItemRedis> getCartItems(Long userId) {
        log.info("장바구니 조회 - userId: {}", userId);

        String cartKey = getCartKey(userId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
        Map<String, Object> cartItemsMap = hashOps.entries(cartKey);

        List<CartItemRedis> cartItems = new ArrayList<>();
        for (Object value : cartItemsMap.values()) {
            CartItemRedis item = objectMapper.convertValue(value, CartItemRedis.class);
            cartItems.add(item);
        }

        log.info("장바구니 조회 완료 - userId: {}, itemCount: {}", userId, cartItems.size());
        return cartItems;
    }

    public void removeFromCart(Long userId, Long productId, Long productOptionId) {
        log.info("장바구니 삭제 - userId: {}, productId: {}, optionId: {}",
                userId, productId, productOptionId);

        String cartKey = getCartKey(userId);
        String fieldKey = getFieldKey(productId, productOptionId);
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();

        hashOps.delete(cartKey, fieldKey);

        log.info("장바구니 삭제 완료 - userId: {}, fieldKey: {}", userId, fieldKey);
    }

    public void clearCart(Long userId) {
        log.info("장바구니 전체 삭제 - userId: {}", userId);

        String cartKey = getCartKey(userId);
        redisTemplate.delete(cartKey);

        log.info("장바구니 전체 삭제 완료 - userId: {}", userId);
    }

    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private String getFieldKey(Long productId, Long productOptionId) {
        return productId + "::" + productOptionId;
    }

    private int calculateDiscount(CouponValidationResponse.CouponPolicyDto policy, int totalPrice) {
        if (policy == null) {
            return 0;
        }

        int discount = 0;

        if (totalPrice < policy.getMinimumOrderAmount()) {
            return 0;
        }
        DiscountType discountType = DiscountType.valueOf(policy.getDiscountType());

        if (discountType == DiscountType.FIXED_DISCOUNT) {
            discount = policy.getDiscountValue();
        } else if (discountType == DiscountType.RATE_DISCOUNT) {
            discount = (int) (totalPrice * policy.getDiscountValue() / 100.0);
        }

        if (discount > policy.getMaximumDiscountAmount()) {
            discount = policy.getMaximumDiscountAmount();
        }

        return discount;
    }
}
