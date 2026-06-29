package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.GetChatTokenUseCase;
import com.livecomerce.live.application.port.out.AgoraTokenPort;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.domain.LiveNotFoundException;
import com.livecomerce.live.domain.LiveNotLiveException;
import com.livecomerce.live.domain.LiveStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatTokenService implements GetChatTokenUseCase {

    private static final int TOKEN_TTL_SECONDS = 3600;

    private final LoadLivePort   loadLivePort;
    private final AgoraTokenPort agoraTokenPort;

    @Override
    public ChatTokenResult getChatToken(GetChatTokenCommand command) {
        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        if (live.getStatus() != LiveStatus.LIVE) {
            throw new LiveNotLiveException(live.getId());
        }

        var channelName = "live-chat:" + command.liveId();
        var token       = agoraTokenPort.generateRtmToken(command.userId().toString(), TOKEN_TTL_SECONDS);
        var appId       = agoraTokenPort.getAppId();

        return new ChatTokenResult(token, channelName, appId);
    }
}
