package productservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import productservice.facade.RedissonLockDecreaseStockFacade;
import productservice.facade.RedissonLockIncreaseFacade;
import productservice.service.ProductService;
import productservice.service.dto.ProductOptionRequest;
import productservice.service.dto.ProductRequest;
import productservice.service.dto.ProductResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final RedissonLockDecreaseStockFacade redissonLockDecreaseStockFacade;
    private final RedissonLockIncreaseFacade redissonLockIncreaseFacade;

    @PostMapping
    public ProductResponse create(@Valid @RequestBody ProductRequest.Create request) {
        return productService.create(request);
    }

    @GetMapping("/{id}")
    public ProductResponse read(@PathVariable Long id) {
        return productService.read(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @RequestBody ProductRequest.Update request
    ) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @PostMapping("/stock/decrease")
    public void decreaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests) {
        redissonLockDecreaseStockFacade.decreaseStock(requests);
    }

    @PostMapping("/stock/increase")
    public void increaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests) {
        redissonLockIncreaseFacade.increaseStock(requests);
    }

    @PostMapping("/batch")
    List<ProductResponse> getProducts(@RequestBody ProductRequest.ReadProductIds request) {
        return productService.getProductByIds(request.getProductIds());
    }
}
