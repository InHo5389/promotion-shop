package userservice.service;

import userservice.service.domain.CartItem;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface CartRepository {

    Map<String, CartItem> getCart(Long userId);
    void saveCartItem(Long userId, Long productId, Long optionId, CartItem item);
    void removeCartItem(Long userId, Long productId, Long optionId);
    void setCartExpire(Long userId, long timeout, TimeUnit unit);
}
