package com.livecomerce.live.domain;

import com.livecomerce.live.application.port.out.VideoBroadcastPort.ChannelHandle;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SellerIvsChannelTest {

    private static final UUID SELLER_ID = UUID.randomUUID();

    private SellerIvsChannel buildChannel() {
        var handle = new ChannelHandle("arn:ivs:channel", "rtmps://ingest", "arn:ivs:key",
                "sk_stream_key", "https://playback.url");
        return SellerIvsChannel.create(SELLER_ID, handle);
    }

    @Test
    void create_setsFieldsCorrectly() {
        var channel = buildChannel();

        assertThat(channel.getId()).isNotNull();
        assertThat(channel.isNew()).isTrue();
        assertThat(channel.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(channel.getChannelArn()).isEqualTo("arn:ivs:channel");
        assertThat(channel.getIngestEndpoint()).isEqualTo("rtmps://ingest");
        assertThat(channel.getStreamKeyArn()).isEqualTo("arn:ivs:key");
        assertThat(channel.getStreamKeyValue()).isEqualTo("sk_stream_key");
        assertThat(channel.getPlaybackUrl()).isEqualTo("https://playback.url");
        assertThat(channel.getCreatedAt()).isNotNull();
        assertThat(channel.getUpdatedAt()).isNotNull();
    }
}
