package productservice.entity;

import jakarta.persistence.*;
import lombok.*;
import productservice.common.BaseEntity;

import java.math.BigDecimal;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Setter
    @OneToOne(mappedBy = "productOption", cascade = CascadeType.ALL)
    private ProductStock stock;

    private String size;
    private String color;
    private BigDecimal additionalPrice;

    public static ProductOption create(Product product, String size, String color, BigDecimal additionalPrice) {
        ProductOption productOption = new ProductOption();
        productOption.product = product;
        productOption.stock = null;
        productOption.size = size;
        productOption.color = color;
        productOption.additionalPrice = additionalPrice;
        return productOption;
    }

    public void update(String size, String color, BigDecimal additionalPrice) {
        if (size != null) this.size = size;
        if (color != null) this.color = color;
        if (additionalPrice != null) this.additionalPrice = additionalPrice;
    }
}
