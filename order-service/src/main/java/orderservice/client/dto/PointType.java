package orderservice.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointType {

    USED("사용"),
    EARNED("적립"),
    CANCELED("취소");

    private final String description;
}
