package pointservice.service.v2;

public interface PointRedisRepository {

    Long getBalanceFormCache(Long userId);
    void updateBalanceCache(Long userId,Long balance);
}
