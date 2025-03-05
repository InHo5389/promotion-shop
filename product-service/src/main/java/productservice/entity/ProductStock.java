package productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import productservice.common.BaseEntity;

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

    public static ProductStock create(ProductOption productOption, Integer quantity) {
        ProductStock productStock = new ProductStock();
        productStock.productOption = productOption;
        productStock.quantity = quantity;
        return productStock;
    }

    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
