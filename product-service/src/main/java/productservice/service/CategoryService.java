package productservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import productservice.common.exception.CustomGlobalException;
import productservice.common.exception.ErrorType;
import productservice.entity.Category;
import productservice.repository.CategoryJpaRepository;
import productservice.service.dto.CategoryResponse;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryJpaRepository categoryJpaRepository;

    @Transactional
    public CategoryResponse.Create create(String name){
        Category category = Category.create(name);

        return CategoryResponse.Create.from(categoryJpaRepository.save(category));
    }

    @Transactional
    public List<CategoryResponse> readAll(){
        List<CategoryResponse> responses = categoryJpaRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
        return responses;
    }

    @Transactional
    public CategoryResponse update(Long id, String name) {
        Category category = categoryJpaRepository.findById(id)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY));
        category.update(name);
        return CategoryResponse.from(categoryJpaRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        Category category = categoryJpaRepository.findById(id)
                .orElseThrow(() -> new CustomGlobalException(ErrorType.NOT_FOUND_CATEGORY));
        categoryJpaRepository.delete(category);
    }
}
