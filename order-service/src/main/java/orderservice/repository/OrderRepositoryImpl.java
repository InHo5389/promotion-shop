package orderservice.repository;

import lombok.RequiredArgsConstructor;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.service.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public List<Order> findByUserId(Long userId) {
        return orderJpaRepository.findByUserId(userId);
    }

    @Override
    public List<Order> findByUserIdAndStatus(Long userId, OrderStatus status) {
        return orderJpaRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public Order findOrderWithItems(Long orderId) {
        return orderJpaRepository.findOrderWithItems(orderId);
    }

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return orderJpaRepository.findById(orderId);
    }
}
