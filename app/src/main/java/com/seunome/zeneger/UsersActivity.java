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
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import de.hdodenhof.circleimageview.CircleImageView;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class UsersActivity extends AppCompatActivity {

    RecyclerView recyclerView, storiesRecyclerView;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    List<Conversation> allConversations      = new ArrayList<>();
    List<Conversation> filteredConversations = new ArrayList<>();
    List<Story> storyList = new ArrayList<>();

    UserAdapter adapter;
    StoriesAdapter storiesAdapter;
    ValueEventListener contactsListener, groupsListener;
    boolean isLoggingOut = false;
    static final int PICK_STORY_IMAGE = 99;
    String currentFilter = "all";
    Set<String> favoriteIds = new HashSet<>();

    TextView tabChats, tabStories;
    LinearLayout chatsPanel, storiesPanel;
    TextView filterAll, filterUnread, filterGroups, filterFavorites;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);
        PremiumUi.apply(this);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() == null) { goToLogin(); return; }

        requestNotificationPermission();

        // Abas
        tabChats     = findViewById(R.id.tabChats);
        tabStories   = findViewById(R.id.tabStories);
        chatsPanel   = findViewById(R.id.chatsPanel);
        storiesPanel = findViewById(R.id.storiesPanel);

        if (tabChats != null)   tabChats.setOnClickListener(v -> switchTab(true));
        if (tabStories != null) tabStories.setOnClickListener(v -> switchTab(false));

        // Filtros
        filterAll       = findViewById(R.id.filterAll);
        filterUnread    = findViewById(R.id.filterUnread);
        filterGroups    = findViewById(R.id.filterGroups);
        filterFavorites = findViewById(R.id.filterFavorites);

        if (filterAll != null)       filterAll.setOnClickListener(v -> applyFilter("all"));
        if (filterUnread != null)    filterUnread.setOnClickListener(v -> applyFilter("unread"));
        if (filterGroups != null)    filterGroups.setOnClickListener(v -> applyFilter("groups"));
        if (filterFavorites != null) filterFavorites.setOnClickListener(v -> applyFilter("favorites"));

        // Botões toolbar
        View settingsBtn    = findViewById(R.id.settingsBtn);
        View addContactBtn  = findViewById(R.id.addContactBtn);
        View createGroupBtn = findViewById(R.id.createGroupBtn);
        View fabNewChat     = findViewById(R.id.fabNewChat);

        if (settingsBtn != null)
            settingsBtn.setOnClickListener(v ->
                    startActivity(new Intent(this, SettingsActivity.class)));

        if (addContactBtn != null)
            addContactBtn.setOnClickListener(v ->
                    startActivity(new Intent(this, AddContactActivity.class)));

        if (createGroupBtn != null)
            createGroupBtn.setOnClickListener(v ->
                    startActivity(new Intent(this, CreateGroupActivity.class)));

        if (fabNewChat != null) {
            fabNewChat.setOnClickListener(v -> {
                try {
                    v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));
                } catch (Exception ignored) {}
                startActivity(new Intent(this, AddContactActivity.class));
            });
        }

        // RecyclerView conversas
        recyclerView = findViewById(R.id.usersRecyclerView);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new UserAdapter(filteredConversations, conv -> {
                if (isLoggingOut || conv == null) return;
                openChat(conv);
            });
            adapter.setOnLongClickListener(this::toggleFavorite);
            recyclerView.setAdapter(adapter);
        }

        // RecyclerView stories
        storiesRecyclerView = findViewById(R.id.storiesRecyclerView);
        if (storiesRecyclerView != null) {
            storiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            storiesAdapter = new StoriesAdapter(storyList, story -> {
                if (story == null) return;
                Intent intent = new Intent(this, ViewStoryActivity.class);
                intent.putExtra("imageUrl", story.imageUrl != null ? story.imageUrl : "");
                intent.putExtra("userName", story.userName != null ? story.userName : "");
                intent.putExtra("timeAgo", story.getTimeAgo());
                startActivity(intent);
            });
            storiesRecyclerView.setAdapter(storiesAdapter);
        }

        // Meu story
        View myStoryBtn = findViewById(R.id.myStoryBtn);
        if (myStoryBtn != null) myStoryBtn.setOnClickListener(v -> pickStoryImage());

        // Busca
        EditText searchConversations = findViewById(R.id.searchConversations);
        if (searchConversations != null) {
            searchConversations.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterBySearch(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        loadFavorites();
        loadConversations();
        loadStories();
    }

    private void openChat(Conversation conv) {
        Intent intent = new Intent(this, ChatActivity.class);
        if (conv.type == Conversation.TYPE_GROUP) {
            if (conv.id == null || conv.id.isEmpty()) return;
            intent.putExtra("isGroup", true);
            intent.putExtra("groupId", conv.id);
            intent.putExtra("groupName", conv.name != null ? conv.name : "Grupo");
        } else {
            if (conv.id == null || conv.id.isEmpty()) return;
            intent.putExtra("isGroup", false);
            intent.putExtra("receiverId", conv.id);
            intent.putExtra("receiverName", conv.name != null ? conv.name : "");
            intent.putExtra("receiverPhoto", conv.photoUrl != null ? conv.photoUrl : "");
        }
        startActivity(intent);
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(token -> {
                        if (mAuth.getCurrentUser() != null && token != null) {
                            mDatabase.child("users")
                                    .child(mAuth.getCurrentUser().getUid())
                                    .child("fcmToken").setValue(token);
                        }
                    });
        } catch (Exception ignored) {}
    }

    private void filterBySearch(String query) {
        filteredConversations.clear();
        for (Conversation c : allConversations) {
            if (c == null) continue;
            c.favorite = favoriteIds.contains(c.id);
            boolean matchFilter = matchesFilter(c);
            boolean matchSearch = query.isEmpty() || (c.name != null &&
                    c.name.toLowerCase().contains(query.toLowerCase()));
            if (matchFilter && matchSearch) filteredConversations.add(c);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private boolean matchesFilter(Conversation c) {
        switch (currentFilter) {
            case "unread":    return c.unread;
            case "groups":    return c.type == Conversation.TYPE_GROUP;
            case "favorites": return c.favorite;
            default:          return true;
        }
    }

    private void toggleFavorite(Conversation conv) {
        if (conv == null || conv.id == null) return;
        String myUid     = mAuth.getCurrentUser().getUid();
        boolean newState = !conv.favorite;
        if (newState) {
            mDatabase.child("favorites").child(myUid).child(conv.id).setValue(true);
        } else {
            mDatabase.child("favorites").child(myUid).child(conv.id).removeValue();
        }
        Toast.makeText(this,
                newState ? "Adicionado aos favoritos ⭐" : "Removido dos favoritos",
                Toast.LENGTH_SHORT).show();
    }

    private void loadFavorites() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();
        mDatabase.child("favorites").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        favoriteIds.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            if (ds.getKey() != null) favoriteIds.add(ds.getKey());
                        }
                        rebuildList();
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    private void switchTab(boolean showChats) {
        if (chatsPanel == null || storiesPanel == null) return;
        if (showChats) {
            chatsPanel.setVisibility(View.VISIBLE);
            storiesPanel.setVisibility(View.GONE);
            if (tabChats != null)   { tabChats.setTextColor(0xFFFFFFFF); tabChats.setAlpha(1f); }
            if (tabStories != null) { tabStories.setTextColor(0x80FFFFFF); tabStories.setAlpha(0.7f); }
        } else {
            chatsPanel.setVisibility(View.GONE);
            storiesPanel.setVisibility(View.VISIBLE);
            if (tabChats != null)   { tabChats.setTextColor(0x80FFFFFF); tabChats.setAlpha(0.7f); }
            if (tabStories != null) { tabStories.setTextColor(0xFFFFFFFF); tabStories.setAlpha(1f); }
            loadStories();
        }
    }

    private void applyFilter(String filter) {
        currentFilter = filter;
        TextView[] chips = {filterAll, filterUnread, filterGroups, filterFavorites};
        String[]   keys  = {"all", "unread", "groups", "favorites"};
        for (int i = 0; i < chips.length; i++) {
            if (chips[i] == null) continue;
            if (keys[i].equals(filter)) {
                chips[i].setBackgroundResource(R.drawable.bg_chip_active);
                chips[i].setTextColor(0xFFFFFFFF);
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_chip_inactive);
                chips[i].setTextColor(getResources().getColor(R.color.text_secondary));
            }
        }
        rebuildList();
    }

    private void rebuildList() {
        filteredConversations.clear();
        for (Conversation c : allConversations) {
            if (c == null) continue;
            c.favorite = favoriteIds.contains(c.id);
            if (matchesFilter(c)) filteredConversations.add(c);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void pickStoryImage() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_STORY_IMAGE);
        } catch (Exception e) {
            Toast.makeText(this, "Erro ao abrir galeria", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_STORY_IMAGE && resultCode == Activity.RESULT_OK
                && data != null && data.getData() != null) {
            uploadStory(data.getData());
        }
    }

    private void uploadStory(Uri imageUri) {
        Toast.makeText(this, "Publicando story...", Toast.LENGTH_SHORT).show();
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        new Thread(() -> {
            try {
                InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) return;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
                byte[] imageBytes = buffer.toByteArray();

                String fileName  = "story_" + myUid + "_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = SupabaseConfig.SUPABASE_URL
                        + "/storage/v1/object/" + SupabaseConfig.BUCKET + "/" + fileName;

                URL url = new URL(uploadUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "image/jpeg");
                conn.setRequestProperty("x-upsert", "true");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.getOutputStream().write(imageBytes);

                int code = conn.getResponseCode();
                if (code == 200 || code == 201) {
                    String publicUrl = SupabaseConfig.SUPABASE_URL
                            + "/storage/v1/object/public/" + SupabaseConfig.BUCKET + "/" + fileName;

                    mDatabase.child("users").child(myUid).child("name")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snap) {
                                    String name = snap.getValue(String.class);
                                    Story story = new Story(myUid,
                                            name != null ? name : "Você",
                                            publicUrl,
                                            String.valueOf(System.currentTimeMillis()));
                                    mDatabase.child("stories").child(myUid).push().setValue(story);
                                    runOnUiThread(() -> {
                                        Toast.makeText(UsersActivity.this,
                                                "Story publicado! 🎉", Toast.LENGTH_SHORT).show();
                                        TextView sub = findViewById(R.id.myStorySubtitle);
                                        if (sub != null) sub.setText("Toque para atualizar");
                                        loadStories();
                                    });
                                }
                                @Override public void onCancelled(DatabaseError e) {}
                            });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Erro ao enviar story",
                                    Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void loadStories() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("contacts").child(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot contactsSnap) {
                        storyList.clear();
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot ds : contactsSnap.getChildren()) {
                            if (ds.getKey() != null) ids.add(ds.getKey());
                        }
                        ids.add(myUid);

                        for (String uid : ids) {
                            mDatabase.child("stories").child(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot snap) {
                                            for (DataSnapshot ds : snap.getChildren()) {
                                                try {
                                                    Story s = ds.getValue(Story.class);
                                                    if (s != null && !s.isExpired()) {
                                                        boolean exists = false;
                                                        for (Story ex : storyList) {
                                                            if (ex.uid != null && ex.uid.equals(s.uid)) {
                                                                exists = true; break;
                                                            }
                                                        }
                                                        if (!exists) storyList.add(s);
                                                    }
                                                } catch (Exception ignored) {}
                                            }
                                            if (storiesAdapter != null)
                                                storiesAdapter.notifyDataSetChanged();
                                        }
                                        @Override public void onCancelled(DatabaseError e) {}
                                    });
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    private void loadConversations() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        contactsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isLoggingOut) return;

                List<String> ids = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.getKey() != null) ids.add(ds.getKey());
                }

                mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot usersSnap) {
                        Iterator<Conversation> it = allConversations.iterator();
                        while (it.hasNext()) {
                            if (it.next().type == Conversation.TYPE_USER) it.remove();
                        }

                        for (String uid : ids) {
                            try {
                                User u = usersSnap.child(uid).getValue(User.class);
                                if (u == null || u.uid == null) continue;
                                Conversation c = new Conversation();
                                c.type     = Conversation.TYPE_USER;
                                c.id       = u.uid;
                                c.name     = u.name != null ? u.name : "Usuário";
                                c.photoUrl = u.photoUrl;
                                c.online   = u.online;
                                c.lastSeen = u.lastSeen;
                                c.unread   = false;
                                allConversations.add(c);
                                countUnread(c);
                            } catch (Exception ignored) {}
                        }
                        rebuildList();
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
            }
            @Override public void onCancelled(DatabaseError e) {}
        };
        mDatabase.child("contacts").child(myUid).addValueEventListener(contactsListener);

        groupsListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isLoggingOut) return;
                Iterator<Conversation> it = allConversations.iterator();
                while (it.hasNext()) {
                    if (it.next().type == Conversation.TYPE_GROUP) it.remove();
                }
                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Group g = ds.getValue(Group.class);
                        if (g == null || g.members == null || g.groupId == null) continue;
                        if (!g.members.containsKey(myUid)) continue;
                        Conversation c = new Conversation();
                        c.type            = Conversation.TYPE_GROUP;
                        c.id              = g.groupId;
                        c.name            = g.name != null ? g.name : "Grupo";
                        c.lastMessage     = g.lastMessage;
                        c.lastMessageTime = g.lastMessageTime;
                        c.unread          = false;
                        allConversations.add(c);
                    } catch (Exception ignored) {}
                }
                rebuildList();
            }
            @Override public void onCancelled(DatabaseError e) {}
        };
        mDatabase.child("groups").addValueEventListener(groupsListener);
    }

    private void countUnread(Conversation conv) {
        if (conv == null || conv.id == null || mAuth.getCurrentUser() == null) return;
        if (conv.type == Conversation.TYPE_GROUP) return;
        String myUid = mAuth.getCurrentUser().getUid();

        String chatId = myUid.compareTo(conv.id) < 0
                ? myUid + "_" + conv.id
                : conv.id + "_" + myUid;

        mDatabase.child("chats").child(chatId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int count = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Message msg = ds.getValue(Message.class);
                                if (msg != null && msg.senderId != null
                                        && !msg.senderId.equals(myUid) && !msg.read) {
                                    count++;
                                }
                            } catch (Exception ignored) {}
                        }
                        conv.unread      = count > 0;
                        conv.unreadCount = count;
                        rebuildList();
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    private void goToLogin() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isLoggingOut = true;
        if (mAuth.getCurrentUser() != null) {
            String myUid = mAuth.getCurrentUser().getUid();
            if (contactsListener != null)
                mDatabase.child("contacts").child(myUid).removeEventListener(contactsListener);
            if (groupsListener != null)
                mDatabase.child("groups").removeEventListener(groupsListener);
        }
    }
}
