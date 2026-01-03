package productservice.service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import productservice.entity.ProductOption;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class ProductOptionDto {

    private Long id;
    private String optionName;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer additionalPrice;
    private Integer stock;

    @Builder
    public ProductOptionDto(Long id, String optionName, String size, String color, BigDecimal price, Integer additionalPrice, Integer stock) {
        this.id = id;
        this.optionName = optionName;
        this.size = size;
        this.color = color;
        this.price = price;
        this.additionalPrice = additionalPrice;
        this.stock = stock;
    }

    public static ProductOptionDto from(ProductOption productOption) {
        return ProductOptionDto.builder()
                .id(productOption.getId())
                .optionName(productOption.getProduct().getName())
                .size(productOption.getSize())
                .color(productOption.getColor())
                .price(productOption.getProduct().getPrice())
                .additionalPrice(productOption.getAdditionalPrice().intValue())
                .stock(productOption.getStock().getQuantity())
                .build();
    }
}
