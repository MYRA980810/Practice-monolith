package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.domain.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCategoriesServiceTest {

    @Mock LoadCategoryPort loadCategoryPort;

    @InjectMocks ListCategoriesService service;

    @Test
    void listActive_delegatesToPort() {
        var c1 = Category.create("Electrónica", "electronica", null);
        var c2 = Category.create("Moda Femenina", "moda-femenina", null);
        when(loadCategoryPort.loadAllActive()).thenReturn(List.of(c1, c2));

        var result = service.listActive();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Electrónica");
    }
}
