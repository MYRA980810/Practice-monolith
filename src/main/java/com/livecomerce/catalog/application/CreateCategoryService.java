package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateCategoryUseCase;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.SaveCategoryPort;
import com.livecomerce.catalog.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateCategoryService implements CreateCategoryUseCase {

    private final LoadCategoryPort loadCategoryPort;
    private final SaveCategoryPort saveCategoryPort;

    @Override
    public Category create(CreateCategoryCommand command) {
        loadCategoryPort.findBySlug(command.slug())
                .ifPresent(c -> { throw new CategorySlugAlreadyTakenException(command.slug()); });
        return saveCategoryPort.save(Category.create(command.name(), command.slug(), command.createdBy()));
    }
}
