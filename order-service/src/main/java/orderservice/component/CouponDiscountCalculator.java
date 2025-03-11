package orderservice.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.entity.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
            discountAmount = originalAmount.multiply(
                    new BigDecimal(discountValue)
                            .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP)
            );

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
}
