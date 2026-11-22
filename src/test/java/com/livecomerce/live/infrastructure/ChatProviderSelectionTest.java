package com.livecomerce.live.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.ChatProvider;
import com.livecomerce.live.application.port.out.AgoraRtmMessagePort;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import software.amazon.awssdk.services.ivschat.IvschatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Proves {@code live.chat-provider} selects exactly one {@link LiveRoomEventPort}
 * and that the choice agrees with {@link ChatProvider#isIvs} (what StartLiveService uses).
 */
class ChatProviderSelectionTest {

    @Configuration
    @ComponentScan(
            basePackages = "com.livecomerce.live.infrastructure",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = LiveRoomEventPort.class))
    static class RoomEventAdapters {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(RoomEventAdapters.class)
            .withBean(IvschatClient.class, () -> mock(IvschatClient.class))
            .withBean(AgoraRtmMessagePort.class, () -> mock(AgoraRtmMessagePort.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    private void assertSelects(ApplicationContextRunner contextRunner, String effectiveValue) {
        String expected = ChatProvider.isIvs(effectiveValue) ? "IvsRoomEventAdapter" : "AgoraRoomEventAdapter";
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LiveRoomEventPort.class);
            assertThat(context.getBean(LiveRoomEventPort.class).getClass().getSimpleName()).isEqualTo(expected);
        });
    }

    @Test
    void unset_selectsAgora() {
        assertSelects(runner, ChatProvider.DEFAULT);
        assertThat(ChatProvider.isIvs(ChatProvider.DEFAULT)).isFalse();
    }

    @Test
    void agora_selectsAgora() {
        assertSelects(runner.withPropertyValues("live.chat-provider=agora"), "agora");
    }

    @Test
    void ivs_selectsIvs() {
        assertSelects(runner.withPropertyValues("live.chat-provider=ivs"), "ivs");
        assertThat(ChatProvider.isIvs("ivs")).isTrue();
    }

    @Test
    void empty_selectsAgora() {
        assertSelects(runner.withPropertyValues("live.chat-provider="), "");
    }

    @Test
    void typo_selectsAgora() {
        assertSelects(runner.withPropertyValues("live.chat-provider=ivss"), "ivss");
    }

    @Test
    void upperCaseIvs_selectsAgora_likeStartLiveService() {
        assertSelects(runner.withPropertyValues("live.chat-provider=IVS"), "IVS");
    }
}
