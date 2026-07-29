package com.seunome.zeneger;

public class Conversation {
    public static final int TYPE_USER  = 1;
    public static final int TYPE_GROUP = 2;

    public int type;
    public String id;
    public String name;
    public String photoUrl;
    public boolean online;
    public String lastSeen;
    public String lastMessage;
    public String lastMessageTime;
    public boolean unread;
    public boolean favorite;
    public int unreadCount;

    public Conversation() {}
}