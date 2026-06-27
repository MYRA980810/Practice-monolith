package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.CreateLiveUseCase;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateLiveService implements CreateLiveUseCase {

    private final SaveLivePort saveLivePort;

    @Override
    public Live createLive(CreateLiveCommand command) {
        if (command.context() == LiveContext.STORE && command.storeId() == null) {
            throw new IllegalArgumentException(
                    "storeId must not be null when context is STORE");
        }

        var live = Live.create(
                command.sellerId(),
                command.storeId(),
                command.context(),
                command.title(),
                command.thumbnailUrl(),
                command.scheduledAt(),
                command.displayDurationSeconds());

        return saveLivePort.save(live);
    }
}
