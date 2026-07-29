package com.seunome.zeneger;

public class User {
    public String uid, name, email, photoUrl, lastSeen, bio;
    public boolean online;

    public User() {}

    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.photoUrl = "";
        this.lastSeen = "";
        this.bio = "";
        this.online = false;
    }
}