package com.seunome.zeneger;

import java.util.Map;

public class Group {
    public String groupId, name, createdBy, lastMessage, lastMessageTime;
    public Map<String, Boolean> members;

    public Group() {}

    public Group(String groupId, String name, String createdBy) {
        this.groupId = groupId;
        this.name = name;
        this.createdBy = createdBy;
        this.lastMessage = "";
        this.lastMessageTime = "";
    }
}