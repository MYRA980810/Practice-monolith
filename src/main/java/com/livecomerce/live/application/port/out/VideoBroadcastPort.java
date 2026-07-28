package com.livecomerce.live.application.port.out;

public interface VideoBroadcastPort {

    ChannelHandle createChannel(String channelName);

    ChannelHandle getChannel(String channelArn);

    void stopStream(String channelArn);

    record ChannelHandle(String channelArn, String ingestEndpoint,
                          String streamKeyArn, String streamKeyValue, String playbackUrl) {}
}
