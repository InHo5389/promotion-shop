package orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.service.dto.request.OrderRequest;
import orderservice.service.dto.response.OrderResponse;
import orderservice.service.v1.OrderService;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("orderControllerV3")
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse.Create createCartOrder(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody OrderRequest.Create request
    ) {
        return orderService.createOrderFromCart(userId, request);
    }

    @PostMapping("/{orderId}/process")
    public void confirmOrder(@PathVariable Long orderId) {
        orderService.confirmOrder(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse.Cancel cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return orderService.cancelOrder(orderId, userId);
    }

    @GetMapping("/{orderId}")
    public OrderResponse.Detail getOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return orderService.getOrder(orderId, userId);
    }
}
