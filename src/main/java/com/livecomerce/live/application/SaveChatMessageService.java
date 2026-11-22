package com.livecomerce.live.application;

import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import com.livecomerce.live.application.port.out.LoadLivePort;
import com.livecomerce.live.application.port.out.SaveChatMessagePort;
import com.livecomerce.live.domain.LiveChatMessage;
import com.livecomerce.live.domain.LiveNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveChatMessageService implements SaveChatMessageUseCase {

    private final LoadLivePort        loadLivePort;
    private final SaveChatMessagePort saveChatMessagePort;

    @Override
    @Transactional
    public void saveChatMessage(SaveChatMessageCommand command) {
        // Idempotency: duplicate provider message ids (webhook retries, SQS redelivery) are ignored.
        // existsBy is only a fast path; saveIfAbsent is what makes concurrent duplicates race-safe.
        if (saveChatMessagePort.existsByProviderMsgId(command.providerMsgId())) {
            return;
        }

        var live = loadLivePort.loadById(command.liveId())
                .orElseThrow(() -> new LiveNotFoundException(command.liveId()));

        var message = LiveChatMessage.create(
                live,
                command.providerMsgId(),
                command.userId(),
                command.username(),
                command.content(),
                command.sentAt()
        );

        saveChatMessagePort.saveIfAbsent(message);
    }
}
