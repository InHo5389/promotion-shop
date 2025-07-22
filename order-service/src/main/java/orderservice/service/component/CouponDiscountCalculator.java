package orderservice.service.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.CouponResponse;
import orderservice.client.serviceclient.CouponServiceClient;
import orderservice.common.exception.CompensationException;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.service.dto.ProductCouponInfo;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponDiscountCalculator {

    private static final String FIXED_DISCOUNT = "FIXED_DISCOUNT";
    private static final String RATE_DISCOUNT = "RATE_DISCOUNT";

    @Transactional
    public BigDecimal calculateDiscountAmount(String discountType, Integer discountValue,
                                              Integer maximumDiscountAmount, BigDecimal originalAmount) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        log.info("Coupon discount calculation - type: {}, value: {}, maxAmount: {}, originalAmount: {}",
                discountType, discountValue, maximumDiscountAmount, originalAmount);

        if (FIXED_DISCOUNT.equals(discountType)) {
            // 정액 할인
            discountAmount = new BigDecimal(discountValue);
        } else if (RATE_DISCOUNT.equals(discountType)){
            // 정률 할인
            discountAmount = originalAmount.multiply(new BigDecimal(discountValue)
                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));

            // 최대 할인 금액 적용
            BigDecimal maxDiscount = new BigDecimal(maximumDiscountAmount);
            if (discountAmount.compareTo(maxDiscount) > 0) {
                discountAmount = maxDiscount;
            }
        }

        // 할인 금액이 상품 가격을 초과하지 않도록 처리
        if (discountAmount.compareTo(originalAmount) > 0) {
            discountAmount = originalAmount;
        }

        return discountAmount;
    }

    @Transactional
    public BigDecimal calculateFinalTotalAmount(Order order) {
        return order.getOrderItems().stream()
                .map(item -> {
                    if (item.getDiscountedTotalPrice() != null) {
                        return item.getDiscountedTotalPrice();
                    } else {
                        return item.getTotalPrice();
                    }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateDiscount(BigDecimal originalAmount, String discountType, BigDecimal discountValue) {
        switch (discountType.toUpperCase()) {
            case RATE_DISCOUNT:
                // 퍼센트 할인: 원금 * (할인율 / 100)
                return originalAmount.multiply(discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            case FIXED_DISCOUNT:
                BigDecimal discount = discountValue;
                return discount.compareTo(originalAmount) > 0 ? originalAmount : discount;

            default:
                throw new CustomGlobalException(ErrorType.NOT_FOUND_DISCOUNT_TYPE);
        }
    }

    /**
     * 모든 상품 쿠폰 적용 (새로 추가)
     */
    @Transactional
    public void applyAllProductCoupons(Order order, List<ProductCouponInfo> productCoupons, CouponServiceClient couponClient) {

        for (ProductCouponInfo couponInfo : productCoupons) {
            if (couponInfo.getCouponId() == null) {
                continue;
            }

            // 해당 상품의 OrderItem 찾기
            OrderItem orderItem = order.getOrderItems().stream()
                    .filter(item -> item.getProductId().equals(couponInfo.getProductId())
                            && item.getProductOptionId().equals(couponInfo.getProductOptionId()))
                    .findFirst()
                    .orElse(null);

            if (orderItem == null) {
                log.warn("OrderItem not found for productId: {}", couponInfo.getProductId());
                continue;
            }

            try {
                // 쿠폰 정보 조회 및 사용
                CouponResponse.Response coupon = couponClient.getCoupon(couponInfo.getCouponId());
                couponClient.useCoupon(couponInfo.getCouponId(), order.getId());

                // 할인 금액 계산 (기존 메서드 사용)
                BigDecimal discountAmount = calculateDiscountAmount(
                        coupon.getDiscountType(),
                        coupon.getDiscountValue(),
                        coupon.getMaximumDiscountAmount(),
                        orderItem.getTotalPrice()
                );

                // OrderItem에 할인 적용
                applyCouponToOrderItem(orderItem, couponInfo.getCouponId(), discountAmount);

                log.info("Coupon applied - productId: {}, couponId: {}, discount: {}",
                        orderItem.getProductId(), couponInfo.getCouponId(), discountAmount);

            } catch (Exception e) {
                log.error("Failed to apply coupon - couponId: {}, error: {}",
                        couponInfo.getCouponId(), e.getMessage());
                throw new CompensationException("쿠폰 적용 실패: " + e.getMessage());
            }
        }

        // 주문 총액 재계산 (기존 메서드 사용)
        updateOrderAmounts(order);
    }

    /**
     * OrderItem에 쿠폰 할인 적용 (새로 추가)
     */
    private void applyCouponToOrderItem(OrderItem orderItem, Long couponId, BigDecimal discountAmount) {
        orderItem.setCouponId(couponId);
        orderItem.setDiscountPrice(discountAmount);

        BigDecimal discountedPrice = orderItem.getTotalPrice().subtract(discountAmount);
        if (discountedPrice.compareTo(BigDecimal.ZERO) < 0) {
            discountedPrice = BigDecimal.ZERO;
        }
        orderItem.setDiscountedTotalPrice(discountedPrice);
    }

    /**
     * 주문 금액 업데이트 (새로 추가)
     */
    private void updateOrderAmounts(Order order) {
        // 총 할인 금액 계산
        BigDecimal totalDiscount = order.getOrderItems().stream()
                .map(item -> item.getDiscountPrice() != null ? item.getDiscountPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setDiscountAmount(totalDiscount);

        // 최종 금액 계산 (기존 메서드 사용)
        BigDecimal finalAmount = calculateFinalTotalAmount(order);
        order.setTotalAmount(finalAmount);
    }
}
