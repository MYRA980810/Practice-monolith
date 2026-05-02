package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.CreateStoreUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import com.livecomerce.store.domain.StoreCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateStoreService implements CreateStoreUseCase {

    private final LoadStorePort loadStorePort;
    private final SaveStorePort saveStorePort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Store create(CreateStoreCommand command) {
        if (loadStorePort.loadByUserId(command.userId()).isPresent()) {
            throw new StoreAlreadyExistsException(command.userId());
        }
        if (loadStorePort.loadBySlug(command.slug()).isPresent()) {
            throw new SlugAlreadyTakenException(command.slug());
        }

        var store = Store.create(command.userId(), command.name(), command.slug(), command.description());
        var saved = saveStorePort.save(store);

        eventPublisher.publishEvent(new StoreCreatedEvent(saved.getId(), saved.getUserId(), saved.getSlug()));

        return saved;
    }
}
