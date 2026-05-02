package com.livecomerce.store.application;

import com.livecomerce.store.application.port.in.ChangePlanUseCase;
import com.livecomerce.store.application.port.out.LoadStorePort;
import com.livecomerce.store.application.port.out.SaveStorePort;
import com.livecomerce.store.domain.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangePlanService implements ChangePlanUseCase {

    private final LoadStorePort loadStorePort;
    private final SaveStorePort saveStorePort;

    @Override
    public Store change(ChangePlanCommand command) {
        Store store = loadStorePort.loadByUserId(command.userId())
                .orElseThrow(() -> new StoreNotFoundException(command.userId().toString()));

        store.changePlan(command.plan());

        return saveStorePort.save(store);
    }
}
