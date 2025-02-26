package productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import productservice.entity.ProductStock;

public interface ProductStockJpaRepository extends JpaRepository<ProductStock, Long> {
}
