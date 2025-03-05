package orderservice.service;

import orderservice.entity.Order;
import orderservice.entity.OrderStatus;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    Order findOrderWithItems(Long orderId);
    Order save(Order order);
    Optional<Order> findById(Long orderId);
}
