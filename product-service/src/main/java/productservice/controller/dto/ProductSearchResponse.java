package productservice.controller.dto;

import lombok.Builder;
import productservice.entity.Product;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ProductSearchResponse(
        Long id,
        String name,
        BigDecimal price,
        String image,
        String categoryName,
        List<OptionInfo> options
) {

    @Builder
    public record OptionInfo(
            Long id,
            String size,
            String color,
            BigDecimal additionalPrice,
            Integer stockQuantity
    ) { }

    public static ProductSearchResponse from(Product product) {
        List<OptionInfo> options = product.getOptions().stream()
                .map(option -> OptionInfo.builder()
                        .id(option.getId())
                        .size(option.getSize())
                        .color(option.getColor())
                        .additionalPrice(option.getAdditionalPrice())
                        .stockQuantity(option.getStock() != null ?
                                option.getStock().getQuantity() - option.getStock().getReservedQuantity() : 0)
                        .build())
                .toList();

        return ProductSearchResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .image(product.getImage())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .options(options)
                .build();
    }
}
