package productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import productservice.entity.ProductOption;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOption, Long> {
}
