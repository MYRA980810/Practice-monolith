package com.livecomerce.live.infrastructure.redis;

import com.livecomerce.live.application.LiveFeedEvent;
import com.livecomerce.live.application.port.in.LiveFeedEventSink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveFeedEventDispatcherTest {

    @Mock ObjectProvider<LiveFeedEventSink> sinkProvider;
    @Mock LiveFeedEventSink first;
    @Mock LiveFeedEventSink second;

    private static final LiveFeedEvent EVENT = new LiveFeedEvent.Resynced(7L);

    @Test
    void deliver_fansOutToEverySink() {
        when(sinkProvider.orderedStream()).thenReturn(Stream.of(first, second));

        new LiveFeedEventDispatcher(sinkProvider).deliver(EVENT);

        verify(first).deliver(EVENT);
        verify(second).deliver(EVENT);
    }

    @Test
    void deliver_sinkFailure_doesNotStopOtherSinksNorPropagate() {
        when(sinkProvider.orderedStream()).thenReturn(Stream.of(first, second));
        doThrow(new IllegalStateException("boom")).when(first).deliver(EVENT);

        assertThatCode(() -> new LiveFeedEventDispatcher(sinkProvider).deliver(EVENT)).doesNotThrowAnyException();

        verify(second).deliver(EVENT);
    }

    @Test
    void deliver_noSinks_isNoOp() {
        when(sinkProvider.orderedStream()).thenReturn(Stream.empty());

        assertThatCode(() -> new LiveFeedEventDispatcher(sinkProvider).deliver(EVENT)).doesNotThrowAnyException();
    }
}
