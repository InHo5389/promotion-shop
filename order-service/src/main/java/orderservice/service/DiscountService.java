package orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.dto.CouponRequest;
import orderservice.client.dto.CouponResponse;
import orderservice.client.serviceclient.CouponServiceClient;
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
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final CouponServiceClient couponClient;
    private final CouponDiscountCalculator couponDiscountCalculator;

    @Transactional
    public void applyProductCoupons(Order order, List<ProductCouponInfo> productCoupons) {
        if (productCoupons == null || productCoupons.isEmpty()) {
            log.info("DiscountService applyProductCoupons null");
            return;
        }

        Map<Pair<Long, Long>, OrderItem> orderItemMap = order.getOrderItems().stream()
                .collect(Collectors.toMap(
                        item -> Pair.of(item.getProductId(), item.getProductOptionId()),
                        item -> item
                ));

        // 초반에 OrderItem에 couponId 설정 코드는 제거 (아래에서 한 번만 설정)

        BigDecimal totalDiscountAmount = BigDecimal.ZERO;

        for (ProductCouponInfo couponInfo : productCoupons) {
            if (couponInfo.getCouponId() == null) {
                continue;
            }

            OrderItem orderItem = orderItemMap.get(
                    Pair.of(couponInfo.getProductId(), couponInfo.getProductOptionId())
            );

            if (orderItem == null) {
                log.warn("Order item not found for product ID: {}, option ID: {}",
                        couponInfo.getProductId(), couponInfo.getProductOptionId());
                continue;
            }


            // 쿠폰 사용 API 호출
            CouponResponse.Response couponResponse = couponClient.use(couponInfo.getCouponId(), new CouponRequest.Use(order.getId()));

            // 할인 금액 계산
            BigDecimal discountAmount = couponDiscountCalculator.calculateDiscountAmount(
                    couponResponse.getDiscountType(),
                    couponResponse.getDiscountValue(),
                    couponResponse.getMaximumDiscountAmount(),
                    orderItem.getUnitPrice());

            // 주문 아이템에 쿠폰 정보 설정
            orderItem.setCouponId(couponInfo.getCouponId());
            orderItem.setDiscountPrice(discountAmount);

            // 할인 적용 후 가격 계산
            BigDecimal discountedTotalPrice = orderItem.getTotalPrice().subtract(discountAmount);
            if (discountedTotalPrice.compareTo(BigDecimal.ZERO) < 0) {
                discountedTotalPrice = BigDecimal.ZERO;
            }
            orderItem.setDiscountedTotalPrice(discountedTotalPrice);

            // 총 할인 금액 누적
            totalDiscountAmount = totalDiscountAmount.add(discountAmount);

            log.info("Applied coupon ID: {} to product ID: {}, discount amount: {}",
                    couponInfo.getCouponId(), orderItem.getProductId(), discountAmount);

        }

        // 주문 총액 재계산 (할인 적용 후)
        BigDecimal finalTotalAmount = couponDiscountCalculator.calculateFinalTotalAmount(order);
        order.setTotalAmount(finalTotalAmount);
    }
}
