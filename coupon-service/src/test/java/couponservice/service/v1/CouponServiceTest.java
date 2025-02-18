package couponservice.service.v1;

import couponservice.common.exception.CustomGlobalException;
import couponservice.common.exception.ErrorType;
import couponservice.common.interceptor.UserIdInterceptor;
import couponservice.entity.Coupon;
import couponservice.entity.CouponPolicy;
import couponservice.entity.CouponStatus;
import couponservice.repository.CouponPolicyRepository;
import couponservice.repository.CouponRepository;
import couponservice.service.dto.v1.CouponRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponPolicyRepository couponPolicyRepository;

    private MockedStatic<UserIdInterceptor> mockStatic;

    @BeforeEach
    void setUp() {
        mockStatic = mockStatic(UserIdInterceptor.class);
        mockStatic.when(UserIdInterceptor::getCurrentUserId).thenReturn(1L);
    }

    @AfterEach
    void tearDown() {
        mockStatic.close();
    }

    @Test
    @DisplayName("쿠폰 발급에 성공한다.")
    void issue() {
        //given
        long couponPolicyId = 1L;
        CouponRequest.Issue request = CouponRequest.Issue.builder()
                .couponPolicyId(couponPolicyId)
                .build();

        Optional<CouponPolicy> optionalCouponPolicy =
                Optional.of(CouponPolicy.builder()
                        .id(couponPolicyId)
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .totalQuantity(10)
                        .build());
        given(couponPolicyRepository.findById(couponPolicyId))
                .willReturn(optionalCouponPolicy);
        given(couponRepository.countByCouponPolicyId(couponPolicyId))
                .willReturn(2L);

        Coupon mockCoupon = Coupon.create(optionalCouponPolicy.get(), 1L, "testCode");
        given(couponRepository.save(any()))
                .willReturn(mockCoupon);
        //when
        Coupon coupon = couponService.issue(request);
        //then
        assertThat(coupon).extracting("status", "userId")
                .containsExactlyInAnyOrder(CouponStatus.AVAILABLE, 1L);
    }

    @Test
    @DisplayName("쿠폰 정책에서 사용 기간이 전일때 쿠폰을 발급하면 예외가 발생한다.")
    void issue_before_start_time() {
        //given
        CouponRequest.Issue request = CouponRequest.Issue.builder().couponPolicyId(1L).build();

        Optional<CouponPolicy> optionalCouponPolicy =
                Optional.of(CouponPolicy.builder()
                        .startTime(LocalDateTime.now().plusDays(1))
                        .build());
        given(couponPolicyRepository.findById(any()))
                .willReturn(optionalCouponPolicy);
        //when
        //then
        assertThatThrownBy(() -> couponService.issue(request))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_NOT_ISSUABLE_PERIOD.getMessage());
    }

    @Test
    @DisplayName("쿠폰 정책에서 종료 기간이 지나고 쿠폰을 발급하면 예외가 발생한다.")
    void issue_after_end_time() {
        //given
        CouponRequest.Issue request = CouponRequest.Issue.builder().couponPolicyId(1L).build();

        Optional<CouponPolicy> optionalCouponPolicy =
                Optional.of(CouponPolicy.builder()
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().minusDays(1))
                        .build());
        given(couponPolicyRepository.findById(any()))
                .willReturn(optionalCouponPolicy);
        //when
        //then
        assertThatThrownBy(() -> couponService.issue(request))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_NOT_ISSUABLE_PERIOD.getMessage());
    }

    @Test
    @DisplayName("쿠폰을 발급할때 쿠폰 정책에서 설정한 쿠폰 수량이 넘어가면 예외가 발생한다.")
    void issue_exceed_total_quantity() {
        //given
        long couponPolicyId = 1L;
        CouponRequest.Issue request = CouponRequest.Issue.builder()
                .couponPolicyId(couponPolicyId)
                .build();

        Optional<CouponPolicy> optionalCouponPolicy =
                Optional.of(CouponPolicy.builder()
                        .id(couponPolicyId)
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .totalQuantity(2)
                        .build());
        given(couponPolicyRepository.findById(couponPolicyId))
                .willReturn(optionalCouponPolicy);
        given(couponRepository.countByCouponPolicyId(couponPolicyId))
                .willReturn(3L);
        //when
        //then
        assertThatThrownBy(() -> couponService.issue(request))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_QUANTITY_EXHAUSTED.getMessage());
    }

    @Test
    @DisplayName("쿠폰을 발급할때 쿠폰 정책에서 설정한 쿠폰 수량이 같으면 예외가 발생한다.")
    void issue_equal_total_quantity() {
        //given
        long couponPolicyId = 1L;
        CouponRequest.Issue request = CouponRequest.Issue.builder()
                .couponPolicyId(couponPolicyId)
                .build();

        Optional<CouponPolicy> optionalCouponPolicy =
                Optional.of(CouponPolicy.builder()
                        .id(couponPolicyId)
                        .startTime(LocalDateTime.now().minusDays(1))
                        .endTime(LocalDateTime.now().plusDays(1))
                        .totalQuantity(2)
                        .build());
        given(couponPolicyRepository.findById(couponPolicyId))
                .willReturn(optionalCouponPolicy);
        given(couponRepository.countByCouponPolicyId(couponPolicyId))
                .willReturn(2L);
        //when
        //then
        assertThatThrownBy(() -> couponService.issue(request))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_QUANTITY_EXHAUSTED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 쿠폰을 사용하려고 하면 예외가 발생한다")
    void use_coupon_not_found() {
        long userId = 1L;
        long couponId = 1L;

        given(couponRepository.findByIdAndUserId(couponId, userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.use(couponId, userId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.NOT_FOUND_COUPON.getMessage());
    }

    @Test
    @DisplayName("이미 사용된 쿠폰을 다시 사용하려는 경우 예외가 발생한다")
    void use_already_used_coupon() {
        long userId = 1L;
        long couponId = 1L;

        Coupon coupon = Coupon.builder()
                .usedAt(LocalDateTime.now())
                .status(CouponStatus.USED)
                .build();
        given(couponRepository.findByIdAndUserId(couponId, userId))
                .willReturn(Optional.of(coupon));

        assertThatThrownBy(() -> couponService.use(couponId, userId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_ALREADY_USED.getMessage());
    }

    @Test
    @DisplayName("만료된 쿠폰을 사용하려고 하면 예외가 발생한다")
    void use_expired_coupon() {
        //given
        long couponId = 1L;
        long orderId = 1L;

        CouponPolicy expiredPolicy = CouponPolicy.builder()
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().minusDays(1))
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .id(couponId)
                .userId(1L)
                .status(CouponStatus.AVAILABLE)
                .couponPolicy(expiredPolicy)
                .build();

        given(couponRepository.findByIdAndUserId(couponId, 1L))
                .willReturn(Optional.of(expiredCoupon));

        //when //then
        assertThatThrownBy(() -> couponService.use(couponId, orderId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_EXPIRED.getMessage());
    }

    @Test
    @DisplayName("사용된 쿠폰이 아닌것을 쿠폰을 취소할때 예외가 발생한다.")
    void cancel_not_used() {
        //given
        long couponId = 1L;

        CouponPolicy couponPolicy = CouponPolicy.builder()
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .userId(1L)
                .status(CouponStatus.AVAILABLE)
                .couponPolicy(couponPolicy)
                .build();

        given(couponRepository.findByIdAndUserId(couponId, 1L))
                .willReturn(Optional.of(coupon));

        //when //then
        assertThatThrownBy(() -> couponService.cancel(couponId))
                .isInstanceOf(CustomGlobalException.class)
                .hasMessage(ErrorType.COUPON_NOT_USED.getMessage());
    }

    @Test
    @DisplayName("쿠폰을 취소하면 orderId, usedAt이 null이 되어야 하고 쿠폰 상태가 CANCEL이 되어야 한다")
    void cancel() {
        //given
        long couponId = 1L;

        CouponPolicy couponPolicy = CouponPolicy.builder()
                .startTime(LocalDateTime.now().minusDays(2))
                .endTime(LocalDateTime.now().plusDays(1))
                .build();

        Coupon coupon = Coupon.builder()
                .id(couponId)
                .userId(1L)
                .orderId(1L)
                .usedAt(LocalDateTime.now())
                .status(CouponStatus.USED)
                .couponPolicy(couponPolicy)
                .build();

        given(couponRepository.findByIdAndUserId(couponId, 1L))
                .willReturn(Optional.of(coupon));

        //when
        Coupon cancelCoupon = couponService.cancel(couponId);
        //then
        assertThat(cancelCoupon).extracting("usedAt", "orderId", "status")
                .containsExactly(null, null, CouponStatus.CANCELED);
    }

    @Test
    @DisplayName("쿠폰 목록을 조회한다")
    void getCoupons() {
        //given
        CouponRequest.GetList request = CouponRequest.GetList.builder()
                .status(CouponStatus.AVAILABLE)
                .page(0)
                .size(10)
                .build();

        List<Coupon> coupons = List.of(
                Coupon.builder()
                        .id(1L)
                        .userId(1L)
                        .status(CouponStatus.AVAILABLE)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Coupon.builder()
                        .id(2L)
                        .userId(1L)
                        .status(CouponStatus.AVAILABLE)
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .build()
        );

        Page<Coupon> couponPage = new PageImpl<>(coupons);

        given(couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(1L),
                eq(CouponStatus.AVAILABLE),
                any(PageRequest.class)))
                .willReturn(couponPage);

        //when
        Page<Coupon> result = couponService.getCoupons(request);

        //then
        assertThat(result.getContent())
                .hasSize(2)
                .extracting("id", "userId", "status")
                .containsExactly(
                        tuple(1L, 1L, CouponStatus.AVAILABLE),
                        tuple(2L, 1L, CouponStatus.AVAILABLE)
                );
    }

    @Test
    @DisplayName("페이지 정보가 없을 경우 기본값으로 조회한다")
    void getCoupons_with_default_page() {
        //given
        CouponRequest.GetList request = CouponRequest.GetList.builder()
                .status(CouponStatus.AVAILABLE)
                .build();

        List<Coupon> coupons = List.of(
                Coupon.builder()
                        .id(1L)
                        .userId(1L)
                        .status(CouponStatus.AVAILABLE)
                        .build()
        );

        Page<Coupon> couponPage = new PageImpl<>(coupons);

        given(couponRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                eq(1L),
                eq(CouponStatus.AVAILABLE),
                eq(PageRequest.of(0, 10))))  // 기본값 검증
                .willReturn(couponPage);

        //when
        Page<Coupon> result = couponService.getCoupons(request);

        //then
        assertThat(result.getContent())
                .hasSize(1)
                .extracting("id", "userId", "status")
                .containsExactly(
                        tuple(1L, 1L, CouponStatus.AVAILABLE)
                );
    }
}