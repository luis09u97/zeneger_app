package com.seunome.zeneger;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import java.util.*;

public class CreateGroupActivity extends AppCompatActivity {

    EditText groupNameEditText;
    RecyclerView contactsRecycler;
    TextView createBtn;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    List<User> contactList = new ArrayList<>();
    Set<String> selectedIds = new HashSet<>();
    GroupMemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);
        PremiumUi.apply(this);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        try {
            findViewById(android.R.id.content)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception ignored) {}

        View backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        groupNameEditText = findViewById(R.id.groupNameEditText);
        createBtn         = findViewById(R.id.createGroupBtn);
        contactsRecycler  = findViewById(R.id.contactsRecycler);

        if (contactsRecycler != null) {
            contactsRecycler.setLayoutManager(new LinearLayoutManager(this));
            adapter = new GroupMemberAdapter(contactList, selectedIds);
            contactsRecycler.setAdapter(adapter);
        }

        loadContacts();

        if (createBtn != null) {
            createBtn.setOnClickListener(v -> {
                try {
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                } catch (Exception ignored) {}
                createGroup();
            });
        }
    }

    private void loadContacts() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("contacts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (ds.getKey() != null) ids.add(ds.getKey());
                        }

                        if (ids.isEmpty()) {
                            Toast.makeText(CreateGroupActivity.this,
                                    "Adicione contatos antes de criar um grupo",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        mDatabase.child("users").addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot usersSnap) {
                                        contactList.clear();
                                        for (String uid : ids) {
                                            try {
                                                User u = usersSnap.child(uid).getValue(User.class);
                                                if (u != null) contactList.add(u);
                                            } catch (Exception ignored) {}
                                        }
                                        if (adapter != null) adapter.notifyDataSetChanged();
                                    }
                                    @Override public void onCancelled(DatabaseError e) {}
                                });
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    private void createGroup() {
        if (groupNameEditText == null) return;
        String name = groupNameEditText.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Digite um nome para o grupo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedIds.size() < 1) {
            Toast.makeText(this, "Selecione pelo menos 1 contato", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;
        String myUid  = mAuth.getCurrentUser().getUid();
        String groupId = mDatabase.child("groups").push().getKey();
        if (groupId == null) return;

        Group group = new Group(groupId, name, myUid);
        Map<String, Boolean> members = new HashMap<>();
        members.put(myUid, true);
        for (String id : selectedIds) members.put(id, true);
        group.members = members;

        mDatabase.child("groups").child(groupId).setValue(group)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Grupo criado! 🎉", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro ao criar grupo: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
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
