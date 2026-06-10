package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.ListCategoriesUseCase;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.domain.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListCategoriesService implements ListCategoriesUseCase {

    private final LoadCategoryPort loadCategoryPort;

    @Override
    public List<Category> listActive() {
        return loadCategoryPort.loadAllActive();
    }
}
