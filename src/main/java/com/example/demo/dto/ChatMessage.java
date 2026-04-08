package com.example.demo.dto;

public class ChatMessage {
    private String role;
    private String content;

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String role() {
        return this.role;
    }

    public String content() {
        return this.content;
    }
}