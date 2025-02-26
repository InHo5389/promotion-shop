package productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productservice.entity.Product;

import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    // 쿼리 3방을 1방으로 줄임.
    @Query("SELECT p FROM Product p " +
            "JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.options o " +
            "LEFT JOIN FETCH o.stock " +
            "WHERE p.id = :productId")
    Optional<Product> findByIdWithFetchJoin(@Param("productId") Long productId);
}
