package com.livecomerce.live.application;

import com.livecomerce.live.application.port.out.ChatRoomPort;
import com.livecomerce.live.application.port.out.LiveRoomEventPort;
import com.livecomerce.live.application.port.out.RoomEvent;
import com.livecomerce.live.application.port.out.VideoBroadcastPort;
import com.livecomerce.live.domain.Live;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveRoomCloserTest {

    @Mock LiveRoomEventPort   liveRoomEventPort;
    @Mock VideoBroadcastPort  videoBroadcastPort;
    @Mock ChatRoomPort        chatRoomPort;
    @InjectMocks LiveRoomCloser sut;

    private static final String CHANNEL_ARN   = "arn:ivs:channel";
    private static final String CHAT_ROOM_ARN = "arn:ivschat:room";

    private Live live() {
        var live = Live.create(UUID.randomUUID(), "My Live", null, null, 60);
        live.start();
        return live;
    }

    private Live ivsLive() {
        var live = live();
        live.setIvsChannel(CHANNEL_ARN, "rtmps://ingest", "arn:ivs:key", "sk_stream_key", "https://playback.url");
        return live;
    }

    @Test
    void closeRoom_publishesLiveEndedRoomEvent() {
        var live = live();

        sut.closeRoom(live);

        verify(liveRoomEventPort).publish(live, new RoomEvent.LiveEnded(live.getId()));
    }

    @Test
    void closeRoom_withIvsChannel_stopsIvsStream() {
        var live = ivsLive();

        sut.closeRoom(live);

        verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
    }

    @Test
    void closeRoom_withoutIvsChannel_skipsStopStream() {
        sut.closeRoom(live());

        verifyNoInteractions(videoBroadcastPort);
    }

    @Test
    void closeRoom_stopStreamFailure_isSwallowed() {
        var live = ivsLive();
        doThrow(new RuntimeException("IVS error")).when(videoBroadcastPort).stopStream(CHANNEL_ARN);

        assertThatCode(() -> sut.closeRoom(live)).doesNotThrowAnyException();
    }

    @Test
    void closeRoom_roomEventNotDelivered_stillStopsIvsStream() {
        var live = ivsLive();
        when(liveRoomEventPort.publish(any(), any())).thenReturn(false);

        assertThatCode(() -> sut.closeRoom(live)).doesNotThrowAnyException();
        verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
    }

    @Test
    void closeRoom_withChatRoom_deletesChatRoom() {
        var live = live();
        live.setChatRoom(CHAT_ROOM_ARN);

        sut.closeRoom(live);

        verify(chatRoomPort).deleteRoom(CHAT_ROOM_ARN);
    }

    @Test
    void closeRoom_withoutChatRoom_skipsDeleteRoom() {
        sut.closeRoom(live());

        verifyNoInteractions(chatRoomPort);
    }

    @Test
    void closeRoom_deleteRoomFailure_isSwallowed_andStillStopsIvsStream() {
        var live = ivsLive();
        live.setChatRoom(CHAT_ROOM_ARN);
        doThrow(new RuntimeException("IVS Chat error")).when(chatRoomPort).deleteRoom(CHAT_ROOM_ARN);

        assertThatCode(() -> sut.closeRoom(live)).doesNotThrowAnyException();
        verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
    }

    // --- Transaction-aware teardown: the end/cancel transaction must commit before anything external happens ---

    private final TransactionTemplate tx = new TransactionTemplate(new NoOpTransactionManager());

    private Live ivsLiveWithChatRoom() {
        var live = ivsLive();
        live.setChatRoom(CHAT_ROOM_ARN);
        return live;
    }

    @Test
    void closeRoom_insideTransaction_defersTeardownUntilCommit() {
        var live = ivsLiveWithChatRoom();

        tx.executeWithoutResult(status -> {
            sut.closeRoom(live);
            verifyNoInteractions(liveRoomEventPort, videoBroadcastPort, chatRoomPort);
        });

        var order = inOrder(liveRoomEventPort, videoBroadcastPort, chatRoomPort);
        order.verify(liveRoomEventPort).publish(live, new RoomEvent.LiveEnded(live.getId()));
        order.verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
        order.verify(chatRoomPort).deleteRoom(CHAT_ROOM_ARN);
    }

    @Test
    void closeRoom_transactionRolledBack_neverDeletesRoomNorAnnouncesEnd() {
        var live = ivsLiveWithChatRoom();

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            sut.closeRoom(live);
            throw new IllegalStateException("close failed after closeRoom");
        })).isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(liveRoomEventPort, videoBroadcastPort, chatRoomPort);
    }

    @Test
    void closeRoom_transactionMarkedRollbackOnly_neverDeletesRoom() {
        var live = ivsLiveWithChatRoom();

        tx.executeWithoutResult(status -> {
            sut.closeRoom(live);
            status.setRollbackOnly();
        });

        verifyNoInteractions(liveRoomEventPort, videoBroadcastPort, chatRoomPort);
    }

    @Test
    void closeRoom_withoutTransaction_tearsDownImmediately() {
        var live = ivsLiveWithChatRoom();

        sut.closeRoom(live);

        verify(liveRoomEventPort).publish(live, new RoomEvent.LiveEnded(live.getId()));
        verify(videoBroadcastPort).stopStream(CHANNEL_ARN);
        verify(chatRoomPort).deleteRoom(CHAT_ROOM_ARN);
    }

    @Test
    void closeRoom_afterCommitFailures_areSwallowed_andNeverFailTheCommittedClose() {
        var live = ivsLiveWithChatRoom();
        when(liveRoomEventPort.publish(any(), any())).thenThrow(new RuntimeException("room event error"));
        doThrow(new RuntimeException("IVS error")).when(videoBroadcastPort).stopStream(CHANNEL_ARN);
        doThrow(new RuntimeException("IVS Chat error")).when(chatRoomPort).deleteRoom(CHAT_ROOM_ARN);

        assertThatCode(() -> tx.executeWithoutResult(status -> sut.closeRoom(live)))
                .doesNotThrowAnyException();
        verify(chatRoomPort).deleteRoom(CHAT_ROOM_ARN);
        verifyNoMoreInteractions(chatRoomPort);
    }

    /** Real Spring transaction synchronization lifecycle without a database. */
    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
