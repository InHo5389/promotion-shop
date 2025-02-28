package userservice.service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private Long productId;
    private Long optionId;
    private int quantity;
    private LocalDateTime addedAt;

    public void updateQuantity(int newQuantity) {
        this.quantity = newQuantity;
    }
}
