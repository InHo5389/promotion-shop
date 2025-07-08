package timesaleservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import timesaleservice.common.BaseEntity;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@Table(name = "time_sales")
@NoArgsConstructor
@AllArgsConstructor
public class TimeSale extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long remainingQuantity;

    @Column(nullable = false)
    private Long discountPrice;

    @Column(nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeSaleStatus status = TimeSaleStatus.ACTIVE;

    @Version
    @Builder.Default
    private Long version = 0L;
}
