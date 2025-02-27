package userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import userservice.client.ProductClient;
import userservice.client.dto.ProductResponse;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.entity.User;
import userservice.repository.UserRepository;
import userservice.service.dto.CartItemRedis;
import userservice.service.dto.CartProductResponse;
import userservice.service.dto.CartRequest;
import userservice.service.dto.CartResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductClient productClient;

    public CartResponse getCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));

        Map<String, CartItemRedis> cartItemMap = cartRepository.getCartItems(userId);

        if (cartItemMap.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
                    .items(Collections.emptyList())
                    .totalPrice(BigDecimal.ZERO)
                    .build();
        }

        List<CartProductResponse> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Map.Entry<String, CartItemRedis> entry : cartItemMap.entrySet()) {
            CartItemRedis item = entry.getValue();

            // 제품 정보 가져오기
            ProductResponse product = productClient.getProduct(item.getProductId().toString());

            // 제품 옵션 찾기
            ProductResponse.ProductOptionDto option = findProductOption(product, item.getOptionId());

            // 장바구니 아이템 응답 생성
            CartProductResponse itemResponse = CartProductResponse.from(item, product, option);
            items.add(itemResponse);

            // 총 가격 계산
            totalPrice = totalPrice.add(itemResponse.getTotalPrice());
        }

        return CartResponse.builder()
                .userId(userId)
                .username(user.getName())
                .items(items)
                .totalPrice(totalPrice)
                .build();
    }

    // 장바구니에 상품 추가
    public CartItemRedis addToCart(Long userId, CartRequest.Add request) {
        // 제품 정보 확인
        ProductResponse product = productClient.getProduct(request.getProductId().toString());

        // 옵션 정보 확인
        ProductResponse.ProductOptionDto option = findProductOption(product, request.getProductOptionId());

        // 재고 확인
        if (option.getStockQuantity() < request.getQuantity()) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
        }

        // 현재 장바구니 상태 확인
        Map<String, CartItemRedis> cartItems = cartRepository.getCartItems(userId);
        String itemKey = request.getProductId() + "::" + request.getProductOptionId();
        CartItemRedis cartItem = cartItems.get(itemKey);

        if (cartItem != null) {
            // 이미 있으면 수량 업데이트
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
            cartItem.setAddedAt(LocalDateTime.now());
        } else {
            // 새 아이템 추가
            cartItem = CartItemRedis.builder()
                    .productId(request.getProductId())
                    .optionId(request.getProductOptionId())
                    .quantity(request.getQuantity())
                    .addedAt(LocalDateTime.now())
                    .build();
        }

        // Redis에 저장
        cartRepository.saveCartItem(userId, request.getProductId(), request.getProductOptionId(), cartItem);

        // 장바구니 만료 설정 - 7일
        cartRepository.setCartExpire(userId, 7, TimeUnit.DAYS);

        return cartItem;
    }

    // 제품에서 해당 옵션을 찾는 메서드
    private ProductResponse.ProductOptionDto findProductOption(ProductResponse product, Long optionId) {
        return product.getOptions().stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT_OPTION));
    }

    public CartItemRedis updateOption(Long userId, CartRequest.Update request) {
        // 제품 옵션 정보 확인 및 재고 체크
        ProductResponse product = productClient.getProduct(request.getProductId().toString());
        ProductResponse.ProductOptionDto option = findProductOption(product, request.getProductOptionId());

        if (request.getQuantity() > 0 && option.getStockQuantity() < request.getQuantity()) {
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
        }

        // 현재 장바구니 상태 확인
        Map<String, CartItemRedis> cartItems = cartRepository.getCartItems(userId);
        String itemKey = request.getProductId() + "::" + request.getProductOptionId();
        CartItemRedis cartItem = cartItems.get(itemKey);

        if (cartItem == null) {
            throw new CustomGlobalException(ErrorType.NOT_FOUND_CART_PRODUCT);
        }

        if (request.getQuantity() <= 0) {
            // 수량이 0 이하면 제품 삭제
            cartRepository.removeCartItem(userId, request.getProductId(), request.getProductOptionId());
        } else {
            // 수량 업데이트
            cartItem.setQuantity(request.getQuantity());
            cartItem.setAddedAt(LocalDateTime.now());
            cartRepository.saveCartItem(userId, request.getProductId(), request.getProductOptionId(), cartItem);
        }
        return cartItem;
    }

    public void remove(Long userId, CartRequest.Remove request) {
        // 현재 장바구니 상태 확인
        Map<String, CartItemRedis> cartItems = cartRepository.getCartItems(userId);
        String itemKey = request.getProductId() + "::" + request.getProductOptionId();

        if (!cartItems.containsKey(itemKey)) {
            throw new CustomGlobalException(ErrorType.NOT_FOUND_CART_PRODUCT);
        }

        // 상품 삭제
        cartRepository.removeCartItem(userId, request.getProductId(), request.getProductOptionId());
    }
}
