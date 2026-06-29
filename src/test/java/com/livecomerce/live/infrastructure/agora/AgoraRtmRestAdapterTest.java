package com.livecomerce.live.infrastructure.agora;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AgoraRtmRestAdapterTest {

    private static final String APP_ID         = "test-app-id";
    private static final String CUSTOMER_ID    = "test-cust-id";
    private static final String CUSTOMER_SECRET = "test-cust-secret";
    private static final String EXPECTED_AUTH  =
            "Basic " + Base64.getEncoder().encodeToString((CUSTOMER_ID + ":" + CUSTOMER_SECRET).getBytes());

    private MockRestServiceServer server;
    private AgoraRtmRestAdapter sut;

    @BeforeEach
    void setUp() {
        var rt = new RestTemplate();
        server = MockRestServiceServer.createServer(rt);
        var restClient = RestClient.create(rt);
        sut = new AgoraRtmRestAdapter(restClient, APP_ID, CUSTOMER_ID, CUSTOMER_SECRET);
    }

    @Test
    void sendChannelMessage_setsBasicAuthHeader() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString("channel_messages")))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("Authorization", EXPECTED_AUTH))
              .andExpect(header("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
              .andRespond(withSuccess());

        sut.sendChannelMessage("live-chat:123", "{\"type\":\"test\"}");

        server.verify();
    }

    @Test
    void sendChannelMessage_hitsChannelMessagesEndpointWithCorrectBody() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "/dev/v2/project/" + APP_ID + "/rtm/users/live-server/channel_messages")))
              .andExpect(method(HttpMethod.POST))
              .andExpect(jsonPath("$.channel_name").value("live-chat:123"))
              .andExpect(jsonPath("$.enable_historical_messaging").value(false))
              .andExpect(jsonPath("$.payload").value("{\"type\":\"test\"}"))
              .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        sut.sendChannelMessage("live-chat:123", "{\"type\":\"test\"}");

        server.verify();
    }

    @Test
    void sendPeerMessage_hitsPeerMessagesEndpointWithDestination() {
        server.expect(requestTo(org.hamcrest.Matchers.containsString(
                        "/dev/v2/project/" + APP_ID + "/rtm/users/live-server/peer_messages")))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("Authorization", EXPECTED_AUTH))
              .andExpect(jsonPath("$.destination").value("seller-user-id"))
              .andExpect(jsonPath("$.enable_offline_messaging").value(false))
              .andExpect(jsonPath("$.enable_historical_messaging").value(false))
              .andExpect(jsonPath("$.payload").value("{\"type\":\"order-confirmed\"}"))
              .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        sut.sendPeerMessage("seller-user-id", "{\"type\":\"order-confirmed\"}");

        server.verify();
    }
}
