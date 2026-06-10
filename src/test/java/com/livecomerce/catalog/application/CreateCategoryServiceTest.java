package com.livecomerce.catalog.application;

import com.livecomerce.catalog.application.port.in.CreateCategoryUseCase.CreateCategoryCommand;
import com.livecomerce.catalog.application.port.out.LoadCategoryPort;
import com.livecomerce.catalog.application.port.out.SaveCategoryPort;
import com.livecomerce.catalog.domain.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCategoryServiceTest {

    @Mock LoadCategoryPort loadCategoryPort;
    @Mock SaveCategoryPort saveCategoryPort;

    @InjectMocks CreateCategoryService service;

    private static final UUID CREATOR_ID = UUID.randomUUID();

    @Test
    void create_savesAndReturnsCategory() {
        var command = new CreateCategoryCommand("Electrónica", "electronica", CREATOR_ID);
        when(loadCategoryPort.findBySlug("electronica")).thenReturn(Optional.empty());
        when(saveCategoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.create(command);

        assertThat(result.getName()).isEqualTo("Electrónica");
        assertThat(result.getSlug()).isEqualTo("electronica");
    }

    @Test
    void create_throwsOnDuplicateSlug() {
        var command = new CreateCategoryCommand("Electrónica", "existing-slug", CREATOR_ID);
        var existing = Category.create("Existing", "existing-slug", null);
        when(loadCategoryPort.findBySlug("existing-slug")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(CategorySlugAlreadyTakenException.class);
    }
}
