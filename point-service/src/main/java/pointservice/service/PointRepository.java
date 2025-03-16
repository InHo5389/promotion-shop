package pointservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pointservice.entity.Point;

import java.util.Optional;

public interface PointRepository {
    Point save(Point point);
    Optional<Point> findById(Long pointId);
    Page<Point> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
