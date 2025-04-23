package orderservice.service.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.ProductClient;
import orderservice.client.dto.CartProductResponse;
import orderservice.client.dto.ProductResponse;
import orderservice.client.serviceclient.ProductServiceClient;
import orderservice.common.exception.CustomGlobalException;
import orderservice.common.exception.ErrorType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartValidator {

    private final ProductServiceClient productClient;

    public void validateCartItems(List<CartProductResponse> cartItems) {
        // 상품 정보 일괄 조회
        List<Long> productIds = cartItems.stream()
                .map(CartProductResponse::getProductId)
                .distinct()
                .toList();

        List<ProductResponse> products = productClient.getProducts(productIds);
        log.debug("Products information retrieved - requested={}, received={}", productIds.size(), products.size());

        Map<Long, ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(
                        ProductResponse::getId,
                        p -> p,
                        (existing, replacement) -> existing));

        // 각 장바구니 아이템에 대해 재고 검증
        for (CartProductResponse cartItem : cartItems) {
            ProductResponse product = productMap.get(cartItem.getProductId());

            // 상품이 판매 중인지 확인
            if (!"ACTIVE".equals(product.getStatus())) {
                log.info("Attempt to order inactive product - productId={}, status={}",
                        cartItem.getProductId(), product.getStatus());
                throw new CustomGlobalException(ErrorType.PRODUCT_NOT_SELL);
            }

            // 옵션을 찾아 재고 확인
            ProductResponse.ProductOptionDTO option = product.getOptions().stream()
                    .filter(opt -> opt.getId().equals(cartItem.getOptionId()))
                    .findFirst()
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.OPTION_NOT_FOUND));

            if (option.getStockQuantity() < cartItem.getQuantity()) {
                throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
            }
        }
    }
}
