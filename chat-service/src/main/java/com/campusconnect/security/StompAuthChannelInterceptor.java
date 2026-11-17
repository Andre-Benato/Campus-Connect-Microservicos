package com.campusconnect.security;

import com.campusconnect.chat.infrastructure.adapters.out.http.TokenValidateResponse;
import com.campusconnect.chat.infrastructure.adapters.out.http.UserAuthClient;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Valida o Bearer no CONNECT STOMP via {@link UserAuthClient} (mesmo fluxo do filtro REST).
 * Rejeita a conexao se o token for ausente ou invalido.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserAuthClient userAuthClient;

    public StompAuthChannelInterceptor(UserAuthClient userAuthClient) {
        this.userAuthClient = userAuthClient;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authorization = firstNativeHeader(accessor, HttpHeaders.AUTHORIZATION);
        if (authorization == null
                || !authorization.regionMatches(
                        true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new org.springframework.messaging.MessagingException(
                    "CONNECT STOMP exige Authorization Bearer");
        }

        TokenValidateResponse validated =
                userAuthClient
                        .validate(authorization)
                        .orElseThrow(
                                () ->
                                        new org.springframework.messaging.MessagingException(
                                                "Token invalido ou user-service indisponivel"));

        AuthenticatedUser principal =
                new AuthenticatedUser(validated.userId(), validated.email(), validated.name());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        accessor.setUser(authentication);
        return message;
    }

    private static String firstNativeHeader(StompHeaderAccessor accessor, String name) {
        List<String> values = accessor.getNativeHeader(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }
}
