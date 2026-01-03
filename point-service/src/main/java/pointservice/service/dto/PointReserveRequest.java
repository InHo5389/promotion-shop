package pointservice.service.dto;

import lombok.Builder;

@Builder
public record PointReserveRequest(
        Long orderId,
        Long userId,
        Long amount
){
}
