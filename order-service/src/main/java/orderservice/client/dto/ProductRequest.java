package orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

public class ProductRequest {

    @Getter
    @AllArgsConstructor
    public static class ReadProductIds{
        private List<Long> productIds;
    }
}
