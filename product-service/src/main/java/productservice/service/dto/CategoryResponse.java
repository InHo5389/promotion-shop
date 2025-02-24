package productservice.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import productservice.entity.Category;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;

    public static CategoryResponse from(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.id = category.getId();
        response.name = category.getName();
        return response;
    }

    @Getter
    public static class Create{
        private Long id;
        private String name;

        public static Create from(Category category) {
            CategoryResponse.Create response = new CategoryResponse.Create();
            response.id = category.getId();
            response.name = category.getName();
            return response;
        }
    }
}
