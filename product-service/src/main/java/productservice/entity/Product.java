package productservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import productservice.common.BaseEntity;
import productservice.service.dto.ProductOptionRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options;

    private String name;
    private BigDecimal price;
    private String image;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    public static Product create(Category category, String name, BigDecimal price, String image) {
        Product product = new Product();
        product.category = category;
        product.options = new ArrayList<>();
        product.name = name;
        product.price = price;
        product.image = image;
        product.status = ProductStatus.ACTIVE;
        return product;
    }

    public void update(String name, BigDecimal price, String image, ProductStatus status) {
        if (name != null) this.name = name;
        if (price != null) this.price = price;
        if (image != null) this.image = image;
        if (status != null) this.status = status;
    }

    public void updateCategory(Category category) {
        this.category = category;
    }

    public void addOption(ProductOption option) {
        this.options.add(option);
        option.setProduct(this);
    }

    public void updateOptions(List<ProductOptionRequest.Update> options) {
        if (options == null) {
            return;
        }

        // 요청에 포함된 옵션 ID 집합
        Set<Long> requestOptionIds = options.stream()
                .filter(o -> o.getId() != null)
                .map(ProductOptionRequest.Update::getId)
                .collect(Collectors.toSet());

        // 삭제할 옵션 찾기 (요청에 포함되지 않은 기존 옵션)
        List<ProductOption> optionsToRemove = this.options.stream()
                .filter(option -> !requestOptionIds.contains(option.getId()))
                .toList();

        // 옵션 삭제
        this.options.removeAll(optionsToRemove);

        Map<Long, ProductOption> optionMap = this.options.stream()
                .collect(Collectors.toMap(ProductOption::getId, option -> option));

        for (ProductOptionRequest.Update request : options) {
            if (request.getId() != null && optionMap.containsKey(request.getId())) {
                // 기존 옵션 업데이트
                ProductOption option = optionMap.get(request.getId());
                option.update(
                        request.getSize(),
                        request.getColor(),
                        request.getAdditionalPrice()
                );

                // 재고 업데이트
                if (option.getStock() != null) {
                    // 기존 재고가 있으면 수량만 업데이트
                    option.getStock().updateQuantity(request.getStockQuantity());
                } else {
                    // 기존 재고가 없으면 새로 생성
                    ProductStock stock = ProductStock.create(option, request.getStockQuantity());
                    option.setStock(stock);
                }
            } else {
                // 새 옵션 추가
                ProductOption newOption = ProductOption.create(
                        this,
                        request.getSize(),
                        request.getColor(),
                        request.getAdditionalPrice()
                );
                this.addOption(newOption);

                // 새 옵션에 재고 추가
                ProductStock stock = ProductStock.create(newOption, request.getStockQuantity());
                newOption.setStock(stock);
            }
        }
    }
}
