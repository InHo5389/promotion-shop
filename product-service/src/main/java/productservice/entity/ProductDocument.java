package productservice.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
@Setting(settingPath = "elasticsearch/product-settings.json")
@Mapping(mappingPath = "elasticsearch/product-mappings.json")
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String name;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)
    private String image;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Text, analyzer = "nori")
    private String categoryName;

    @Field(type = FieldType.Nested)
    private List<ProductOptionDocument> options;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductOptionDocument {
        @Field(type = FieldType.Long)
        private String id;

        @Field(type = FieldType.Keyword)
        private String size;

        @Field(type = FieldType.Keyword)
        private String color;

        @Field(type = FieldType.Double)
        private BigDecimal additionalPrice;

        @Field(type = FieldType.Integer)
        private Integer availableQuantity;
    }

    public static ProductDocument from(Product product) {
        List<ProductOptionDocument> options = product.getOptions().stream()
                .map(option -> ProductOptionDocument.builder()
                        .id(option.getId().toString())
                        .size(option.getSize())
                        .color(option.getColor())
                        .additionalPrice(option.getAdditionalPrice())
                        .availableQuantity(option.getStock() != null ?
                                option.getStock().getQuantity() - option.getStock().getReservedQuantity() : 0)
                        .build())
                .toList();

        return ProductDocument.builder()
                .id(String.valueOf(product.getId()))
                .name(product.getName())
                .price(product.getPrice())
                .image(product.getImage())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .options(options)
                .status(product.getStatus().name())
                .createdAt(product.getCreatedAt())
                .build();
    }
}