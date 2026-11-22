package com.livecomerce.live.infrastructure.ivs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ivschat.IvschatClient;

import java.time.Duration;

@Configuration
public class IvsChatClientConfig {

    @Value("${ivs.region}")
    private String ivsRegion;

    @Value("${ivs.chat.api-call-timeout-ms:3000}")
    private long apiCallTimeoutMs;

    @Value("${ivs.chat.api-call-attempt-timeout-ms:1500}")
    private long apiCallAttemptTimeoutMs;

    @Bean
    public IvschatClient ivschatClient() {
        return IvschatClient.builder()
                .region(Region.of(ivsRegion))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                // Timeouts surface as SdkClientException, which the adapters map to ChatProviderUnavailableException.
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofMillis(apiCallTimeoutMs))
                        .apiCallAttemptTimeout(Duration.ofMillis(apiCallAttemptTimeoutMs))
                        .build())
                .build();
    }
}
