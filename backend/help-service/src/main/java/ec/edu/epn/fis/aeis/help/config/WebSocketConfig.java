package ec.edu.epn.fis.aeis.help.config;

import ec.edu.epn.fis.aeis.help.security.JwtHandshakeInterceptor;
import ec.edu.epn.fis.aeis.help.security.WsAuthChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Endpoint /ws sin SockJS, broker simple /topic, prefijo de aplicación /app
 * (ver PLAN.md §7.3). El gateway reenvía el handshake tal cual, sin reescribir
 * la ruta.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final WsAuthChannelInterceptor wsAuthChannelInterceptor;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           WsAuthChannelInterceptor wsAuthChannelInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.wsAuthChannelInterceptor = wsAuthChannelInterceptor;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(wsAuthChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(jwtHandshakeInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
