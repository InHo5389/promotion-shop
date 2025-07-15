package timesaleservice.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TimeSaleStatus {

    SCHEDULED("예정"),
    ACTIVE("진행중"),
    ENDED("종료"),
    SOLD_OUT("매진");

    private final String value;

}
