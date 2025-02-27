package userservice.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private Long userId;
    private String username;
    private List<CartProductResponse> items;
    private BigDecimal totalPrice;
}
