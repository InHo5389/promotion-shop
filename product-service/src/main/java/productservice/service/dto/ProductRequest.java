package productservice.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import productservice.entity.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequest {

    @Getter
    public static class Create{
        @NotBlank(message = "상품명은 필수입니다")
        private String name;

        @NotNull(message = "카테고리 ID는 필수입니다")
        private Long categoryId;

        @NotNull(message = "가격은 필수입니다")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다")
        private BigDecimal price;

        private String image;

        @Valid
        @NotEmpty(message = "최소 하나 이상의 옵션이 필요합니다")
        private List<ProductOptionRequest> options;
    }

    @Getter
    public static class Read{
        private Long productId;
        private String name;
        private Long categoryId;
        private Long categoryName;
        private BigDecimal price;
        private String image;
        private ProductStatus status;
        private List<ProductOptionRequest.Read> options;
    }

    @Getter
    public static class Update {
        private String name;
        private Long categoryId;
        private BigDecimal price;
        private String image;
        private ProductStatus status;
        private List<ProductOptionRequest.Update> options;
    }

    @Getter
    public static class ReadProductIds{
        private List<Long> productIds;
    }
}
