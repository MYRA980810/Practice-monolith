package com.livecomerce.live.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ChatProviderTest {

    @Test
    void isIvs_exactIvsValue_isTrue() {
        assertThat(ChatProvider.isIvs("ivs")).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"agora", "IVS", "ivs ", "ivss", "typo"})
    void isIvs_anyOtherValue_isFalse(String value) {
        assertThat(ChatProvider.isIvs(value)).isFalse();
    }

    @Test
    void defaultValue_isAgora() {
        assertThat(ChatProvider.DEFAULT).isEqualTo("agora");
        assertThat(ChatProvider.isIvs(ChatProvider.DEFAULT)).isFalse();
    }
}
