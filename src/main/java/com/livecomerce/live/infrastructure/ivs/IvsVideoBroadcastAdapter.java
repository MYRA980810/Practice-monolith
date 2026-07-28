package com.livecomerce.live.infrastructure.ivs;

import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ivs.IvsClient;
import software.amazon.awssdk.services.ivs.model.ChannelNotBroadcastingException;
import software.amazon.awssdk.services.ivs.model.CreateChannelRequest;
import software.amazon.awssdk.services.ivs.model.GetChannelRequest;
import software.amazon.awssdk.services.ivs.model.StopStreamRequest;

@Component
@RequiredArgsConstructor
public class IvsVideoBroadcastAdapter implements VideoBroadcastPort {

    private final IvsClient ivsClient;

    @Value("${ivs.channel-type:STANDARD}")
    private String channelType;

    @Value("${ivs.latency-mode:LOW}")
    private String latencyMode;

    @Override
    public ChannelHandle createChannel(String channelName) {
        var request = CreateChannelRequest.builder()
                .name(channelName)
                .latencyMode(latencyMode)
                .type(channelType)
                .authorized(false)
                .build();

        var response = ivsClient.createChannel(request);

        return new ChannelHandle(
                response.channel().arn(),
                response.channel().ingestEndpoint(),
                response.streamKey().arn(),
                response.streamKey().value(),
                response.channel().playbackUrl());
    }

    @Override
    public ChannelHandle getChannel(String channelArn) {
        var request = GetChannelRequest.builder()
                .arn(channelArn)
                .build();

        var channel = ivsClient.getChannel(request).channel();

        // AWS IVS never returns the stream key value again after creation, for security reasons
        return new ChannelHandle(
                channel.arn(),
                channel.ingestEndpoint(),
                null,
                null,
                channel.playbackUrl());
    }

    @Override
    public void stopStream(String channelArn) {
        try {
            ivsClient.stopStream(StopStreamRequest.builder().channelArn(channelArn).build());
        } catch (ChannelNotBroadcastingException e) {
            // stream already stopped/not broadcasting, nothing to do
        }
    }
}
