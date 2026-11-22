package com.livecomerce.live.infrastructure;

import com.livecomerce.live.application.ChatProvider;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Bean conditions on {@code live.chat-provider}, decided by {@link ChatProvider#isIvs}
 * so adapter selection always agrees with StartLiveService.
 */
public final class ChatProviderConditions {

    private ChatProviderConditions() {
    }

    private static boolean ivsSelected(ConditionContext context) {
        return ChatProvider.isIvs(context.getEnvironment().getProperty(ChatProvider.PROPERTY, ChatProvider.DEFAULT));
    }

    public static final class IvsSelected implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return ivsSelected(context);
        }
    }

    public static final class AgoraSelected implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !ivsSelected(context);
        }
    }
}
