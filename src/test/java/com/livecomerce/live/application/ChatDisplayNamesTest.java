package com.livecomerce.live.application;

import com.livecomerce.live.LoadSellerNamesPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatDisplayNamesTest {

    @Mock LoadSellerNamesPort loadUserNamesPort;

    private static final UUID ANN = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @Test
    void normalize_stripsAndKeepsShortNames() {
        assertThat(ChatDisplayNames.normalize("  Ann  ")).isEqualTo("Ann");
    }

    @Test
    void normalize_nullOrBlank_fallsBackToUser() {
        assertThat(ChatDisplayNames.normalize(null)).isEqualTo(ChatDisplayNames.FALLBACK);
        assertThat(ChatDisplayNames.normalize("   ")).isEqualTo(ChatDisplayNames.FALLBACK);
        assertThat(ChatDisplayNames.FALLBACK).isEqualTo("User");
    }

    @Test
    void normalize_truncatesTo64CodePoints_withoutSplittingSurrogatePairs() {
        assertThat(ChatDisplayNames.normalize("x".repeat(200))).isEqualTo("x".repeat(64));
        assertThat(ChatDisplayNames.normalize("😀".repeat(100))).isEqualTo("😀".repeat(64));
    }

    @Test
    void resolve_loadsAllNamesInOneBatch_andNormalizesEach() {
        when(loadUserNamesPort.loadNames(any())).thenReturn(Map.of(ANN, " Ann "));

        var names = ChatDisplayNames.resolve(loadUserNamesPort, List.of(ANN, BOB));

        assertThat(names).containsExactlyInAnyOrderEntriesOf(Map.of(ANN, "Ann", BOB, "User"));
        verify(loadUserNamesPort, times(1)).loadNames(any());
    }

    @Test
    void resolve_lookupFailure_fallsBackForEveryUser() {
        when(loadUserNamesPort.loadNames(any())).thenThrow(new IllegalStateException("db down"));

        var names = ChatDisplayNames.resolve(loadUserNamesPort, List.of(ANN, BOB));

        assertThat(names).containsExactlyInAnyOrderEntriesOf(Map.of(ANN, "User", BOB, "User"));
    }

    @Test
    void resolve_noUsers_skipsTheLookup() {
        assertThat(ChatDisplayNames.resolve(loadUserNamesPort, List.of())).isEmpty();
        verifyNoInteractions(loadUserNamesPort);
    }
}
