package timesaleservice.client.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ProductStatus {
    ACTIVE("판매중"),
    STOP("판매 종료"),
    SOLD_OUT("품절");

    private final String description;
}