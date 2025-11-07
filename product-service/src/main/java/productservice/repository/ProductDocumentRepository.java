package productservice.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import productservice.entity.ProductDocument;

public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {
}
