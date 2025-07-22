package orderservice.client;

import jakarta.validation.Valid;
import orderservice.client.dto.PointRequest;
import orderservice.client.dto.PointResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "point-service")
public interface PointClient {

    @PostMapping("/api/v2/points/use")
    PointResponse use(@Valid @RequestBody PointRequest.Use request);

    @PostMapping("/api/v2/points//earn")
    PointResponse earn(@Valid @RequestBody PointRequest.Earn request);
}
