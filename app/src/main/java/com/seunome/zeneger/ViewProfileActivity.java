package com.seunome.zeneger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.database.*;
import de.hdodenhof.circleimageview.CircleImageView;
import java.text.SimpleDateFormat;
import java.util.*;

public class ViewProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);
        PremiumUi.apply(this);

        String userId = getIntent().getStringExtra("userId");

        CircleImageView avatar  = findViewById(R.id.profileAvatar);
        TextView avatarLetter   = findViewById(R.id.profileAvatarLetter);
        TextView name           = findViewById(R.id.profileName);
        TextView bio            = findViewById(R.id.profileBio);
        TextView statusText     = findViewById(R.id.statusText);
        TextView messageBtn     = findViewById(R.id.messageBtn);
        View backBtn            = findViewById(R.id.backBtn);

        try {
            findViewById(android.R.id.content)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception ignored) {}

        if (backBtn != null) backBtn.setOnClickListener(v -> finish());
        if (userId == null) { finish(); return; }

        FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) return;

                        if (name != null) name.setText(user.name);
                        if (bio != null) bio.setText(
                                user.bio != null && !user.bio.isEmpty()
                                        ? user.bio : "Sem bio definida");

                        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                            if (avatar != null) {
                                avatar.setVisibility(View.VISIBLE);
                                try {
                                    Glide.with(ViewProfileActivity.this)
                                            .load(user.photoUrl)
                                            .placeholder(R.drawable.bg_avatar)
                                            .circleCrop()
                                            .into(avatar);
                                } catch (Exception ignored) {}
                            }
                            if (avatarLetter != null) avatarLetter.setVisibility(View.GONE);
                        } else {
                            if (avatar != null) avatar.setVisibility(View.GONE);
                            if (avatarLetter != null) {
                                avatarLetter.setVisibility(View.VISIBLE);
                                avatarLetter.setText(user.name != null && !user.name.isEmpty()
                                        ? String.valueOf(user.name.charAt(0)).toUpperCase() : "?");
                            }
                        }

                        if (statusText != null) {
                            if (user.online) {
                                statusText.setText("● Online agora");
                                statusText.setTextColor(0xFF22C55E);
                            } else if (user.lastSeen != null && !user.lastSeen.isEmpty()) {
                                try {
                                    long ms = Long.parseLong(user.lastSeen);
                                    String fmt = new SimpleDateFormat("dd/MM HH:mm",
                                            Locale.getDefault()).format(new Date(ms));
                                    statusText.setText("Visto por último: " + fmt);
                                } catch (Exception e) {
                                    statusText.setText("Offline");
                                }
                                statusText.setTextColor(0xFF9CA3AF);
                            }
                        }

                        if (messageBtn != null) {
                            messageBtn.setOnClickListener(v -> {
                                try {
                                    v.startAnimation(AnimationUtils.loadAnimation(
                                            ViewProfileActivity.this, R.anim.bounce));
                                } catch (Exception ignored) {}
                                Intent intent = new Intent(ViewProfileActivity.this,
                                        ChatActivity.class);
                                intent.putExtra("receiverId", user.uid);
                                intent.putExtra("receiverName", user.name);
                                intent.putExtra("receiverPhoto",
                                        user.photoUrl != null ? user.photoUrl : "");
                                startActivity(intent);
                                finish();
                            });
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZenegerApp.activityStarted();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZenegerApp.activityStopped();
    }
}
