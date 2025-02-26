package productservice.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class ProductOptionRequest {

    private String size;
    private String color;
    private BigDecimal additionalPrice;

    @NotNull(message = "재고 수량은 필수입니다")
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다")
    private Integer stockQuantity;

    @Getter
    public static class Update{
        private Long id;
        private String size;
        private String color;
        private BigDecimal additionalPrice;

        @NotNull(message = "재고 수량은 필수입니다")
        @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다")
        private Integer stockQuantity;
    }

    @Getter
    public static class Read{
        private Long id;
        private String size;
        private String color;
        private BigDecimal additionalPrice;

        @NotNull(message = "재고 수량은 필수입니다")
        @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다")
        private Integer stockQuantity;
    }
}
