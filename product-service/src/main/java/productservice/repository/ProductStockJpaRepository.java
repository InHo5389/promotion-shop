package productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import productservice.entity.ProductStock;

import java.util.Optional;

public interface ProductStockJpaRepository extends JpaRepository<ProductStock, Long> {
    Optional<ProductStock> findByProductOptionId(Long productOptionId);
}
