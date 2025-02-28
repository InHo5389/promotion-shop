package userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import userservice.common.interceptor.UserIdInterceptor;
import userservice.service.CartService;
import userservice.service.domain.CartItem;
import userservice.service.dto.CartRequest;
import userservice.service.dto.CartResponse;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public CartResponse getCart() {
        Long userId = UserIdInterceptor.getCurrentUserId();
        return cartService.getCart(userId);
    }

    @PostMapping
    public CartItem add(@Valid @RequestBody CartRequest.Add request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        return cartService.addToCart(userId, request);
    }

    @PutMapping("/product/{originalProductId}/option/{originalOptionId}")
    public CartItem updateOption(
            @Valid @RequestBody CartRequest.Update request,
            @PathVariable Long originalProductId,
            @PathVariable Long originalOptionId) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        return cartService.updateOption(userId, originalProductId, originalOptionId, request);
    }

    @DeleteMapping
    public void remove(@Valid @RequestBody CartRequest.Remove request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        cartService.remove(userId, request);
    }
}
