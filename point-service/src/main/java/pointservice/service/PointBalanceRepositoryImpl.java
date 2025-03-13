package pointservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pointservice.entity.PointBalance;
import pointservice.repository.PointBalanceJpaRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointBalanceRepositoryImpl implements PointBalanceRepository{

    private final PointBalanceJpaRepository pointBalanceJpaRepository;

    @Override
    public Optional<PointBalance> findByUserId(Long userId) {
        return pointBalanceJpaRepository.findByUserId(userId);
    }

    @Override
    public PointBalance save(PointBalance pointBalance) {
        return pointBalanceJpaRepository.save(pointBalance);
    }
}
