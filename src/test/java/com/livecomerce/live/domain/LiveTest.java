package com.livecomerce.live.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class LiveTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID STORE_ID  = UUID.randomUUID();

    // ── Factory ──────────────────────────────────────────────────────────────

    @Test
    void create_storeContext_setsCorrectFields() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);

        assertThat(live.getId()).isNotNull();
        assertThat(live.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(live.getStoreId()).isEqualTo(STORE_ID);
        assertThat(live.getContext()).isEqualTo(LiveContext.STORE);
        assertThat(live.getTitle()).isEqualTo("My Live");
        assertThat(live.getStatus()).isEqualTo(LiveStatus.SCHEDULED);
        assertThat(live.getAgoraChannelId()).isNotBlank();
    }

    @Test
    void create_sellerProfileContext_allowsNullStore() {
        var live = Live.create(SELLER_ID, null, LiveContext.SELLER_PROFILE, "Profile Live", null, null, 60);

        assertThat(live.getStoreId()).isNull();
        assertThat(live.getContext()).isEqualTo(LiveContext.SELLER_PROFILE);
        assertThat(live.getStatus()).isEqualTo(LiveStatus.SCHEDULED);
    }

    @Test
    void create_withOptionalFields_setsThemCorrectly() {
        var thumbnail  = "https://cdn.example.com/thumb.jpg";
        var scheduledAt = Instant.now().plusSeconds(3600);
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", thumbnail, scheduledAt, 60);

        assertThat(live.getThumbnailUrl()).isEqualTo(thumbnail);
        assertThat(live.getScheduledAt()).isEqualTo(scheduledAt);
    }

    // ── State Machine ─────────────────────────────────────────────────────────

    @Test
    void start_fromScheduled_transitionsToLive() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);

        live.start();

        assertThat(live.getStatus()).isEqualTo(LiveStatus.LIVE);
        assertThat(live.getStartedAt()).isNotNull();
    }

    @Test
    void start_fromLive_throwsInvalidLiveStateException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();

        assertThatThrownBy(live::start)
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    void start_fromEnded_throwsInvalidLiveStateException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        live.end();

        assertThatThrownBy(live::start)
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    void end_fromLive_transitionsToEnded() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();

        live.end();

        assertThat(live.getStatus()).isEqualTo(LiveStatus.ENDED);
        assertThat(live.getEndedAt()).isNotNull();
    }

    @Test
    void end_fromScheduled_throwsInvalidLiveStateException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);

        assertThatThrownBy(live::end)
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    void cancel_fromScheduled_transitionsToCancelled() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);

        live.cancel();

        assertThat(live.getStatus()).isEqualTo(LiveStatus.CANCELLED);
    }

    @Test
    void cancel_fromLive_transitionsToCancelled() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();

        live.cancel();

        assertThat(live.getStatus()).isEqualTo(LiveStatus.CANCELLED);
    }

    @Test
    void cancel_fromEnded_throwsInvalidLiveStateException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.start();
        live.end();

        assertThatThrownBy(live::cancel)
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    void cancel_fromCancelled_throwsInvalidLiveStateException() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.cancel();

        assertThatThrownBy(live::cancel)
                .isInstanceOf(InvalidLiveStateException.class);
    }

    @Test
    void setStreamToken_setsToken() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);

        live.setStreamToken("agora-rtc-token-xyz");

        assertThat(live.getStreamToken()).isEqualTo("agora-rtc-token-xyz");
    }

    // ── Peak Viewers ──────────────────────────────────────────────────────────

    @Test
    void updatePeakViewers_whenCurrentExceedsPeak_updatesPeak() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);

        live.updatePeakViewers(50);

        assertThat(live.getPeakViewers()).isEqualTo(50);
    }

    @Test
    void updatePeakViewers_whenCurrentLessThanPeak_doesNotDecrease() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.updatePeakViewers(50);

        live.updatePeakViewers(30);

        assertThat(live.getPeakViewers()).isEqualTo(50);
    }

    @Test
    void updatePeakViewers_whenCurrentExceedsExistingPeak_updatesToPeak() {
        var live = Live.create(SELLER_ID, STORE_ID, LiveContext.STORE, "My Live", null, null, 60);
        live.updatePeakViewers(50);

        live.updatePeakViewers(60);

        assertThat(live.getPeakViewers()).isEqualTo(60);
    }
}
