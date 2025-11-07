package productservice.repository;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Repository;
import productservice.entity.ProductDocument;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ProductElasticsearchQuery {

    private final ElasticsearchOperations elasticsearchOperations;

    public Page<ProductDocument> searchProducts(
            String keyword,
            Long lastId,
            int pageSize) {

        log.info("검색 시작 - keyword: {}, lastId: {}", keyword, lastId);

        BoolQuery.Builder boolQuery = new BoolQuery.Builder()
                .must(Query.of(q -> q.term(t -> t
                        .field("status")
                        .value("ACTIVE")
                )));

        if (keyword != null && !keyword.isBlank()) {
            boolQuery.must(Query.of(q -> q.match(m -> m
                    .field("name")
                    .query(keyword)
            )));
        }

        if (lastId != null) {
            boolQuery.must(Query.of(q -> q.range(r -> r
                    .field("id")
                    .lt(JsonData.of(String.valueOf(lastId)))
            )));
        }

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolQuery.build())))
                .withPageable(PageRequest.of(0, pageSize))
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(
                searchQuery,
                ProductDocument.class
        );

        log.info("검색 결과 - Total: {}, 실제: {}",
                searchHits.getTotalHits(),
                searchHits.getSearchHits().size());

        List<ProductDocument> products = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .toList();

        return new PageImpl<>(products, PageRequest.of(0, pageSize), searchHits.getTotalHits());
    }
}