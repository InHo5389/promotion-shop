package timesaleservice.client.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class ProductResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String image;
    private ProductStatus status;
    private Long categoryId;
    private String categoryName;
    private List<ProductOptionDTO> options;
}
