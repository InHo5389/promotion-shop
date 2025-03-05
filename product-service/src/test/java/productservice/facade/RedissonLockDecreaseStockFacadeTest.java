package productservice.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import productservice.entity.Category;
import productservice.entity.Product;
import productservice.entity.ProductOption;
import productservice.entity.ProductStock;
import productservice.repository.CategoryJpaRepository;
import productservice.repository.ProductJpaRepository;
import productservice.repository.ProductOptionJpaRepository;
import productservice.repository.ProductStockJpaRepository;
import productservice.service.dto.ProductOptionRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RedissonLockDecreaseStockFacadeTest {

    @Autowired
    private RedissonLockDecreaseStockFacade redissonLockDecreaseStockFacade;

    @Autowired
    private RedissonLockIncreaseFacade redissonLockIncreaseStockFacade;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private ProductOptionJpaRepository productOptionJpaRepository;

    @Autowired
    private ProductStockJpaRepository productStockJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    private Long productId;
    private Long productOptionId;
    private final int initialStock = 100;

    @BeforeEach
    void setUp() {
        // 테스트용 상품 및 카테고리 생성
        Category category = new Category();
        categoryJpaRepository.save(category);

        Product product = Product.create(category, "테스트 상품", BigDecimal.valueOf(10000), "test.jpg");
        productJpaRepository.save(product);

        ProductOption productOption = ProductOption.create(product, "L", "Red", BigDecimal.ZERO);
        productOptionJpaRepository.save(productOption);

        ProductStock productStock = ProductStock.create(productOption, initialStock);
        productStockJpaRepository.save(productStock);

        productId = product.getId();
        productOptionId = productOption.getId();
    }

    @Test
    @DisplayName("100명이 동시에 재고가 100개인 상품을 1개씩 주문하면 재고가 0개가 되어야 한다")
    void decreaseStockConcurrentlyTest() throws InterruptedException {
        int threadCount = 100;
        int decreaseQuantity = 1;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockDecreaseStockFacade.decreaseStock(
                            Collections.singletonList(new ProductOptionRequest.StockUpdate(productId, productOptionId, decreaseQuantity))
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 최종 재고 확인
        ProductStock finalStock = productStockJpaRepository.findByProductOptionId(productOptionId).get();
        assertThat(finalStock.getQuantity()).isEqualTo(initialStock - (threadCount * decreaseQuantity));
    }

    @Test
    @DisplayName("100명이 동시에 재고가 100개인 상품에 1개씩 재고를 추가하면 재고가 200개가 되어야 한다")
    void increaseStockConcurrentlyTest() throws InterruptedException {
        int threadCount = 100;
        int increaseQuantity = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockIncreaseStockFacade.increaseStock(
                            Collections.singletonList(new ProductOptionRequest.StockUpdate(productId, productOptionId, increaseQuantity))
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        // 최종 재고 확인
        ProductStock finalStock = productStockJpaRepository.findByProductOptionId(productOptionId).get();
        assertThat(finalStock.getQuantity()).isEqualTo(initialStock + (threadCount * increaseQuantity));
    }
}