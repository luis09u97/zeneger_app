package com.seunome.zeneger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    CircleImageView profileImage;
    TextView profileAvatarLetter, userName, userEmail, userBio, backBtn;
    TextView contactsCount, storiesCount, groupsCount;
    FrameLayout changePhotoBtn;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    String myUid;
    static final int PICK_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() == null) { finish(); return; }
        myUid = mAuth.getCurrentUser().getUid();

        profileImage        = findViewById(R.id.profileImage);
        profileAvatarLetter = findViewById(R.id.profileAvatarLetter);
        userName            = findViewById(R.id.userName);
        userEmail           = findViewById(R.id.userEmail);
        userBio             = findViewById(R.id.userBio);
        changePhotoBtn      = findViewById(R.id.changePhotoBtn);
        backBtn             = findViewById(R.id.backBtn);
        contactsCount       = findViewById(R.id.contactsCount);
        storiesCount        = findViewById(R.id.storiesCount);
        groupsCount         = findViewById(R.id.groupsCount);

        try {
            findViewById(android.R.id.content)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception ignored) {}

        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        if (changePhotoBtn != null) {
            changePhotoBtn.setOnClickListener(v -> {
                try {
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                } catch (Exception ignored) {}
                Intent intent = new Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, PICK_IMAGE);
            });
        }

        loadUserData();
        loadStats();
    }

    private void loadStats() {
        mDatabase.child("contacts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (contactsCount != null)
                            contactsCount.setText(String.valueOf(snapshot.getChildrenCount()));
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });

        mDatabase.child("stories").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long count = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Story s = ds.getValue(Story.class);
                                if (s != null && !s.isExpired()) count++;
                            } catch (Exception ignored) {}
                        }
                        if (storiesCount != null) storiesCount.setText(String.valueOf(count));
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });

        mDatabase.child("groups")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long count = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Group g = ds.getValue(Group.class);
                                if (g != null && g.members != null
                                        && g.members.containsKey(myUid)) count++;
                            } catch (Exception ignored) {}
                        }
                        if (groupsCount != null) groupsCount.setText(String.valueOf(count));
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    private void loadUserData() {
        mDatabase.child("users").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) return;

                        if (userName != null) userName.setText(user.name);
                        if (userEmail != null) userEmail.setText(user.email);
                        if (userBio != null) userBio.setText(
                                user.bio != null && !user.bio.isEmpty()
                                        ? user.bio : "Sem bio definida");

                        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                            if (profileImage != null) {
                                profileImage.setVisibility(View.VISIBLE);
                                try {
                                    Glide.with(ProfileActivity.this)
                                            .load(user.photoUrl)
                                            .placeholder(R.drawable.bg_avatar)
                                            .circleCrop()
                                            .into(profileImage);
                                } catch (Exception ignored) {}
                            }
                            if (profileAvatarLetter != null)
                                profileAvatarLetter.setVisibility(View.INVISIBLE);
                        } else {
                            if (profileImage != null) profileImage.setVisibility(View.INVISIBLE);
                            if (profileAvatarLetter != null) {
                                profileAvatarLetter.setVisibility(View.VISIBLE);
                                profileAvatarLetter.setText(
                                        user.name != null && !user.name.isEmpty()
                                                ? String.valueOf(user.name.charAt(0)).toUpperCase() : "U");
                            }
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            uploadImageToSupabase(data.getData());
        }
    }

    private void uploadImageToSupabase(Uri imageUri) {
        Toast.makeText(this, "Enviando foto...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) return;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
                byte[] imageBytes = buffer.toByteArray();

                String fileName = "profile_" + myUid + ".jpg";
                String uploadUrl = SupabaseConfig.SUPABASE_URL
                        + "/storage/v1/object/" + SupabaseConfig.BUCKET + "/" + fileName;

                URL url = new URL(uploadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization",
                        "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "image/jpeg");
                conn.setRequestProperty("x-upsert", "true");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.getOutputStream().write(imageBytes);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    String publicUrl = SupabaseConfig.SUPABASE_URL
                            + "/storage/v1/object/public/" + SupabaseConfig.BUCKET + "/" + fileName;

                    mDatabase.child("users").child(myUid).child("photoUrl").setValue(publicUrl);

                    runOnUiThread(() -> {
                        if (profileImage != null) {
                            profileImage.setVisibility(View.VISIBLE);
                            try {
                                profileImage.startAnimation(
                                        AnimationUtils.loadAnimation(this, R.anim.scale_in));
                                Glide.with(this).load(publicUrl).circleCrop().into(profileImage);
                            } catch (Exception ignored) {}
                        }
                        if (profileAvatarLetter != null)
                            profileAvatarLetter.setVisibility(View.INVISIBLE);
                        Toast.makeText(this, "Foto atualizada! ✅", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erro ao enviar foto",
                                    Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZenegerApp.activityStarted();
        loadUserData();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZenegerApp.activityStopped();
    }
}