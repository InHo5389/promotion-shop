package orderservice.client.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class CartRequest {

    @Getter
    public static class Remove{
        @NotNull
        private Long productId;

        @NotNull
        private Long productOptionId;
    }
}
