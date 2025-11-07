package productservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import productservice.controller.dto.ProductSearchResponse;
import productservice.entity.Product;
import productservice.entity.ProductDocument;
import productservice.repository.ProductElasticsearchQuery;
import productservice.repository.ProductJpaRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductJpaRepository productJpaRepository;
    private final ProductElasticsearchQuery elasticsearchRepository;

    public List<ProductSearchResponse> searchProducts(String keyword, Long lastId, int pageSize) {

        List<Product> products = productJpaRepository.searchProducts(keyword, lastId, PageRequest.of(0, pageSize));

        return products.stream()
                .map(ProductSearchResponse::from)
                .toList();
    }

    public List<ProductSearchResponse> searchElasticsearchProducts(String keyword, Long lastId, int pageSize) {

        Page<ProductDocument> productDocuments = elasticsearchRepository.searchProducts(keyword, lastId, pageSize);

        return productDocuments.getContent().stream()
                .map(this::toResponse)
                .toList();
    }

    private ProductSearchResponse toResponse(ProductDocument document) {
        List<ProductSearchResponse.OptionInfo> options = document.getOptions() != null
                ? document.getOptions().stream()
                .map(option -> ProductSearchResponse.OptionInfo.builder()
                        .id(Long.valueOf(option.getId()))
                        .size(option.getSize())
                        .color(option.getColor())
                        .additionalPrice(option.getAdditionalPrice())
                        .stockQuantity(option.getAvailableQuantity())
                        .build())
                .toList()
                : List.of();

        return ProductSearchResponse.builder()
                .id(Long.valueOf(document.getId()))
                .name(document.getName())
                .price(document.getPrice())
                .image(document.getImage())
                .categoryName(document.getCategoryName())
                .options(options)
                .build();
    }
}
