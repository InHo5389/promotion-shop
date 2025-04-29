package couponservice.service.v3;

import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.Coupon;
import couponservice.entity.CouponPolicy;
import couponservice.entity.DiscountType;
import outboxmessagerelay.MessageRelay;
import outboxmessagerelay.OutboxEvent;
import outboxmessagerelay.OutboxEventPublisher;
import outboxmessagerelay.entity.Outbox;
import outboxmessagerelay.repository.OutboxRepository;
import couponservice.repository.CouponRepository;
import couponservice.repository.v2.CouponLockRepository;
import couponservice.repository.v2.CouponPolicyRedisRepository;
import couponservice.repository.v2.CouponRedisRepository;
import couponservice.service.dto.v1.CouponRequest;
import couponservice.service.dto.v3.CouponDto;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceKafkaTest {
    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponLockRepository couponLockRepository;

    @Mock
    private CouponRedisRepository couponRedisRepository;

    @Mock
    private CouponProducer couponProducer;

    @Mock
    private CouponPolicyRedisRepository couponPolicyRedisRepository;

    @Mock
    private OutboxEventPublisher outboxEventPublisher;

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private RLock lock;

    @InjectMocks
    private CouponService couponService;

    @Captor
    private ArgumentCaptor<CouponDto.IssueMessage> messageCaptor;

    private CouponPolicy couponPolicy;
    private CouponRequest.Issue issueRequest;
    private Long userId = 1L;
    private Long policyId = 100L;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusDays(1);
        LocalDateTime endTime = now.plusDays(1);

        couponPolicy = CouponPolicy.builder()
                .id(policyId)
                .title("Test Coupon")
                .description("Test Description")
                .discountType(DiscountType.FIXED_DISCOUNT)
                .discountValue(1000)
                .minimumOrderAmount(10000)
                .maximumDiscountAmount(1000)
                .totalQuantity(100)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        issueRequest = new CouponRequest.Issue();
        // Assuming setters exist or using reflection to set fields
        ReflectionTestUtils.setField(issueRequest, "couponPolicyId", policyId);

        UserIdInterceptor.setTestUserId(userId);
    }

    /**
     * 이 테스트는 쿠폰 발급 요청 시 Outbox에 메시지가 정상적으로 저장되는지 검증합니다.
     * <p>
     * Outbox 패턴의 첫 단계는 메시지를 Outbox 테이블에 저장하는 것입니다.
     * 이 테스트는 쿠폰 발급 요청이 들어올 때 해당 요청이 Outbox 테이블에
     * 올바르게 저장되는지 확인합니다.
     */
    @Test
    @DisplayName("쿠폰 발급 요청 시 Outbox에 메시지가 저장되어야 함")
    void shouldSaveOutboxMessageWhenRequestingCouponIssue() {
        // Given
        when(couponLockRepository.getLock(policyId)).thenReturn(lock);
        when(couponLockRepository.tryLock(lock)).thenReturn(true);
        when(couponPolicyRedisRepository.getCouponPolicy(policyId)).thenReturn(Optional.of(couponPolicy));
        when(couponPolicyRedisRepository.decrementQuantity(policyId)).thenReturn(true);

        // When
        couponService.requestCouponIssue(issueRequest);

        // Then
        verify(outboxEventPublisher).publishCouponIssueRequest(messageCaptor.capture());
        CouponDto.IssueMessage capturedMessage = messageCaptor.getValue();
        assertThat(capturedMessage).isNotNull();
        assertThat(capturedMessage.getPolicyId()).isEqualTo(policyId);
        assertThat(capturedMessage.getUserId()).isEqualTo(userId);

        verify(couponLockRepository).unlock(lock);
    }

    /**
     * 이 테스트는 메시지 발행 성공 시 Outbox 메시지 삭제를 검증합니다.
     * <p>
     * Outbox 패턴에서는 메시지가 성공적으로 발행된 후에 Outbox 테이블에서
     * 해당 메시지를 삭제해야 합니다. 이 테스트는 메시지 발행이 성공하면
     * Outbox 테이블에서 해당 메시지가 정상적으로 삭제되는지 확인합니다.
     */
    @Test
    @DisplayName("메시지 발행 성공 시 Outbox 메시지가 삭제되어야 함")
    void shouldDeleteOutboxMessageWhenPublishSucceeds() throws Exception {
        // Given
        MessageRelay messageRelay = new MessageRelay(outboxRepository, kafkaTemplate);

        String topic = "coupon-issue-requests";
        String payload = "{\"policyId\":100,\"userId\":1}";
        Outbox outbox = Outbox.create(topic, payload);

        // ListenableFuture 모킹하는 대신, 직접 CompletableFuture를 사용
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>(topic, payload), null)
        );

        when(kafkaTemplate.send(topic, payload)).thenReturn(future);

        // MessageRelay에서 Outbox를 처리하기 위해 OutboxEvent로 감싸기
        OutboxEvent outboxEvent = OutboxEvent.of(outbox);

        // When
        messageRelay.publishEvent(outboxEvent);

        // Then
        verify(outboxRepository).delete(outbox);
    }

    /**
     * 이 테스트는 메시지 발행 실패 시 Outbox 메시지가 유지되는지 검증합니다.
     * <p>
     * Outbox 패턴의 핵심은 메시지 발행 실패 시 메시지를 계속 유지하여
     * 나중에 재시도할 수 있도록 하는 것입니다. 이 테스트는 Kafka 메시지 발행 중
     * 예외가 발생할 경우 Outbox 메시지가 삭제되지 않고 유지되는지 확인합니다.
     */
    @Test
    @DisplayName("메시지 발행 실패 시 Outbox 메시지가 유지되어야 함")
    void shouldKeepOutboxMessageWhenPublishFails() throws Exception {
        // Given
        MessageRelay messageRelay = new MessageRelay(outboxRepository, kafkaTemplate);

        String topic = "coupon-issue-requests";
        String payload = "{\"policyId\":100,\"userId\":1}";
        Outbox outbox = Outbox.create(topic, payload);

        // 실패하는 CompletableFuture 생성
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("발행 실패"));

        when(kafkaTemplate.send(topic, payload)).thenReturn(future);

        // MessageRelay에서 Outbox를 처리하기 위해 OutboxEvent로 감싸기
        OutboxEvent outboxEvent = OutboxEvent.of(outbox);

        // When
        messageRelay.publishEvent(outboxEvent);

        // Then
        verify(outboxRepository, never()).delete(outbox);
    }

    /**
     * 이 테스트는 주기적인 폴링을 통해 미처리된 메시지를 발행하는지 검증합니다.
     *
     * Outbox 패턴에서는 메시지 발행에 실패한 경우, 해당 메시지를 Outbox 테이블에 보관하고
     * 주기적으로 폴링하여 재시도합니다. 이 테스트는 10초 이상 지난 미처리 메시지들이
     * 주기적인 폴링을 통해 발행되는지 확인합니다.
     */
    @Test
    @DisplayName("주기적인 폴링을 통해 미처리된 메시지를 발행해야 함")
    void shouldPublishPendingMessagesViaPeriodicalPolling() throws Exception {
        // Given
        MessageRelay messageRelay = spy(new MessageRelay(outboxRepository, kafkaTemplate));

        LocalDateTime tenSecondsAgo = LocalDateTime.now().minusSeconds(10);

        Outbox outbox1 = Outbox.create("topic1", "payload1");
        Outbox outbox2 = Outbox.create("topic2", "payload2");

        List<Outbox> pendingMessages = List.of(outbox1, outbox2);

        when(outboxRepository.findAllByCreatedAtLessThanEqualOrderByCreatedAtAsc(
                any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(pendingMessages);

        // kafka로의 메시지 발행은 성공하도록 설정
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(
                new SendResult<>(new ProducerRecord<>("topic", "payload"), null)
        );
        when(kafkaTemplate.send(anyString(), anyString())).thenReturn(future);

        // When
        messageRelay.publishPendingMessages();

        // Then
        // 각 메시지가 처리됐는지 확인
        verify(outboxRepository, times(pendingMessages.size())).delete(any(Outbox.class));
    }

    /**
     * 이 테스트는 Kafka 메시지 소비 및 쿠폰 발급 프로세스를 검증합니다.
     * <p>
     * Outbox 패턴과 Kafka를 이용한 비동기 메시징 아키텍처에서 중요한 부분은
     * 메시지가 정상적으로 소비되어 처리되는 것입니다. 이 테스트는 Kafka 메시지가
     * 소비되어 실제 쿠폰이 발급되는 시나리오를 검증합니다.
     * 이 부분은 분산 트랜잭션의 일관성을 보장하는 데 중요합니다.
     */
    @Test
    @DisplayName("Kafka 메시지를 소비하여 쿠폰이 정상적으로 발급되어야 함")
    void shouldIssueCouponWhenConsumingKafkaMessage() {
        // Given
        CouponDto.IssueMessage message = CouponDto.IssueMessage.builder()
                .policyId(policyId)
                .userId(userId)
                .build();

        when(couponPolicyRedisRepository.getCouponPolicy(policyId)).thenReturn(Optional.of(couponPolicy));

        ArgumentCaptor<Coupon> couponCaptor = ArgumentCaptor.forClass(Coupon.class);
        when(couponRepository.save(couponCaptor.capture())).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            // ID 설정 모방 (보통 DB에서 자동 생성됨)
            ReflectionTestUtils.setField(coupon, "id", 1L);
            return coupon;
        });

        // When
        couponService.issue(message);

        // Then
        Coupon savedCoupon = couponCaptor.getValue();

        assertThat(savedCoupon).isNotNull();
        assertThat(savedCoupon.getUserId()).isEqualTo(userId);
        assertThat(savedCoupon.getCouponPolicy()).isEqualTo(couponPolicy);
    }
    /**
     * 이 테스트는 Kafka 연결 타임아웃 발생 시 Outbox 메시지가 유지되는지 검증합니다.
     *
     * 분산 시스템에서 네트워크 장애나 일시적인 서비스 중단은 흔히 발생하는 문제입니다.
     * 이 테스트는 Kafka 서버와의 연결 타임아웃이 발생할 경우, 메시지 손실 없이
     * Outbox에 메시지가 유지되어 나중에 재시도될 수 있는지 확인합니다.
     */
    @Test
    @DisplayName("Kafka 연결 타임아웃 발생 시 Outbox 메시지가 유지되어야 함")
    void shouldKeepOutboxMessageWhenKafkaTimeoutOccurs() throws Exception {
        // Given
        MessageRelay messageRelay = new MessageRelay(outboxRepository, kafkaTemplate);

        String topic = "coupon-issue-requests";
        String payload = "{\"policyId\":100,\"userId\":1}";
        Outbox outbox = Outbox.create(topic, payload);

        // 타임아웃 예외로 CompletableFuture 생성
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException("Connection timed out"));

        when(kafkaTemplate.send(topic, payload)).thenReturn(future);

        // MessageRelay에서 Outbox를 처리하기 위해 OutboxEvent로 감싸기
        OutboxEvent outboxEvent = OutboxEvent.of(outbox);

        // When
        messageRelay.publishEvent(outboxEvent);

        // Then
        // Outbox 메시지가 삭제되지 않았는지 확인
        verify(outboxRepository, never()).delete(outbox);
    }

    /**
     * 이 테스트는 Kafka 브로커가 모두 다운된 극단적인 상황에서도
     * Outbox 패턴이 메시지 손실을 방지하는지 검증합니다.
     *
     * 모든 Kafka 브로커가 다운된 경우, 즉시 예외가 발생하며 메시지 발행이 실패합니다.
     * 이런 상황에서도 Outbox에 메시지가 유지되어 Kafka 브로커가 복구된 후
     * 메시지가 처리될 수 있어야 합니다.
     */
    @Test
    @DisplayName("Kafka 브로커가 모두 다운된 경우 Outbox 메시지가 유지되어야 함")
    void shouldKeepOutboxMessageWhenAllKafkaBrokersDown() throws Exception {
        // Given
        MessageRelay messageRelay = new MessageRelay(outboxRepository, kafkaTemplate);

        String topic = "coupon-issue-requests";
        String payload = "{\"policyId\":100,\"userId\":1}";
        Outbox outbox = Outbox.create(topic, payload);

        // Kafka 브로커가 모두 다운된 상황을 시뮬레이션
        when(kafkaTemplate.send(topic, payload))
                .thenThrow(new org.apache.kafka.common.errors.TimeoutException("No brokers available"));

        // MessageRelay에서 Outbox를 처리하기 위해 OutboxEvent로 감싸기
        OutboxEvent outboxEvent = OutboxEvent.of(outbox);

        // When
        messageRelay.publishEvent(outboxEvent);

        // Then
        // Outbox 메시지가 삭제되지 않았는지 확인
        verify(outboxRepository, never()).delete(outbox);
    }
}