package orderservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.service.dto.CartOrderRequest;
import orderservice.service.dto.OrderRequest;
import orderservice.service.dto.OrderResponse;
import orderservice.service.v1.OrderService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public OrderResponse order(@Valid @RequestBody OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        return orderService.order(request);
    }

    @PostMapping("/cancel")
    public OrderResponse cancel(@Valid @RequestBody OrderRequest.Cancel request) {
        log.info("Cancel order for user: {}", request.getUserId());
        return orderService.cancel(request.getOrderId(), request.getUserId());
    }

    @PostMapping("/cart")
    public OrderResponse cartOrder(@Valid @RequestBody CartOrderRequest request) {
        return orderService.cartOrder(request);
    }
}
