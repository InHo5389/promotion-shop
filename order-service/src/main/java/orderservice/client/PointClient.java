package orderservice.client;

import jakarta.validation.Valid;
import orderservice.client.dto.PointRequest;
import orderservice.client.dto.PointReserveRequest;
import orderservice.client.dto.PointResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "point-service")
public interface PointClient {

    @PostMapping("/api/v2/points/use")
    PointResponse use(@Valid @RequestBody PointRequest.Use request);

    @PostMapping("/api/v2/points/earn")
    PointResponse earn(@Valid @RequestBody PointRequest.Earn request);

    @PostMapping("/api/v2/points/reserve")
    ResponseEntity<Void> reservePoints(@RequestBody PointReserveRequest request);

    @PostMapping("/api/v2/points/confirm/{orderId}")
    ResponseEntity<Void> confirmPoints(@PathVariable Long orderId);

    @PostMapping("/api/v2/points/cancel/{orderId}")
    ResponseEntity<Void> cancelReservation(@PathVariable Long orderId);

    @PostMapping("/api/v2/points/rollback/confirm/{orderId}")
    ResponseEntity<Void> rollbackConfirmPoints(@PathVariable Long orderId);

    @PostMapping("/api/v2/points/rollback/reserve/{orderId}")
    ResponseEntity<Void> rollbackReservePoints(@PathVariable Long orderId);
}
