package pointbatchservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pointbatchservice.entity.Point;

public interface PointJpaRepository extends JpaRepository<Point, Long> {
        @Query("SELECT p from Point p " +
            "LEFT JOIN FETCH p.pointBalance " +
            "WHERE p.userId = :userId " +
            "ORDER BY p.createdAt DESC")
    Page<Point> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);
}
