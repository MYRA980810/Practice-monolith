package com.livecomerce.live.infrastructure.ivs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.livecomerce.live.application.port.in.SaveChatMessageUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The chat-messages SQS listener exists only when {@code live.chat-provider=ivs}. With
 * {@code queue-not-found-strategy: fail}, registering it under the default (agora) config
 * would break startup wherever the {@code ivs-chat-messages} queue is not provisioned.
 */
class IvsChatMessageListenerRegistrationTest {

    @Configuration
    @ComponentScan(
            basePackages = "com.livecomerce.live.infrastructure.ivs",
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = IvsChatMessageListener.class))
    static class ListenerScan {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ListenerScan.class)
            .withBean(SaveChatMessageUseCase.class, () -> mock(SaveChatMessageUseCase.class))
            .withBean(ObjectMapper.class, ObjectMapper::new);

    @Test
    void defaultConfig_doesNotRegisterTheListener() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(IvsChatMessageListener.class);
        });
    }

    @Test
    void agoraProvider_doesNotRegisterTheListener() {
        runner.withPropertyValues("live.chat-provider=agora").run(context ->
                assertThat(context).doesNotHaveBean(IvsChatMessageListener.class));
    }

    @Test
    void ivsProvider_registersTheListener() {
        runner.withPropertyValues("live.chat-provider=ivs").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(IvsChatMessageListener.class);
        });
    }
}
