package productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import productservice.common.BaseEntity;
import productservice.common.exception.CustomGlobalException;
import productservice.common.exception.ErrorType;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStock extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @OneToOne
    @JoinColumn(name = "product_option_id")
    private ProductOption productOption;

    private Integer quantity;

    @Column(nullable = false)
    private Integer reservedQuantity;

    @Version
    private Long version = 0L;

    public static ProductStock create(ProductOption productOption, Integer quantity) {
        ProductStock productStock = new ProductStock();
        productStock.productOption = productOption;
        productStock.quantity = quantity;
        productStock.reservedQuantity = 0;
        return productStock;
    }

    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void reserve(Integer amount) {
        Integer availableQuantity = this.quantity - this.reservedQuantity;
        if (availableQuantity < amount) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
        }
        this.reservedQuantity += amount;
    }

    public void confirmReservation(Integer amount) {
        if (this.reservedQuantity < amount) {
            throw new CustomGlobalException(ErrorType.INVALID_STOCK_RESERVATION);
        }
        this.quantity -= amount;
        this.reservedQuantity -= amount;
    }

    public void cancelReservation(Integer amount) {
        if (this.reservedQuantity < amount) {
            throw new CustomGlobalException(ErrorType.INVALID_STOCK_RESERVATION);
        }
        this.reservedQuantity -= amount;
    }

    public void rollbackConfirmation(Integer amount) {
        this.quantity += amount;
    }

    public void rollbackReservation(Integer amount) {
        this.reservedQuantity += amount;
    }
}
