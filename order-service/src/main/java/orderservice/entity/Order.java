package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import orderservice.common.BaseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    @Column(nullable = false)
    private Integer totalAmount;

    @Column(nullable = false)
    private Integer couponDiscount;

    @Column(nullable = false)
    private Integer pointDiscount;

    @Column(nullable = false)
    private Integer finalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Setter
    private String address;

    @Setter
    private String receiverName;

    @Setter
    private String receiverPhone;

    @Setter
    private String paymentMethod;
    private String paymentStatus;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static Order create(
            Long userId,
            Integer totalAmount,
            Integer couponDiscount,
            Integer pointDiscount,
            Integer finalAmount
    ) {
        return Order.builder()
                .userId(userId)
                .totalAmount(totalAmount)
                .couponDiscount(couponDiscount)
                .pointDiscount(pointDiscount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
    }

    public void addItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void applyDiscounts(int couponDiscount, int pointDiscount,int finalAmount) {
        this.couponDiscount = couponDiscount;
        this.pointDiscount = pointDiscount;
        this.finalAmount = finalAmount;
    }

    public void completed(){
        this.status = OrderStatus.COMPLETED;
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void fail() {
        this.status = OrderStatus.FAILED;
    }

}

