package orderservice.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

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

}