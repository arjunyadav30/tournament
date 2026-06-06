package com.example.Tournament.websocket;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ChatController {

    @Autowired private ChatService chatService;

    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage send(ChatMessage message) throws Exception {
        if (message.getTimestamp() == 0) message.setTimestamp(Instant.now().toEpochMilli());
        chatService.add(message);
        return message;
    }
}
