package com.livecomerce.live.domain;

import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seller_ivs_channels")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerIvsChannel implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "channel_arn", nullable = false, length = 255)
    private String channelArn;

    @Column(name = "ingest_endpoint", length = 255)
    private String ingestEndpoint;

    @Column(name = "stream_key_arn", length = 255)
    private String streamKeyArn;

    @Column(name = "stream_key_value", columnDefinition = "TEXT")
    private String streamKeyValue;

    @Column(name = "playback_url", length = 500)
    private String playbackUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

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

    public static SellerIvsChannel create(UUID sellerId, VideoBroadcastPort.ChannelHandle handle) {
        var channel = new SellerIvsChannel();
        channel.id             = UUID.randomUUID();
        channel.isNew          = true;
        channel.sellerId       = sellerId;
        channel.channelArn     = handle.channelArn();
        channel.ingestEndpoint = handle.ingestEndpoint();
        channel.streamKeyArn   = handle.streamKeyArn();
        channel.streamKeyValue = handle.streamKeyValue();
        channel.playbackUrl    = handle.playbackUrl();
        channel.createdAt      = OffsetDateTime.now();
        channel.updatedAt      = OffsetDateTime.now();
        return channel;
    }
}
