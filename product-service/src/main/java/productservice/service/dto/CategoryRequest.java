package productservice.service.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CategoryRequest {

    @Size(min = 2, max = 10, message = "카테고리명은 2자 이상 10자 이하여야 합니다.")
    private String name;
}
