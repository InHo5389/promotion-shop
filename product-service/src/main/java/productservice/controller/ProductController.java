package productservice.controller;

import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
//import productservice.facade.RedissonLockDecreaseStockFacade;
import productservice.controller.dto.ProductSearchResponse;
import productservice.facade.RedissonLockIncreaseFacade;
import productservice.service.ProductSearchService;
import productservice.service.ProductService;
import productservice.service.dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    //    private final RedissonLockDecreaseStockFacade redissonLockDecreaseStockFacade;
    private final RedissonLockIncreaseFacade redissonLockIncreaseFacade;
    private final ProductSearchService productSearchService;

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

    @PostMapping("/reserve")
    public void reserveStock(@RequestBody StockReserveRequest request) {
        productService.reserveStock(request);
    }

    @PostMapping("/confirm/{orderId}")
    public void confirmReservation(@PathVariable Long orderId) {
        productService.confirmReservation(orderId);
    }

    @PostMapping("/cancel/{orderId}")
    public void cancelReservation(@PathVariable Long orderId) {
        productService.cancelReservation(orderId);
    }

    @PostMapping("/rollback/{orderId}")
    public void rollback(@PathVariable Long orderId) {
        productService.rollbackConfirmation(orderId);
    }

    @PostMapping("/stock/increase")
    public void increaseStock(@RequestBody List<ProductOptionRequest.StockUpdate> requests) {
        redissonLockIncreaseFacade.increaseStock(requests);
    }

    @PostMapping("/batch")
    List<ProductResponse> getProducts(@RequestBody ProductRequest.ReadProductIds request) {
        return productService.getProductByIds(request.getProductIds());
    }

    @GetMapping("/search")
    public List<ProductSearchResponse> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int pageSize) {

        return productSearchService.searchProducts(keyword, lastId, pageSize);
    }

    @GetMapping("/es/search")
    public List<ProductSearchResponse> searchElasticProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") int pageSize) {

        return productSearchService.searchElasticsearchProducts(keyword, lastId, pageSize);
    }
}
