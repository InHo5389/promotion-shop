package couponservice.outboxmessagerelay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import couponservice.outboxmessagerelay.entity.Outbox;
import couponservice.service.dto.v3.CouponDto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 쿠폰 발급 요청 메시지를 Outbox 이벤트로 발행
     */
    public void publishCouponIssueRequest(CouponDto.IssueMessage message) {
        try {
            String messageJson = objectMapper.writeValueAsString(message);

            Outbox outbox = Outbox.create(
                    "coupon-issue-requests",
                    messageJson
            );

            applicationEventPublisher.publishEvent(OutboxEvent.of(outbox));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize coupon issue message", e);
        }
    }
}
