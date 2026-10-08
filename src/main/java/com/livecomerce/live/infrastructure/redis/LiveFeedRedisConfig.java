package com.livecomerce.live.infrastructure.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/** Subscribes every instance to the feed's Pub/Sub channel (one subscription per instance). */
@Configuration
@Profile("!local")
class LiveFeedRedisConfig {

    @Bean
    RedisMessageListenerContainer liveFeedListenerContainer(RedisConnectionFactory connectionFactory,
                                                            RedisLiveFeedSubscriber subscriber) {
        var container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(RedisLiveFeedAdapter.CHANNEL));
        return container;
    }
}
