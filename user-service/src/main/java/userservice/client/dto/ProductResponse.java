package userservice.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String image;
    private String status;
    private Long categoryId;
    private String categoryName;
    private List<ProductOptionDto> options;

    @Getter
    @Builder
    public static class ProductOptionDto {
        private Long id;
        private String size;
        private String color;
        private BigDecimal additionalPrice;
        private Integer stockQuantity;
    }
}
