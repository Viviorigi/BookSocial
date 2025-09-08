package com.duong.chat.service;

import com.corundumstudio.socketio.SocketIOServer;
import com.duong.chat.entity.WebSocketSession;
import com.duong.chat.dto.request.ChatMessageRequest;
import com.duong.chat.dto.response.ChatMessageResponse;
import com.duong.chat.entity.ChatMessage;
import com.duong.chat.entity.ParticipantInfo;
import com.duong.chat.exception.AppException;
import com.duong.chat.exception.ErrorCode;
import com.duong.chat.mapper.ChatMessageMapper;
import com.duong.chat.repository.ChatMessageRepository;
import com.duong.chat.repository.ConversationRepository;
import com.duong.chat.repository.WebSocketSessionRepository;
import com.duong.chat.repository.httpclient.ProfileClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {
    ChatMessageRepository chatMessageRepository;
    ConversationRepository conversationRepository;
    ProfileClient profileClient;
    SocketIOServer socketIOServer;
    ChatMessageMapper chatMessageMapper;
    WebSocketSessionRepository webSocketSessionRepository;

    ObjectMapper objectMapper;

    public List<ChatMessageResponse> getMessages(String conversationId) {
        // Validate conversation
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // validate conversationid
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND))
                .getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny().orElseThrow( ()-> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateDesc(conversationId);

        return messages.stream().map(this::toChatMessageResponse).toList();
    }

    public ChatMessageResponse create(ChatMessageRequest request) throws JsonProcessingException {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        // validate conversationid
        var conversation =  conversationRepository.findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        conversation.getParticipants()
                .stream()
                .filter(participantInfo -> userId.equals(participantInfo.getUserId()))
                .findAny().orElseThrow( ()-> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        // get userInfo from profile service
        var userResponse = profileClient.getProfile(userId);
        if (Objects.isNull(userResponse)) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }

        var userInfo = userResponse.getResult();

        // build chat message info
        ChatMessage chatMessage = chatMessageMapper.toChatMessage(request);
        chatMessage.setSender(ParticipantInfo.builder()
                        .userId(userInfo.getUserId())
                        .username(userInfo.getUsername())
                        .firstName(userInfo.getFirstName())
                        .lastName(userInfo.getLastName())
                        .avatar(userInfo.getAvatar())
                .build());
        chatMessage.setCreatedDate(Instant.now());

        // create chat message
        chatMessage = chatMessageRepository.save(chatMessage);



        //Publish socket event to clients is conversation
        // get participants userIds;
        List<String> userIds =conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId).toList();

        Map<String, WebSocketSession> webSocketSessions =
                webSocketSessionRepository
                    .findAllByUserIdIn(userIds).stream()
                        .collect(Collectors.toMap(
                                WebSocketSession::getSocketSessionId,
                                Function.identity()));

        ChatMessageResponse chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);
        socketIOServer.getAllClients().forEach(client ->{

            var webSocketSession = webSocketSessions.get(client.getSessionId().toString());

            if (Objects.nonNull(webSocketSession)) {
                String message = null;
                try {
                    chatMessageResponse.setMe(webSocketSession.getUserId().equals(userId));
                    message = objectMapper.writeValueAsString(chatMessageResponse);
                    client.sendEvent("message", message);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //convert to response
        return toChatMessageResponse(chatMessage);
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var chatMessageResponse = chatMessageMapper.toChatMessageResponse(chatMessage);

        chatMessageResponse.setMe(userId.equals(chatMessage.getSender().getUserId()));

        return chatMessageResponse;
    }
}