package userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import userservice.common.interceptor.UserIdInterceptor;
import userservice.service.CartService;
import userservice.service.dto.CartItemRedis;
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
    public CartItemRedis add(@Valid @RequestBody CartRequest.Add request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        return cartService.addToCart(userId, request);
    }

    @PutMapping
    public CartItemRedis updateOption(@Valid @RequestBody CartRequest.Update request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        return cartService.updateOption(userId, request);
    }

    @DeleteMapping
    public void remove(@Valid @RequestBody CartRequest.Remove request) {
        Long userId = UserIdInterceptor.getCurrentUserId();
        cartService.remove(userId, request);
    }
}
