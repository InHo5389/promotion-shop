package pointservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import pointservice.entity.Point;
import pointservice.repository.PointJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository{

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    @Override
    public Optional<Point> findById(Long pointId) {
        return pointJpaRepository.findById(pointId);
    }

    @Override
    public Page<Point> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable) {
        return pointJpaRepository.findByUserIdOrderByCreatedAtDesc(userId,pageable);
    }
}
