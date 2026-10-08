package com.livecomerce.live.application;

import com.livecomerce.live.CategoryLookupPort;
import com.livecomerce.live.LiveCategoryChangedEvent;
import com.livecomerce.live.application.port.in.ChangeLiveCategoryUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLivePort;
import com.livecomerce.live.domain.CategoryNotAvailableException;
import com.livecomerce.live.domain.Live;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotOwnedBySellerException;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ChangeLiveCategoryService implements ChangeLiveCategoryUseCase {

    private final LoadLivePort       loadLivePort;
    private final SaveLivePort       saveLivePort;
    private final CategoryLookupPort categoryLookupPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public Live changeCategory(ChangeLiveCategoryCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        verifySeller(live, command.sellerId());
        if (!categoryLookupPort.isActive(command.categoryId())) {
            throw new CategoryNotAvailableException(command.categoryId());
        }
        live.changeCategory(command.categoryId());

        var saved = saveLivePort.save(live);
        if (saved.getStatus() == LiveStatus.LIVE) {
            // Only the active feed shows category; it refreshes the card and the chip counts.
            eventPublisher.publishEvent(new LiveCategoryChangedEvent(saved.getId(), saved.getCategoryId()));
        }
        return saved;
    }

    private void verifySeller(Live live, UUID sellerId) {
        if (!live.getSellerId().equals(sellerId)) {
            throw new LiveNotOwnedBySellerException(live.getId(), sellerId);
        }
    }
}
