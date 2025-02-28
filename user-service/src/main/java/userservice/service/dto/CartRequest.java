package userservice.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class CartRequest {

    @Getter
    public static class Add{
        @NotNull
        private Long productId;

        @NotNull
        private Long productOptionId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Getter
    public static class Update{
        @NotNull
        private Long productId;

        @NotNull
        private Long productOptionId;

        @NotNull
        @Min(1)
        private Integer quantity;
    }

    @Getter
    public static class Remove{
        @NotNull
        private Long productId;

        @NotNull
        private Long productOptionId;
    }
}
