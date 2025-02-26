package productservice.service.dto;

import lombok.Builder;
import lombok.Getter;
import productservice.entity.Product;
import productservice.entity.ProductOption;
import productservice.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String image;
    private ProductStatus status;
    private Long categoryId;
    private String categoryName;
    private List<ProductOptionDTO> options;

    public static ProductResponse from(Product product){
            return ProductResponse.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .image(product.getImage())
                    .status(product.getStatus())
                    .categoryId(product.getCategory().getId())
                    .categoryName(product.getCategory().getName())
                    .options(product.getOptions().stream()
                            .map(ProductOptionDTO::from)
                            .collect(Collectors.toList()))
                    .build();
    }

    @Getter
    @Builder
    public static class ProductOptionDTO {
        private Long id;
        private String size;
        private String color;
        private BigDecimal additionalPrice;
        private Integer stockQuantity;

        public static ProductOptionDTO from(ProductOption option) {
            return ProductOptionDTO.builder()
                    .id(option.getId())
                    .size(option.getSize())
                    .color(option.getColor())
                    .additionalPrice(option.getAdditionalPrice())
                    .stockQuantity(option.getStock() != null ? option.getStock().getQuantity() : 0)
                    .build();
        }
    }
}
