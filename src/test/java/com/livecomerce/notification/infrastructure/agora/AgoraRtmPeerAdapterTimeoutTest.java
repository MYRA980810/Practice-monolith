package com.livecomerce.notification.infrastructure.agora;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(OutputCaptureExtension.class)
class AgoraRtmPeerAdapterTimeoutTest {

    private static final Duration CONNECT_TIMEOUT = Duration.ofMillis(500);
    private static final Duration READ_TIMEOUT    = Duration.ofMillis(300);
    private static final long     SERVER_DELAY_MS = 5_000;

    private HttpServer server;
    private ExecutorService handlers;
    private AgoraRtmPeerAdapter sut;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try {
                Thread.sleep(SERVER_DELAY_MS);
                exchange.sendResponseHeaders(200, -1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        handlers = Executors.newCachedThreadPool();
        server.setExecutor(handlers);
        server.start();
        var baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        sut = new AgoraRtmPeerAdapter(baseUrl, CONNECT_TIMEOUT, READ_TIMEOUT, "app", "cust", "secret");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        handlers.shutdownNow();
    }

    @Test
    void sendPeerMessage_slowAgora_endsWithinReadTimeoutWithoutThrowing(CapturedOutput output) {
        long start = System.nanoTime();

        assertThatCode(() -> sut.sendPeerMessage("user-1", "{}")).doesNotThrowAnyException();

        long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        assertThat(elapsedMs).isLessThan(2_000);
        assertThat(output).contains("WARN").contains("Agora RTM peer message to user 'user-1' failed");
    }
}
