package com.livecomerce.live.infrastructure.ivs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ivs.IvsClient;

@Configuration
public class IvsClientConfig {

    @Value("${ivs.region}")
    private String ivsRegion;

    @Bean
    public IvsClient ivsClient() {
        return IvsClient.builder()
                .region(Region.of(ivsRegion))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }
}
