package com.duong.chat.mapper;

import com.duong.chat.dto.request.ChatMessageRequest;
import com.duong.chat.dto.response.ChatMessageResponse;
import com.duong.chat.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {
    ChatMessageResponse toChatMessageResponse(ChatMessage chatMessage);

    ChatMessage toChatMessage(ChatMessageRequest request);

    List<ChatMessageResponse> toChatMessageResponses(List<ChatMessage> chatMessages);
}