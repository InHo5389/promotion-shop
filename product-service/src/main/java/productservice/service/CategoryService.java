package productservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import productservice.common.exception.CustomGlobalException;
import productservice.common.exception.ErrorType;
import productservice.entity.Category;
import productservice.repository.CategoryJpaRepository;
import productservice.service.dto.CategoryResponse;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryJpaRepository categoryJpaRepository;

    @Transactional
    public CategoryResponse.Create create(String name) {
        log.info("카테고리 생성 요청 - 이름: {}", name);
        Category category = Category.create(name);
        Category savedCategory = categoryJpaRepository.save(category);
        log.info("카테고리 생성 완료 - ID: {}, 이름: {}", savedCategory.getId(), savedCategory.getName());
        return CategoryResponse.Create.from(savedCategory);
    }

    @Transactional
    public List<CategoryResponse> readAll() {
        log.info("전체 카테고리 조회 요청");
        List<CategoryResponse> responses = categoryJpaRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
        return responses;
    }

    @Transactional
    public CategoryResponse update(Long id, String name) {
        log.info("카테고리 수정 요청 - ID: {}, 새 이름: {}", id, name);
        Category category = categoryJpaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("카테고리 수정 실패 - 카테고리 없음 - ID: {}", id);
                    return new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY);
                });
        String oldName = category.getName();

        category.update(name);
        log.info("카테고리 수정 완료 - ID: {}, 이전 이름: {}, 새 이름: {}", id, oldName, name);
        return CategoryResponse.from(categoryJpaRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        log.info("카테고리 삭제 요청 - ID: {}", id);
        Category category = categoryJpaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("카테고리 삭제 실패 - 카테고리 없음 - ID: {}", id);
                    return new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY);
                });

        categoryJpaRepository.delete(category);
        log.info("카테고리 삭제 완료 - ID: {}, 이름: {}", id, category.getName());
    }
}
