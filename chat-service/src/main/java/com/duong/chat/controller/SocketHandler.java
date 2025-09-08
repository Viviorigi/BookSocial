package com.duong.chat.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.duong.chat.dto.request.IntrospectRequest;
import com.duong.chat.entity.WebSocketSession;
import com.duong.chat.service.IdentityService;
import com.duong.chat.service.WebSocketSessionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocketHandler {
    SocketIOServer server;
    IdentityService identityService;
    WebSocketSessionService webSocketSessionService;

    @OnConnect
    public void clientConnected(SocketIOClient client) {
        // get token from request param
        String token = client.getHandshakeData().getSingleUrlParam("token");

        // verify token
        var introspectResponse = identityService.introspect(IntrospectRequest.builder()
                    .token(token)
                .build());

        // if token is invalid -> disconnect
        if(introspectResponse.isValid()){
            log.info("client connected: {} ", client.getSessionId());
            //Persist webSocketSession
            WebSocketSession webSocketSession = WebSocketSession.builder()
                    .socketSessionId(client.getSessionId().toString())
                    .userId(introspectResponse.getUserId())
                    .createdAt(Instant.now())
                    .build();
            webSocketSession = webSocketSessionService.create(webSocketSession);
            log.info("webSocket session created with id : {}", webSocketSession.getId());
        } else {
            log.error("Authentication fail: {} ", client.getSessionId());
            client.disconnect();
        }
    }

    @OnDisconnect
    public void clientDisconnected(SocketIOClient client) {
        log.info("client disconnected: {}", client.getSessionId());
        webSocketSessionService.deleteSession(client.getSessionId().toString());
    }

    @PostConstruct
    public void startServer() {
        server.start();
        server.addListeners(this);
        log.info("Socket Server started");
    }

    @PreDestroy
    public void stopServer() {
        server.stop();
        log.info("Socket Server stopped");
    }
}
