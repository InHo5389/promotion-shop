package orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Entity
@Table(name = "order_items")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long productOptionId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer totalPrice;

    private Long couponId;
    private Integer couponDiscount;

    public static OrderItem create(
            Order order,
            Long productId,
            Long productOptionId,
            Integer quantity,
            Integer price,
            Long couponId,
            Integer couponDiscount
    ) {
        return OrderItem.builder()
                .order(order)
                .productId(productId)
                .productOptionId(productOptionId)
                .quantity(quantity)
                .price(price)
                .totalPrice(price * quantity)
                .couponId(couponId)
                .couponDiscount(couponDiscount != null ? couponDiscount : 0)
                .build();
    }
}
