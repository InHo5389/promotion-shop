package userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import userservice.client.ProductServiceClient;
import userservice.client.dto.ProductResponse;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;
import userservice.entity.User;
import userservice.repository.UserRepository;
import userservice.service.domain.Cart;
import userservice.service.domain.CartItem;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductServiceClient productClient;

    private static final int CART_EXPIRY_DAYS = 7;

    public CartResponse getCart(Long userId) {
        log.info("장바구니 조회 - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_USER));

        Map<String, CartItem> cartItemMap = cartRepository.getCart(userId);

        // 도메인 객체 생성
        Cart cart = Cart.fromCartItemMap(userId, cartItemMap);

        if (cart.isEmpty()) {
            return CartResponse.builder()
                    .userId(userId)
                    .username(user.getName())
                    .items(Collections.emptyList())
                    .totalPrice(BigDecimal.ZERO)
                    .build();
        }

        log.debug("장바구니 아이템 수 - userId: {}, itemCount: {}", userId, cart.getItems().size());

        List<CartProductResponse> itemResponses = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            // 제품 정보 가져오기
            ProductResponse product = productClient.getProduct(item.getProductId().toString());

            // 제품 옵션 찾기
            ProductResponse.ProductOptionDto option = findProductOption(product, item.getOptionId());

            // 장바구니 아이템 응답 생성
            CartProductResponse itemResponse = createCartProductResponse(item, product, option);
            itemResponses.add(itemResponse);

            // 총 가격 계산
            totalPrice = totalPrice.add(itemResponse.getTotalPrice());
        }

        log.debug("장바구니 아이템 수 - userId: {}, itemCount: {}", userId, cart.getItems().size());

        return CartResponse.builder()
                .userId(userId)
                .username(user.getName())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .build();
    }

    public CartItem addToCart(Long userId, CartRequest.Add request) {
        log.info("장바구니 추가 요청 - userId: {}, productId: {}, optionId: {}, quantity: {}",
                userId, request.getProductId(), request.getProductOptionId(), request.getQuantity());

        // 제품 정보 확인
        ProductResponse product = productClient.getProduct(request.getProductId().toString());

        if (!product.getStatus().equals("ACTIVE")) {
            log.info("장바구니 추가 실패 - 판매중이 아닌 상품 - userId: {}, productId: {}, status: {}",
                    userId, request.getProductId(), product.getStatus());
            throw new CustomGlobalException(ErrorType.PRODUCT_NOT_SELL);
        }

        // 옵션 정보 확인
        ProductResponse.ProductOptionDto option = findProductOption(product, request.getProductOptionId());

        // 재고 확인
        if (option.getStockQuantity() < request.getQuantity()) {
            log.info("장바구니 추가 실패 - 재고 부족 - userId: {}, productId: {}, optionId: {}, requested: {}, available: {}",
                    userId, request.getProductId(), request.getProductOptionId(), request.getQuantity(), option.getStockQuantity());
            throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
        }

        // 현재 장바구니 상태 확인
        Map<String, CartItem> cartItemMap = cartRepository.getCart(userId);

        // 도메인 객체 생성
        Cart cart = Cart.fromCartItemMap(userId, cartItemMap);

        // 도메인 로직 수행 - 아이템 추가 또는 업데이트
        CartItem cartItem = cart.addOrUpdateItem(
                request.getProductId(),
                request.getProductOptionId(),
                request.getQuantity()
        );

        // 저장소에 저장 (인프라스트럭처 레이어와의 상호작용)
        cartRepository.saveCartItem(userId, request.getProductId(), request.getProductOptionId(), cartItem);

        // 장바구니 만료 설정
        cartRepository.setCartExpire(userId, CART_EXPIRY_DAYS, TimeUnit.DAYS);

        log.info("장바구니 추가 성공 - userId: {}, productId: {}, optionId: {}, quantity: {}",
                userId, request.getProductId(), request.getProductOptionId(), request.getQuantity());
        return cartItem;
    }

    public CartItem updateOption(Long userId, Long originalProductId, Long originalOptionId, CartRequest.Update request) {
        log.info("장바구니 상품 옵션 변경 - userId: {}, originalProductId: {}, originalOptionId: {}, newProductId: {}, newOptionId: {}, newQuantity: {}",
                userId, originalProductId, originalOptionId, request.getProductId(), request.getProductOptionId(), request.getQuantity());

        // 제품 정보 확인
        ProductResponse product = productClient.getProduct(request.getProductId().toString());

        // 수량이 0보다 큰 경우에만 재고 체크
        if (request.getQuantity() > 0) {
            ProductResponse.ProductOptionDto option = findProductOption(product, request.getProductOptionId());
            if (option.getStockQuantity() < request.getQuantity()) {
                log.info("장바구니 옵션 변경 실패 - 재고 부족 - userId: {}, productId: {}, optionId: {}, requested: {}, available: {}",
                        userId, request.getProductId(), request.getProductOptionId(), request.getQuantity(), option.getStockQuantity());
                throw new CustomGlobalException(ErrorType.NOT_ENOUGH_STOCK);
            }
        }

        // 현재 장바구니 상태 확인
        Map<String, CartItem> cartItemMap = cartRepository.getCart(userId);

        // 원본 아이템 확인
        String originalItemKey = originalProductId + "::" + originalOptionId;
        if (!cartItemMap.containsKey(originalItemKey)) {
            log.info("장바구니 옵션 변경 실패 - 원본 상품 없음 - userId: {}, productId: {}, optionId: {}",
                    userId, originalProductId, originalOptionId);
            throw new CustomGlobalException(ErrorType.NOT_FOUND_CART_PRODUCT);
        }

        // 수량이 0 이하면 제품 삭제 후 종료
        if (request.getQuantity() <= 0) {
            log.info("장바구니 상품 삭제 (수량 0으로 업데이트) - userId: {}, productId: {}, optionId: {}",
                    userId, originalProductId, originalOptionId);
            cartRepository.removeCartItem(userId, originalProductId, originalOptionId);
            return null;
        }


        // 변경하려는 옵션이 이미 장바구니에 있는지 확인
        String newItemKey = request.getProductId() + "::" + request.getProductOptionId();
        if (!originalItemKey.equals(newItemKey) && cartItemMap.containsKey(newItemKey)) {
            CartItem existingItem = cartItemMap.get(newItemKey);
            if (existingItem.getQuantity() == request.getQuantity()) {
                log.info("장바구니 옵션 변경 실패 - 동일한 상품과 수량이 이미 존재함 - userId: {}, productId: {}, optionId: {}, quantity: {}",
                        userId, request.getProductId(), request.getProductOptionId(), request.getQuantity());
                throw new CustomGlobalException(ErrorType.ALREADY_IN_CART);
            }
        }

        // 원본 아이템 제거
        cartRepository.removeCartItem(userId, originalProductId, originalOptionId);

        // 새 아이템 생성 및 저장
        CartItem cartItem = CartItem.builder()
                .productId(request.getProductId())
                .optionId(request.getProductOptionId())
                .quantity(request.getQuantity())
                .addedAt(LocalDateTime.now())
                .build();

        cartRepository.saveCartItem(userId, request.getProductId(), request.getProductOptionId(), cartItem);

        log.info("장바구니 상품 옵션 변경 성공 - userId: {}, 원본: {}::{} → 변경: {}::{}, 수량: {}",
                userId, originalProductId, originalOptionId, request.getProductId(), request.getProductOptionId(), request.getQuantity());
        return cartItem;
    }

    public void remove(Long userId, CartRequest.Remove request) {
        log.info("장바구니 상품 삭제 요청 - userId: {}, productId: {}, optionId: {}",
                userId, request.getProductId(), request.getProductOptionId());

        // 현재 장바구니 상태 확인
        Map<String, CartItem> cartItemMap = cartRepository.getCart(userId);

        // 도메인 객체 생성
        Cart cart = Cart.fromCartItemMap(userId, cartItemMap);

        // 도메인 로직 수행 - 아이템 존재 확인 및 예외처리
        cart.removeItem(request.getProductId(), request.getProductOptionId());

        // 저장소에서 삭제
        cartRepository.removeCartItem(userId, request.getProductId(), request.getProductOptionId());

        log.info("장바구니 상품 삭제 성공 - userId: {}, productId: {}, optionId: {}",
                userId, request.getProductId(), request.getProductOptionId());
    }

    private ProductResponse.ProductOptionDto findProductOption(ProductResponse product, Long optionId) {
        return product.getOptions().stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT_OPTION));
    }

    private CartProductResponse createCartProductResponse(CartItem item, ProductResponse product, ProductResponse.ProductOptionDto option) {
        BigDecimal itemPrice = product.getPrice();
        BigDecimal totalPrice = itemPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartProductResponse.builder()
                .productId(item.getProductId())
                .productName(product.getName())
                .image(product.getImage())
                .status(product.getStatus())
                .optionId(item.getOptionId())
                .optionSize(option.getSize())
                .optionColor(option.getColor())
                .price(itemPrice)
                .quantity(item.getQuantity())
                .totalPrice(totalPrice)
                .build();
    }

    public void clearCart(Long userId) {
        log.info("장바구니 전체 비우기 요청 - userId: {}", userId);

        Map<String, CartItem> cartItemMap = cartRepository.getCart(userId);

        Cart cart = Cart.fromCartItemMap(userId, cartItemMap);

        if (!cart.isEmpty()) {
            cartRepository.deleteCart(userId);
            log.info("장바구니 전체 비우기 성공 - userId: {}, itemCount: {}", userId, cart.getItems().size());
        }
    }
}
