package com.seunome.zeneger;

import android.app.Application;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

public class ZenegerApp extends Application {

    private static int activeActivities = 0;
    private static boolean isAppInForeground = false;

    public static void activityStarted() {
        activeActivities++;
        if (activeActivities == 1 && !isAppInForeground) {
            isAppInForeground = true;
            setOnlineStatus(true);
        }
    }

    public static void activityStopped() {
        activeActivities--;
        if (activeActivities == 0) {
            isAppInForeground = false;
            setOnlineStatus(false);
        }
    }

    private static void setOnlineStatus(boolean online) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        db.getReference("users").child(uid).child("online").setValue(online);
        if (!online) {
            db.getReference("users").child(uid).child("lastSeen")
                    .setValue(String.valueOf(System.currentTimeMillis()));
        }
    }
}