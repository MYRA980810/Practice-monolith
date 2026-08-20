package com.livecomerce.live.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "lives")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Live implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "store_id")
    private UUID storeId;

    @Convert(converter = com.livecomerce.live.infrastructure.persistence.LiveContextConverter.class)
    @Column(name = "live_context", nullable = false, length = 20)
    private LiveContext context;

    @Column(nullable = false, length = 255)
    private String title;

    @Convert(converter = com.livecomerce.live.infrastructure.persistence.LiveStatusConverter.class)
    @Column(nullable = false, length = 10)
    private LiveStatus status;

    @Column(name = "agora_channel_id", unique = true, length = 255)
    private String agoraChannelId;

    @Column(name = "stream_token", columnDefinition = "TEXT")
    private String streamToken;

    @Column(name = "ivs_channel_arn", unique = true, length = 255)
    private String ivsChannelArn;

    @Column(name = "ivs_ingest_endpoint", length = 255)
    private String ivsIngestEndpoint;

    @Column(name = "ivs_stream_key_arn", length = 255)
    private String ivsStreamKeyArn;

    @Column(name = "ivs_stream_key_value", columnDefinition = "TEXT")
    private String ivsStreamKeyValue;

    @Column(name = "ivs_playback_url", length = 500)
    private String ivsPlaybackUrl;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "peak_viewers", nullable = false)
    private int peakViewers = 0;

    @Column(name = "display_duration_seconds", nullable = false)
    private int displayDurationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public static Live create(UUID sellerId, @Nullable UUID storeId, LiveContext context,
                              String title, @Nullable String thumbnailUrl, @Nullable Instant scheduledAt,
                              int displayDurationSeconds) {
        var live = new Live();
        live.id                      = UUID.randomUUID();
        live.isNew                   = true;
        live.sellerId                = sellerId;
        live.storeId                 = storeId;
        live.context                 = context;
        live.title                   = title;
        live.status                  = LiveStatus.SCHEDULED;
        live.agoraChannelId          = UUID.randomUUID().toString();
        live.thumbnailUrl            = thumbnailUrl;
        live.scheduledAt             = scheduledAt;
        live.displayDurationSeconds  = displayDurationSeconds;
        live.createdAt               = OffsetDateTime.now();
        live.updatedAt               = OffsetDateTime.now();
        return live;
    }

    public void start() {
        if (this.status != LiveStatus.SCHEDULED) {
            throw new InvalidLiveStateException(
                    "Cannot start live in status: " + this.status);
        }
        this.status    = LiveStatus.LIVE;
        this.startedAt = Instant.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void end() {
        if (this.status != LiveStatus.LIVE) {
            throw new InvalidLiveStateException(
                    "Cannot end live in status: " + this.status);
        }
        this.status    = LiveStatus.ENDED;
        this.endedAt   = Instant.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void cancel() {
        if (this.status == LiveStatus.ENDED || this.status == LiveStatus.CANCELLED) {
            throw new InvalidLiveStateException(
                    "Cannot cancel live in status: " + this.status);
        }
        this.status    = LiveStatus.CANCELLED;
        this.updatedAt = OffsetDateTime.now();
    }

    public void setStreamToken(String token) {
        this.streamToken = token;
        this.updatedAt   = OffsetDateTime.now();
    }

    public void setIvsChannel(String channelArn, String ingestEndpoint, String streamKeyArn,
                               String streamKeyValue, String playbackUrl) {
        this.ivsChannelArn      = channelArn;
        this.ivsIngestEndpoint  = ingestEndpoint;
        this.ivsStreamKeyArn    = streamKeyArn;
        this.ivsStreamKeyValue  = streamKeyValue;
        this.ivsPlaybackUrl     = playbackUrl;
        this.updatedAt          = OffsetDateTime.now();
    }

    public void updatePeakViewers(int currentCount) {
        if (currentCount > this.peakViewers) {
            this.peakViewers = currentCount;
            this.updatedAt   = OffsetDateTime.now();
        }
    }
}
