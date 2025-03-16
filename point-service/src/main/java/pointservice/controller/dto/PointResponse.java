package pointservice.controller.dto;

import lombok.Builder;
import lombok.Getter;
import pointservice.entity.Point;
import pointservice.entity.PointType;

import java.time.LocalDateTime;

@Getter
@Builder
public class PointResponse {
    private Long id;
    private Long userId;
    private Long amount;
    private PointType type;
    private Long balanceSnapshot;
    private LocalDateTime createdAt;

    public static PointResponse from(Point point) {
        return PointResponse.builder()
                .id(point.getId())
                .userId(point.getUserId())
                .amount(point.getAmount())
                .type(point.getType())
                .balanceSnapshot(point.getBalanceSnapshot())
                .createdAt(point.getCreatedAt())
                .build();
    }

    @Getter
    @Builder
    public static class Balance {
        private Long userId;
        private Long balance;

        public static Balance of(Long userId, Long balance) {
            return Balance.builder()
                    .userId(userId)
                    .balance(balance)
                    .build();
        }
    }
}
