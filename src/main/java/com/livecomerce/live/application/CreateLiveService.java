package com.livecomerce.live.application;

import com.livecomerce.live.CategoryLookupPort;
import com.livecomerce.live.application.port.in.CreateLiveUseCase;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.CategoryNotAvailableException;
import com.livecomerce.live.domain.Live;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateLiveService implements CreateLiveUseCase {

    private final SaveLivePort saveLivePort;
    private final CategoryLookupPort categoryLookupPort;

    @Override
    public Live createLive(CreateLiveCommand command) {
        if (!categoryLookupPort.isActive(command.categoryId())) {
            throw new CategoryNotAvailableException(command.categoryId());
        }

        var live = Live.create(
                command.sellerId(),
                command.title(),
                command.thumbnailUrl(),
                command.scheduledAt(),
                command.displayDurationSeconds(),
                command.categoryId());

        return saveLivePort.save(live);
    }
}
