package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.GetChatHistoryUseCase;
import com.livecomerce.live.application.port.out.LoadChatMessagePort;
import com.livecomerce.live.domain.LiveChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetChatHistoryService implements GetChatHistoryUseCase {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT     = 100;

    private final LoadChatMessagePort loadChatMessagePort;

    @Override
    public List<LiveChatMessage> getChatHistory(GetChatHistoryCommand command) {
        int limit = clamp(command.limit());

        if (command.before() == null) {
            return loadChatMessagePort.findByLiveIdLatest(command.liveId(), limit);
        }
        return loadChatMessagePort.findByLiveIdBefore(command.liveId(), command.before(), limit);
    }

    private int clamp(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}
