package productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import productservice.common.exception.CustomGlobalException;
import productservice.common.exception.ErrorType;
import productservice.entity.Category;
import productservice.entity.Product;
import productservice.entity.ProductOption;
import productservice.entity.ProductStock;
import productservice.repository.CategoryJpaRepository;
import productservice.repository.ProductJpaRepository;
import productservice.repository.ProductOptionJpaRepository;
import productservice.repository.ProductStockJpaRepository;
import productservice.service.dto.ProductRequest;
import productservice.service.dto.ProductResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final CategoryJpaRepository categoryJpaRepository;
    private final ProductJpaRepository productJpaRepository;
    private final ProductStockJpaRepository productStockJpaRepository;
    private final ProductOptionJpaRepository productOptionJpaRepository;

    private final int MAX_RETRIES = 3;

    @Transactional
    public ProductResponse create(ProductRequest.Create request) {

        log.info("ProductRequest.Create = {},{}", request.getCategoryId(), request.getPrice());
        Category category = categoryJpaRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY));
        Product product = Product.create(category, request.getName(), request.getPrice(), request.getImage());

        request.getOptions().forEach(optionReq -> {
            ProductOption option = ProductOption.create(
                    product,
                    optionReq.getSize(),
                    optionReq.getColor(),
                    optionReq.getAdditionalPrice()
            );

            ProductStock stock = ProductStock.create(
                    option,
                    optionReq.getStockQuantity()
            );

            option.setStock(stock);
            product.addOption(option);
        });

        return ProductResponse.from(productJpaRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long productId, ProductRequest.Update request) {
        Product product = productJpaRepository.findById(productId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT));

        if (request.getCategoryId() != null) {
            Category category = categoryJpaRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY));
            product.updateCategory(category);
        }

        product.update(request.getName(), request.getPrice(), request.getImage(), request.getStatus());

        if (request.getOptions() != null) {
            product.updateOptions(request.getOptions());
        }
        return ProductResponse.from(productJpaRepository.save(product));
    }

    public ProductResponse read(Long productId) {
        Product product = productJpaRepository.findByIdWithFetchJoin(productId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT));

        return ProductResponse.from(product);
    }

    public void delete(Long productId) {
        Product product = productJpaRepository.findByIdWithFetchJoin(productId)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_PRODUCT));

        productJpaRepository.delete(product);
    }
}
