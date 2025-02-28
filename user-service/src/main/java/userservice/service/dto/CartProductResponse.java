package userservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import userservice.client.dto.ProductResponse;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartProductResponse {
    private Long productId;
    private String productName;
    private String image;
    private String status;
    private Long optionId;
    private String optionSize;
    private String optionColor;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}