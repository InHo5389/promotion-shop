package timesaleservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOptionDTO {
    private Long id;
    private String size;
    private String color;
    private BigDecimal additionalPrice;
    private Integer stockQuantity;
}