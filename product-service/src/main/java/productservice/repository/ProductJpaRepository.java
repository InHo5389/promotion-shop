package productservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productservice.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    // 쿼리 3방을 1방으로 줄임.
    @Query("SELECT p FROM Product p " +
            "JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.options o " +
            "LEFT JOIN FETCH o.stock " +
            "WHERE p.id = :productId")
    Optional<Product> findByIdWithFetchJoin(@Param("productId") Long productId);

    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.options o " +
            "LEFT JOIN FETCH o.stock " +
            "WHERE p.id IN :ids")
    List<Product> findAllWithCategoryOptionsAndStockByIdIn(@Param("ids") List<Long> ids);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = 'ACTIVE'
            AND (:keyword IS NULL OR p.name LIKE CONCAT('%', :keyword, '%'))
            AND (:lastId IS NULL OR p.id < :lastId)
            ORDER BY p.id ASC
            """)
    List<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("lastId") Long lastId,
            Pageable pageable
    );
}
