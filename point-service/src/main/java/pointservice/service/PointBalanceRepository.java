package pointservice.service;

import pointservice.entity.PointBalance;

import java.util.Optional;

public interface PointBalanceRepository {

    Optional<PointBalance> findByUserId(Long userId);

    PointBalance save(PointBalance pointBalance);
}
