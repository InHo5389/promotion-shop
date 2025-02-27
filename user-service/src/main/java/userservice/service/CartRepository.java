package userservice.service;

import userservice.service.dto.CartItemRedis;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface CartRepository {
    // 장바구니 조회
    Map<String, CartItemRedis> getCartItems(Long userId);

    // 장바구니 아이템 추가/수정
    void saveCartItem(Long userId, Long productId, Long optionId, CartItemRedis item);

    // 장바구니 아이템 삭제
    void removeCartItem(Long userId, Long productId, Long optionId);

    // 장바구니 비우기
    void clearCart(Long userId);

    // 장바구니 만료 설정
    void setCartExpire(Long userId, long timeout, TimeUnit unit);
}
