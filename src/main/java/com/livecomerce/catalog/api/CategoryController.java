package com.livecomerce.catalog.api;

import com.livecomerce.catalog.application.port.in.ListCategoriesUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
class CategoryController {

    private final ListCategoriesUseCase listCategoriesUseCase;

    @GetMapping
    ResponseEntity<List<CategoryResponse>> list() {
        return ResponseEntity.ok(listCategoriesUseCase.listActive().stream()
                .map(CategoryResponse::from)
                .toList());
    }
}
