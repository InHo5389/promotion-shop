package orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class ProductOptionRequest {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdate {
        private Long productId;
        private Long optionId;
        private Integer quantity;

        public static StockUpdate create(Long productId, Long optionId, Integer quantity) {
            return ProductOptionRequest.StockUpdate.builder()
                    .productId(productId)
                    .optionId(optionId)
                    .quantity(quantity)
                    .build();
        }
    }
}
