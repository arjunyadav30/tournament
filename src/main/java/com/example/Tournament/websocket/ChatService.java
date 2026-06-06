package com.example.Tournament.websocket;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    private final List<ChatMessage> history = Collections.synchronizedList(new ArrayList<>());

    public void add(ChatMessage m) {
        if (history.size() > 200) history.remove(0);
        history.add(m);
    }

    public List<ChatMessage> getHistory() {
        return new ArrayList<>(history);
    }
}
