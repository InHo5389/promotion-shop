package orderservice.service.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.CouponClient;
import orderservice.client.dto.CouponResponse;
import orderservice.component.CouponDiscountCalculator;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.service.dto.ProductCouponInfo;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service("DiscountServiceV2")
@RequiredArgsConstructor
public class DiscountService {

    private final CouponDiscountCalculator couponDiscountCalculator;
    private final CouponClient couponClient;

    @Transactional
    public void calculateDiscountOnly(Order order, List<ProductCouponInfo> productCoupons) {
        // 쿠폰 할인만 계산
        calculateCouponDiscount(order, productCoupons);
    }

    @Transactional
    public void calculateTotalDiscount(Order order, List<ProductCouponInfo> productCoupons, Long pointAmount) {
        // 쿠폰 할인 계산
        calculateCouponDiscount(order, productCoupons);

        // 포인트 할인 계산 (있는 경우에만)
        if (pointAmount != null && pointAmount > 0) {
            applyPointDiscount(order, pointAmount);
        }
    }

    private void calculateCouponDiscount(Order order, List<ProductCouponInfo> productCoupons) {
        if (productCoupons == null || productCoupons.isEmpty()) {
            log.info("DiscountService calculateCouponDiscount null");
            return;
        }

        Map<Pair<Long, Long>, OrderItem> orderItemMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(
                        item -> Pair.of(item.getProductId(), item.getProductOptionId()),
                        item -> item));

        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        for (ProductCouponInfo couponInfo : productCoupons) {
            if (couponInfo.getCouponId() == null) {
                continue;
            }

            OrderItem orderItem = orderItemMap.get(
                    Pair.of(couponInfo.getProductId(), couponInfo.getProductOptionId()));

            if (orderItem == null) {
                log.warn("Order item not found for product ID: {}, option ID: {}",
                        couponInfo.getProductId(), couponInfo.getProductOptionId());
                continue;
            }

            // 쿠폰 정보 조회 (사용하지 않고 정보만 가져옴)
            CouponResponse.Response couponInfoDto = couponClient.getCoupon(couponInfo.getCouponId());

            // 할인 금액 계산
            BigDecimal discountAmount = couponDiscountCalculator.calculateDiscountAmount(
                    couponInfoDto.getDiscountType(),
                    couponInfoDto.getDiscountValue(),
                    couponInfoDto.getMaximumDiscountAmount(),
                    orderItem.getUnitPrice());

            // OrderItem의 메서드를 사용하여 할인 적용
            orderItem.applyCouponDiscount(couponInfo.getCouponId(), discountAmount);

            // 총 할인 금액 누적
            totalDiscountAmount = totalDiscountAmount.add(discountAmount);

            log.info("Coupon discount calculated orderId={}, couponId={}, productId={}, discountAmount={}",
                    order.getId(), couponInfo.getCouponId(), orderItem.getProductId(), discountAmount);
        }

        // 쿠폰 할인 금액 적용
        order.applyCouponDiscount(totalDiscountAmount);

        log.info("Coupon discount calculation completed orderId={}, totalDiscountAmount={}, finalAmount={}",
                order.getId(), totalDiscountAmount, order.getFinalAmount());
    }

    private void applyPointDiscount(Order order, Long pointAmount) {
        // 포인트 사용 가능 여부 검증
        BigDecimal totalAfterCouponDiscount = order.getTotalAmountAfterDiscount();
        BigDecimal pointAmountDecimal = new BigDecimal(pointAmount);

        // 주문 총액보다 포인트 사용액이 크지 않도록 검증
        if (totalAfterCouponDiscount.compareTo(pointAmountDecimal) < 0) {
            log.warn("Point amount exceeds order total after discount. orderId={}, pointAmount={}, totalAfterDiscount={}",
                    order.getId(), pointAmount, totalAfterCouponDiscount);
            throw new IllegalArgumentException("포인트 사용 금액이 결제 금액보다 클 수 없습니다.");
        }

        // 포인트 사용 조건 검증 (100원 이상, 10원 단위 사용 불가)
        if (pointAmount < 100) {
            log.warn("Point amount is less than minimum. orderId={}, pointAmount={}",
                    order.getId(), pointAmount);
            throw new IllegalArgumentException("포인트는 100원 이상부터 사용 가능합니다.");
        }

        if (pointAmount % 10 == 0 && pointAmount % 100 != 0) {
            log.warn("Point amount is in invalid units. orderId={}, pointAmount={}",
                    order.getId(), pointAmount);
            throw new IllegalArgumentException("포인트는 10원 단위로 사용할 수 없습니다.");
        }

        // 포인트 사용 적용
        order.applyPointDiscount(pointAmount);

        log.info("Point discount applied orderId={}, pointAmount={}, finalAmount={}",
                order.getId(), pointAmount, order.getFinalAmount());
    }
}