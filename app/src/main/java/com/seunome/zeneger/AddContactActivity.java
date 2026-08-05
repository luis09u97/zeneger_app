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

public class AddContactActivity extends AppCompatActivity {

    EditText searchEditText;
    TextView searchBtn;
    RecyclerView recyclerView;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    List<User> resultList = new ArrayList<>();
    SearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);
        PremiumUi.apply(this);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        try {
            findViewById(android.R.id.content)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception ignored) {}

        View backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        searchEditText = findViewById(R.id.searchEditText);
        searchBtn      = findViewById(R.id.searchBtn);
        recyclerView   = findViewById(R.id.searchResultsRecycler);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new SearchAdapter(resultList, this::addContact);
            recyclerView.setAdapter(adapter);
        }

        if (searchBtn != null) {
            searchBtn.setOnClickListener(v -> {
                try {
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                } catch (Exception ignored) {}
                searchUsers();
            });
        }

        if (searchEditText != null) {
            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                searchUsers();
                return true;
            });
        }
    }

    private void searchUsers() {
        if (searchEditText == null) return;
        String query = searchEditText.getText().toString().trim().toLowerCase();
        if (query.isEmpty()) {
            Toast.makeText(this, "Digite um nome ou email", Toast.LENGTH_SHORT).show();
            return;
        }

        String myUid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "";

        resultList.clear();
        if (adapter != null) adapter.notifyDataSetChanged();

        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                resultList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        User user = ds.getValue(User.class);
                        if (user == null || user.uid == null || user.uid.equals(myUid)) continue;
                        String name  = user.name  != null ? user.name.toLowerCase()  : "";
                        String email = user.email != null ? user.email.toLowerCase() : "";
                        if (name.contains(query) || email.contains(query)) {
                            resultList.add(user);
                        }
                    } catch (Exception ignored) {}
                }
                if (adapter != null) adapter.notifyDataSetChanged();
                if (resultList.isEmpty()) {
                    Toast.makeText(AddContactActivity.this,
                            "Nenhum usuário encontrado", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void addContact(User user) {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();
        mDatabase.child("contacts").child(myUid).child(user.uid).setValue(true);
        mDatabase.child("contacts").child(user.uid).child(myUid).setValue(true);
        Toast.makeText(this, user.name + " adicionado! ✅", Toast.LENGTH_SHORT).show();
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
