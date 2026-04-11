package edu.uni.smartdocs.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // hỗ trợ browser
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue", "/topic");   // /queue cho chat 1-1, /topic cho broadcast
        registry.setApplicationDestinationPrefixes("/app"); // prefix cho @MessageMapping
        registry.setUserDestinationPrefix("/user");        // quan trọng cho gửi riêng theo user (email)
    }
}