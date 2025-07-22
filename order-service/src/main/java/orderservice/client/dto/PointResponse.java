package orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointResponse {
    private Long id;
    private Long userId;
    private Long amount;
    private PointType type;
    private Long balanceSnapshot;
    private LocalDateTime createdAt;
}
