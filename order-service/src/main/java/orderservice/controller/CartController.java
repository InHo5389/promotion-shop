package orderservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.service.dto.response.CartCouponApplyResponse;
import orderservice.service.dto.response.CartResponse;
import orderservice.service.v1.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<Void> addToCart(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long productId,
            @RequestParam Long productOptionId,
            @RequestParam Integer quantity
    ) {

        cartService.addToCart(userId, productId, productOptionId, quantity);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        CartResponse response = cartService.getCartWithDiscounts(userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFromCart(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long productId,
            @RequestParam Long productOptionId
    ) {
        cartService.removeFromCart(userId, productId, productOptionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        cartService.clearCart(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/coupon")
    public ResponseEntity<CartCouponApplyResponse> applyCoupon(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long productId,
            @RequestParam Long productOptionId,
            @RequestParam Long couponId
    ) {
        CartCouponApplyResponse response = cartService.applyCouponToCartItem(
                userId, productId, productOptionId, couponId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/coupon")
    public ResponseEntity<Void> removeCoupon(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestParam Long productId,
            @RequestParam Long productOptionId
    ) {
        cartService.removeCouponFromCartItem(userId, productId, productOptionId);
        return ResponseEntity.ok().build();
    }
}
