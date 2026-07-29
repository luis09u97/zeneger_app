package com.seunome.zeneger;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class PrivacyActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        prefs     = getSharedPreferences("zeneger_prefs", MODE_PRIVATE);

        try {
            findViewById(android.R.id.content)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception ignored) {}

        View backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        View lastSeenOption = findViewById(R.id.lastSeenPrivacyOption);
        if (lastSeenOption != null)
            lastSeenOption.setOnClickListener(v -> showLastSeenDialog());

        View profilePhotoOption = findViewById(R.id.profilePhotoPrivacyOption);
        if (profilePhotoOption != null)
            profilePhotoOption.setOnClickListener(v -> showProfilePhotoDialog());

        updatePrivacyValues();
        loadBlockedUsers();
    }

    private void updatePrivacyValues() {
        TextView lastSeenValue = findViewById(R.id.lastSeenPrivacyValue);
        if (lastSeenValue != null)
            lastSeenValue.setText(prefs.getString("last_seen_privacy", "Todos"));

        TextView profilePhotoValue = findViewById(R.id.profilePhotoValue);
        if (profilePhotoValue != null)
            profilePhotoValue.setText(prefs.getString("profile_photo_privacy", "Todos"));
    }

    private void showLastSeenDialog() {
        String[] options = {"Todos", "Meus contatos", "Ninguém"};
        String current   = prefs.getString("last_seen_privacy", "Todos");
        int selected = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(current)) { selected = i; break; }
        }
        new AlertDialog.Builder(this)
                .setTitle("Última visualização")
                .setSingleChoiceItems(options, selected, (d, which) -> {
                    prefs.edit().putString("last_seen_privacy", options[which]).apply();
                    updatePrivacyValues();
                    d.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showProfilePhotoDialog() {
        String[] options = {"Todos", "Meus contatos", "Ninguém"};
        String current   = prefs.getString("profile_photo_privacy", "Todos");
        int selected = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(current)) { selected = i; break; }
        }
        new AlertDialog.Builder(this)
                .setTitle("Foto de perfil")
                .setSingleChoiceItems(options, selected, (d, which) -> {
                    prefs.edit().putString("profile_photo_privacy", options[which]).apply();
                    updatePrivacyValues();
                    d.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadBlockedUsers() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        RecyclerView blockedRecycler = findViewById(R.id.blockedRecycler);
        TextView emptyText = findViewById(R.id.emptyBlockedText);
        if (blockedRecycler == null) return;

        List<String[]> blockedList = new ArrayList<>();
        blockedRecycler.setLayoutManager(new LinearLayoutManager(this));

        mDatabase.child("blocked").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        blockedList.clear();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            if (emptyText != null) emptyText.setVisibility(View.VISIBLE);
                            blockedRecycler.setAdapter(null);
                            return;
                        }

                        if (emptyText != null) emptyText.setVisibility(View.GONE);

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String blockedUid = ds.getKey();
                            if (blockedUid == null) continue;

                            mDatabase.child("users").child(blockedUid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot userSnap) {
                                            try {
                                                User user = userSnap.getValue(User.class);
                                                if (user != null) {
                                                    blockedList.add(new String[]{
                                                            user.uid,
                                                            user.name != null ? user.name : "Usuário"
                                                    });
                                                    blockedRecycler.setAdapter(
                                                            buildBlockedAdapter(blockedList, myUid));
                                                }
                                            } catch (Exception ignored) {}
                                        }
                                        @Override public void onCancelled(DatabaseError e) {}
                                    });
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    private RecyclerView.Adapter<RecyclerView.ViewHolder> buildBlockedAdapter(
            List<String[]> list, String myUid) {

        return new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(
                    android.view.ViewGroup parent, int viewType) {
                android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                TextView text1 = holder.itemView.findViewById(android.R.id.text1);
                TextView text2 = holder.itemView.findViewById(android.R.id.text2);
                if (text1 != null) text1.setText(list.get(position)[1]);
                if (text2 != null) text2.setText("Toque para desbloquear");

                holder.itemView.setOnClickListener(v -> {
                    String uid  = list.get(position)[0];
                    String name = list.get(position)[1];
                    showUnblockDialog(uid, name, myUid);
                });
            }

            @Override
            public int getItemCount() { return list.size(); }
        };
    }

    private void showUnblockDialog(String uid, String name, String myUid) {
        new AlertDialog.Builder(this)
                .setTitle("Desbloquear " + name + "?")
                .setMessage(name + " poderá enviar mensagens para você novamente.")
                .setPositiveButton("Desbloquear", (d, w) ->
                        mDatabase.child("blocked").child(myUid).child(uid).removeValue()
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, name + " desbloqueado! ✅",
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZenegerApp.activityStarted();
        updatePrivacyValues();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZenegerApp.activityStopped();
    }
}