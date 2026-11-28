package com.livecomerce.live.domain;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LiveChatMessageTest {

    @Test
    void create_assignsAllFields() {
        var live      = Live.create(UUID.randomUUID(), "Live Test", null, null, 60);
        var providerMsgId = "provider-msg-001";
        var userId    = UUID.randomUUID();
        var sentAt    = OffsetDateTime.now();

        var msg = LiveChatMessage.create(live, providerMsgId, userId, "alice", "Hello world!", sentAt);

        assertThat(msg.getId()).isNotNull();
        assertThat(msg.getLive()).isEqualTo(live);
        assertThat(msg.getProviderMsgId()).isEqualTo(providerMsgId);
        assertThat(msg.getUserId()).isEqualTo(userId);
        assertThat(msg.getUsername()).isEqualTo("alice");
        assertThat(msg.getContent()).isEqualTo("Hello world!");
        assertThat(msg.getSentAt()).isEqualTo(sentAt);
    }

    @Test
    void providerMsgId_staysMappedToTheExistingAgoraColumn() throws NoSuchFieldException {
        // The physical rename is deferred to the Agora cleanup PR: no migration in this deploy.
        var column = LiveChatMessage.class.getDeclaredField("providerMsgId").getAnnotation(jakarta.persistence.Column.class);

        assertThat(column.name()).isEqualTo("agora_msg_id");
    }

    @Test
    void create_generatesUniqueIds() {
        var live = Live.create(UUID.randomUUID(), "Live Test", null, null, 60);
        var sentAt = OffsetDateTime.now();

        var msg1 = LiveChatMessage.create(live, "msg-001", UUID.randomUUID(), "alice", "Hi", sentAt);
        var msg2 = LiveChatMessage.create(live, "msg-002", UUID.randomUUID(), "bob", "Hey", sentAt);

        assertThat(msg1.getId()).isNotEqualTo(msg2.getId());
    }
}
