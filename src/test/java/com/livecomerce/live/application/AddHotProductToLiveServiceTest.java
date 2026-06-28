package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.AddHotProductUseCase.AddHotProductCommand;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveLiveProductPort;
import com.livecomerce.live.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddHotProductToLiveServiceTest {

    @Mock LoadLivePort        loadLivePort;
    @Mock SaveLiveProductPort saveLiveProductPort;
    @InjectMocks AddHotProductToLiveService sut;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    @Test
    void addHotProduct_toScheduledLive_succeeds() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));
        when(saveLiveProductPort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var cmd = new AddHotProductCommand(
                live.getId(), SELLER_ID, "Mystery Box", new BigDecimal("99.00"), "MXN", 20, "https://example.com/image.jpg");
        var result = sut.addHotProduct(cmd);

        assertThat(result.isHot()).isTrue();
        assertThat(result.getProductId()).isNull();
        assertThat(result.getVariantId()).isNull();
        assertThat(result.getProductNameSnapshot()).isEqualTo("Mystery Box");
    }

    @Test
    void addHotProduct_zeroStock_throwsAtDomainLevel() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "Live", null, null, 60);
        when(loadLivePort.loadById(live.getId())).thenReturn(Optional.of(live));

        var cmd = new AddHotProductCommand(
                live.getId(), SELLER_ID, "Mystery Box", new BigDecimal("99.00"), "MXN", 0, "https://example.com/image.jpg");

        assertThatThrownBy(() -> sut.addHotProduct(cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
