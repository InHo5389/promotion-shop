package orderservice.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;

import java.time.LocalDateTime;
import java.util.List;

public class OrderResponse {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private Long orderId;
        private Long userId;
        private Integer totalAmount;
        private Integer couponDiscount;
        private Integer pointDiscount;
        private Integer finalAmount;
        private String status;
        private String address;
        private String receiverName;
        private String receiverPhone;
        private String paymentMethod;
        private LocalDateTime expiresAt;
        private List<OrderItemDto> items;

        public static Create from(Order order) {
            List<OrderItemDto> items = order.getOrderItems().stream()
                    .map(OrderItemDto::from)
                    .toList();

            return Create.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .couponDiscount(order.getCouponDiscount())
                    .pointDiscount(order.getPointDiscount())
                    .finalAmount(order.getFinalAmount())
                    .status(order.getStatus().name())
                    .address(order.getAddress())
                    .receiverName(order.getReceiverName())
                    .receiverPhone(order.getReceiverPhone())
                    .paymentMethod(order.getPaymentMethod())
                    .expiresAt(order.getExpiresAt())
                    .items(items)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Confirm {
        private Long orderId;
        private Long userId;
        private String status;
        private Integer finalAmount;
        private LocalDateTime confirmedAt;

        public static Confirm from(Order order) {
            return Confirm.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .status(order.getStatus().name())
                    .finalAmount(order.getFinalAmount())
                    .confirmedAt(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Cancel {
        private Long orderId;
        private String status;
        private LocalDateTime cancelledAt;

        public static Cancel from(Order order) {
            return Cancel.builder()
                    .orderId(order.getId())
                    .status(order.getStatus().name())
                    .cancelledAt(LocalDateTime.now())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Detail {
        private Long orderId;
        private Long userId;
        private Integer totalAmount;
        private Integer couponDiscount;
        private Integer pointDiscount;
        private Integer finalAmount;
        private String status;
        private String address;
        private String receiverName;
        private String receiverPhone;
        private String paymentMethod;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private List<OrderItemDto> items;

        public static Detail from(Order order) {
            List<OrderItemDto> items = order.getOrderItems().stream()
                    .map(OrderItemDto::from)
                    .toList();

            return Detail.builder()
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .totalAmount(order.getTotalAmount())
                    .couponDiscount(order.getCouponDiscount())
                    .pointDiscount(order.getPointDiscount())
                    .finalAmount(order.getFinalAmount())
                    .status(order.getStatus().name())
                    .address(order.getAddress())
                    .receiverName(order.getReceiverName())
                    .receiverPhone(order.getReceiverPhone())
                    .paymentMethod(order.getPaymentMethod())
                    .expiresAt(order.getExpiresAt())
                    .createdAt(order.getCreatedAt())
                    .items(items)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemDto {
        private Long productOptionId;
        private Integer quantity;
        private Integer price;
        private Integer totalPrice;
        private Long couponId;
        private Integer couponDiscount;

        public static OrderItemDto from(OrderItem item) {
            return OrderItemDto.builder()
                    .productOptionId(item.getProductOptionId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .totalPrice(item.getTotalPrice())
                    .couponId(item.getCouponId())
                    .couponDiscount(item.getCouponDiscount())
                    .build();
        }
    }
}