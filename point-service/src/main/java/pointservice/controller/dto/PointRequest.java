package pointservice.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

public class PointRequest {

    @Getter
    @Builder
    public static class Earn {

        @NotNull(message = "amount는 null이 될 수 없습니다.")
        @Min(value = 1,message = "amount는 0보다 커야합니다.")
        private Long amount;
    }

    @Getter
    @Builder
    public static class Use {

        @NotNull(message = "amount는 null이 될 수 없습니다.")
        @Min(value = 1,message = "amount는 0보다 커야합니다.")
        private Long amount;
    }
}
