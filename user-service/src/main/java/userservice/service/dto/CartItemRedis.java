package userservice.service.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRedis implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long productId;
    private Long optionId;
    private Integer quantity;
    private LocalDateTime addedAt;
}
