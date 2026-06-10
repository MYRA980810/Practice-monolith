package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.port.in.CreateCategoryUseCase;
import com.livecomerce.catalog.application.port.in.ListCategoriesUseCase;
import com.livecomerce.catalog.domain.Category;
import com.livecomerce.shared.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;

    @GetMapping
    ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(listCategoriesUseCase.listActive().stream()
                .map(CategoryResponse::from)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    ResponseEntity<CategoryResponse> create(
            @RequestBody @Valid CreateCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Category category = createCategoryUseCase.create(
                new CreateCategoryUseCase.CreateCategoryCommand(
                        request.name(), request.slug(), principal.getUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CategoryResponse.from(category));
    }
}
