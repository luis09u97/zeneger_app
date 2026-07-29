package com.seunome.zeneger;

import java.util.Map;

public class Message {
    public String senderId, text, timestamp, fullTimestamp;
    public boolean deletedForAll, read;
    public Map<String, Boolean> deletedFor;
    public Map<String, String> reactions;

    public Message() {}

    public Message(String senderId, String text, String timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.fullTimestamp = String.valueOf(System.currentTimeMillis());
        this.read = false;
    }
}