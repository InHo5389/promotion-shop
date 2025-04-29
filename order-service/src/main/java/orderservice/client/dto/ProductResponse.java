package orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private BigDecimal price;
    private String image;
    private String status;
    private Long categoryId;
    private String categoryName;
    private List<ProductOptionDTO> options;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOptionDTO {
        private Long id;
        private String size;
        private String color;
        private BigDecimal additionalPrice;
        private Integer stockQuantity;
    }
}
