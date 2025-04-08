package pointservice.service.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pointservice.entity.Point;

@Service
@RequiredArgsConstructor
public class PointFacade {

    private final PointService pointService;

    public Point earn(Long userId, Long amount) throws InterruptedException {
        while (true){
            try {
                return pointService.earn(userId,amount);
            }catch (Exception e){
                Thread.sleep(20);
            }
        }
    }
}
