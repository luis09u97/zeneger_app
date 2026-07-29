package com.seunome.zeneger;

public class Story {
    public String uid, userName, imageUrl, timestamp;

    public Story() {}

    public Story(String uid, String userName, String imageUrl, String timestamp) {
        this.uid       = uid;
        this.userName  = userName;
        this.imageUrl  = imageUrl;
        this.timestamp = timestamp;
    }

    public boolean isExpired() {
        try {
            long posted = Long.parseLong(timestamp);
            long now    = System.currentTimeMillis();
            return (now - posted) > 24 * 60 * 60 * 1000; // 24 horas
        } catch (Exception e) { return true; }
    }

    public String getTimeAgo() {
        try {
            long posted = Long.parseLong(timestamp);
            long diff   = System.currentTimeMillis() - posted;
            long hours  = diff / (60 * 60 * 1000);
            long mins   = diff / (60 * 1000);
            if (hours >= 1) return "há " + hours + "h";
            return "há " + mins + "min";
        } catch (Exception e) { return ""; }
    }
}