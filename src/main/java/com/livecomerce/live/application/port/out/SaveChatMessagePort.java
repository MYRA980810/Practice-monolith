package com.livecomerce.live.application.port.out;

import com.livecomerce.live.domain.LiveChatMessage;

public interface SaveChatMessagePort {

    LiveChatMessage save(LiveChatMessage message);

    boolean existsByAgoraMsgId(String agoraMsgId);
}
