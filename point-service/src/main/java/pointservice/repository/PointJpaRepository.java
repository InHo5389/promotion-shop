package pointservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pointservice.entity.Point;
import pointservice.entity.PointType;

import java.util.List;

public interface PointJpaRepository extends JpaRepository<Point, Long> {
        @Query("SELECT p from Point p " +
            "LEFT JOIN FETCH p.pointBalance " +
            "WHERE p.userId = :userId " +
            "ORDER BY p.createdAt DESC")
    Page<Point> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    List<Point> findByOrderIdAndTypeAndUserId(Long orderId, PointType pointType, Long userId);
}
