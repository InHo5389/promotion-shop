package couponservice.service.v3;

import couponservice.service.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponConsumer {

    private final CouponService couponService;

    /**
     * 토픽이 coupon-issue-requests인 경우
     * groupId가 coupon-service인 경우
     * 설정한 factory를 사용해서 해당 메시지를 역직렬화하여 하여 해당 메서드 호출
     */
    @KafkaListener(topics = "coupon-issue-requests", groupId = "coupon-service", containerFactory = "couponKafkaListenerContainerFactory")
    public void consumeCouponIssueRequest(CouponDto.IssueMessage message) {
        try {
            log.info("Received coupon issue request: {}", message);
            couponService.issue(message);
        } catch (Exception e) {
            log.error("Failed to process coupon issue request: {}", e.getMessage(), e);
        }
    }
}

