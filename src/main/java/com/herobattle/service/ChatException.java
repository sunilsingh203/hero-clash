package com.herobattle.service;

/** A chat message was rejected (empty, too long, or from a non-member). Maps to HTTP 422. */
public class ChatException extends RuntimeException {

    public ChatException(String message) {
        super(message);
    }
}
