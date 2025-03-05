package productservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import productservice.entity.ProductOption;

import java.util.List;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOption, Long> {
    List<ProductOption> findAllByIdIn(List<Long> optionIds);
    @Query("SELECT o FROM ProductOption o LEFT JOIN FETCH o.stock WHERE o.id IN :ids")
    List<ProductOption> findAllWithStockByIdIn(@Param("ids") List<Long> ids);
}
