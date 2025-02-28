package userservice.service.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import userservice.common.exception.CustomGlobalException;
import userservice.common.exception.ErrorType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    private Long userId;
    private List<CartItem> items;
    private LocalDateTime lastUpdated;

    public static Cart fromCartItemMap(Long userId, Map<String, CartItem> cartItemMap) {
        return Cart.builder()
                .userId(userId)
                .items(new ArrayList<>(cartItemMap.values()))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    public Optional<CartItem> findItem(Long productId, Long optionId) {
        if (items == null) {
            items = new ArrayList<>();
            return Optional.empty();
        }
        return items.stream()
                .filter(item -> item.getProductId().equals(productId) && item.getOptionId().equals(optionId))
                .findFirst();
    }

    public CartItem addOrUpdateItem(Long productId, Long optionId, int quantity) {
        Optional<CartItem> existingItem = findItem(productId, optionId);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (item.getQuantity() == quantity) {
                throw new CustomGlobalException(ErrorType.ALREADY_IN_CART);
            }

            item.updateQuantity(quantity);
            updateLastModified();
            return item;
        } else {
            CartItem newItem = CartItem.builder()
                    .productId(productId)
                    .optionId(optionId)
                    .quantity(quantity)
                    .addedAt(LocalDateTime.now())
                    .build();

            if (items == null) {
                items = new ArrayList<>();
            }
            items.add(newItem);
            updateLastModified();
            return newItem;
        }
    }

    public boolean hasItem(Long productId, Long optionId) {
        return findItem(productId, optionId).isPresent();
    }

    public void removeItem(Long productId, Long optionId) {
        Optional<CartItem> itemToRemove = findItem(productId, optionId);

        if (itemToRemove.isEmpty()) {
            throw new CustomGlobalException(ErrorType.NOT_FOUND_CART_PRODUCT);
        }

        items.remove(itemToRemove.get());
        updateLastModified();
    }

    private void updateLastModified() {
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isEmpty() {
        if (items == null) {
            return true;
        }
        return items.isEmpty();
    }
}