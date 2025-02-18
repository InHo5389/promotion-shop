package couponservice.service.dto.v1;

import couponservice.entity.CouponStatus;
import lombok.*;

public class CouponRequest {
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue{
        private Long couponPolicyId;
    }


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Use{
        private Long orderId;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetList{
        private CouponStatus status;
        private Integer page;
        private Integer size;
    }
}
