package orderservice.client.dto;

import lombok.Builder;
import lombok.Getter;

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
}
