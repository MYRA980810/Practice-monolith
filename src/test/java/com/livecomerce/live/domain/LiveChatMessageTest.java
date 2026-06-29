package com.livecomerce.live.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LiveChatMessageTest {

    @Test
    void create_assignsAllFields() {
        var live      = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "Live Test", null, null, 60);
        var agoraMsgId = "agora-msg-001";
        var userId    = UUID.randomUUID();
        var sentAt    = OffsetDateTime.now();

        var msg = LiveChatMessage.create(live, agoraMsgId, userId, "alice", "Hello world!", sentAt);

        assertThat(msg.getId()).isNotNull();
        assertThat(msg.getLive()).isEqualTo(live);
        assertThat(msg.getAgoraMsgId()).isEqualTo(agoraMsgId);
        assertThat(msg.getUserId()).isEqualTo(userId);
        assertThat(msg.getUsername()).isEqualTo("alice");
        assertThat(msg.getContent()).isEqualTo("Hello world!");
        assertThat(msg.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void create_generatesUniqueIds() {
        var live = Live.create(UUID.randomUUID(), UUID.randomUUID(), LiveContext.STORE, "Live Test", null, null, 60);
        var sentAt = OffsetDateTime.now();

        var msg1 = LiveChatMessage.create(live, "msg-001", UUID.randomUUID(), "alice", "Hi", sentAt);
        var msg2 = LiveChatMessage.create(live, "msg-002", UUID.randomUUID(), "bob", "Hey", sentAt);

        assertThat(msg1.getId()).isNotEqualTo(msg2.getId());
    }
}
