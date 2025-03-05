package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import orderservice.common.BaseEntity;
import orderservice.service.dto.OrderRequest;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "orders")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Setter
    private BigDecimal totalAmount;

    @Setter
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private String address;
    private String receiverName;
    private String receiverPhone;

    private String paymentMethod;
    private String paymentStatus;

    public static Order create(OrderRequest request) {
        return Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PENDING)
                .address(request.getAddress())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus("WAITING")
                .build();
    }

    public void addItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
}
