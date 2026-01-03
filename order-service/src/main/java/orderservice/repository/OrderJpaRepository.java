package orderservice.repository;

import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
    @Query("SELECT o FROM Order o JOIN FETCH o.orderItems WHERE o.id = :orderId")
    Order findOrderWithItems(Long orderId);

    @Query("SELECT o FROM Order o WHERE o.status = 'PENDING' AND o.expiresAt < :now")
    List<Order> findExpiredOrders(@Param("now") LocalDateTime now);
}
