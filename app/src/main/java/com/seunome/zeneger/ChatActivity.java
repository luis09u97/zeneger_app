package com.seunome.zeneger;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import de.hdodenhof.circleimageview.CircleImageView;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText messageEditText;
    FrameLayout sendButton;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    String receiverId, receiverName, myUid, chatId;
    boolean isGroup = false;
    List<Message> messageList = new ArrayList<>();
    List<Message> allMessages = new ArrayList<>();
    List<String> messageKeys  = new ArrayList<>();
    ChatAdapter adapter;
    TextView typingIndicator;
    LinearLayout replyContainer;
    TextView replyTextView;
    String replyToText = null;
    android.os.Handler typingHandler = new android.os.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        PremiumUi.apply(this);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() == null) { finish(); return; }
        myUid = mAuth.getCurrentUser().getUid();

        isGroup = getIntent().getBooleanExtra("isGroup", false);

        TextView toolbarName   = findViewById(R.id.toolbarName);
        TextView toolbarAvatar = findViewById(R.id.toolbarAvatarLetter);
        TextView toolbarStatus = findViewById(R.id.toolbarStatus);
        View toolbarOnlineDot  = findViewById(R.id.toolbarOnlineDot);
        View optionsBtn        = findViewById(R.id.optionsBtn);

        if (isGroup) {
            chatId       = getIntent().getStringExtra("groupId");
            receiverName = getIntent().getStringExtra("groupName");
            if (toolbarAvatar != null) toolbarAvatar.setText("G");
            if (toolbarStatus != null) toolbarStatus.setText("Grupo");
        } else {
            receiverId   = getIntent().getStringExtra("receiverId");
            receiverName = getIntent().getStringExtra("receiverName");

            if (receiverId == null || receiverId.isEmpty()) { finish(); return; }

            chatId = myUid.compareTo(receiverId) < 0
                    ? myUid + "_" + receiverId
                    : receiverId + "_" + myUid;

            if (receiverName != null && !receiverName.isEmpty() && toolbarAvatar != null) {
                toolbarAvatar.setText(
                        String.valueOf(receiverName.charAt(0)).toUpperCase());
            }

            // Foto do destinatário
            String receiverPhoto = getIntent().getStringExtra("receiverPhoto");
            if (receiverPhoto != null && !receiverPhoto.isEmpty()) {
                CircleImageView avatarImg = findViewById(R.id.toolbarAvatar);
                if (avatarImg != null) {
                    avatarImg.setVisibility(View.VISIBLE);
                    if (toolbarAvatar != null) toolbarAvatar.setVisibility(View.INVISIBLE);
                    try {
                        Glide.with(this).load(receiverPhoto)
                                .circleCrop().placeholder(R.drawable.bg_avatar).into(avatarImg);
                    } catch (Exception ignored) {}
                }
            }

            // Status online
            mDatabase.child("users").child(receiverId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            if (user == null) return;
                            if (toolbarStatus != null) {
                                if (user.online) {
                                    toolbarStatus.setText("online");
                                    toolbarStatus.setTextColor(0xFF22C55E);
                                    if (toolbarOnlineDot != null)
                                        toolbarOnlineDot.setVisibility(View.VISIBLE);
                                } else if (user.lastSeen != null && !user.lastSeen.isEmpty()) {
                                    try {
                                        long ms = Long.parseLong(user.lastSeen);
                                        String fmt = new SimpleDateFormat("dd/MM HH:mm",
                                                Locale.getDefault()).format(new Date(ms));
                                        toolbarStatus.setText("visto " + fmt);
                                    } catch (Exception e) {
                                        toolbarStatus.setText("offline");
                                    }
                                    toolbarStatus.setTextColor(0xFFCCCCCC);
                                    if (toolbarOnlineDot != null)
                                        toolbarOnlineDot.setVisibility(View.GONE);
                                }
                            }
                        }
                        @Override public void onCancelled(DatabaseError e) {}
                    });
        }

        if (toolbarName != null) toolbarName.setText(receiverName);
        if (toolbarName != null) toolbarName.setOnClickListener(v -> openReceiverProfile());
        if (toolbarAvatar != null) toolbarAvatar.setOnClickListener(v -> openReceiverProfile());
        if (optionsBtn != null) optionsBtn.setOnClickListener(v -> showChatOptions());

        View videoCallBtn = findViewById(R.id.videoCallBtn);
        View callBtn      = findViewById(R.id.callBtn);
        if (videoCallBtn != null) videoCallBtn.setOnClickListener(v ->
                Toast.makeText(this, "Chamada de vídeo em breve!", Toast.LENGTH_SHORT).show());
        if (callBtn != null) callBtn.setOnClickListener(v ->
                Toast.makeText(this, "Chamada de voz em breve!", Toast.LENGTH_SHORT).show());

        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        recyclerView    = findViewById(R.id.messagesRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton      = findViewById(R.id.sendButton);
        typingIndicator = findViewById(R.id.typingIndicator);
        replyContainer  = findViewById(R.id.replyContainer);
        replyTextView   = findViewById(R.id.replyText);

        View cancelReplyBtn = findViewById(R.id.cancelReplyBtn);
        if (cancelReplyBtn != null) {
            cancelReplyBtn.setOnClickListener(v -> {
                replyToText = null;
                if (replyContainer != null) replyContainer.setVisibility(View.GONE);
            });
        }

        View attachBtn = findViewById(R.id.attachBtn);
        if (attachBtn != null) {
            attachBtn.setOnClickListener(v -> {
                try { v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce)); }
                catch (Exception ignored) {}
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 88);
            });
        }

        adapter = new ChatAdapter(messageList, myUid, (msgIndex, msgKey) ->
                showDeleteOptions(msgKey, msgIndex));

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        if (recyclerView != null) {
            recyclerView.setLayoutManager(lm);
            recyclerView.setAdapter(adapter);
        }

        if (sendButton != null) {
            sendButton.setOnClickListener(v -> {
                try { v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce)); }
                catch (Exception ignored) {}
                sendMessage();
            });
        }

        if (messageEditText != null) {
            messageEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (!isGroup && receiverId != null && chatId != null) {
                        mDatabase.child("typing").child(chatId).child(myUid).setValue(true);
                        typingHandler.removeCallbacksAndMessages(null);
                        typingHandler.postDelayed(() ->
                                        mDatabase.child("typing").child(chatId).child(myUid).setValue(false),
                                2000);
                    }
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        if (!isGroup && receiverId != null && chatId != null) {
            mDatabase.child("typing").child(chatId).child(receiverId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (typingIndicator == null) return;
                            Boolean isTyping = snapshot.getValue(Boolean.class);
                            typingIndicator.setVisibility(
                                    isTyping != null && isTyping ? View.VISIBLE : View.GONE);
                        }
                        @Override public void onCancelled(DatabaseError e) {}
                    });
        }

        if (chatId != null) {
            mDatabase.child("pinnedMessages").child(chatId)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String pinned = snapshot.getValue(String.class);
                            View pinnedContainer = findViewById(R.id.pinnedContainer);
                            TextView pinnedView  = findViewById(R.id.pinnedMessage);
                            if (pinnedContainer == null || pinnedView == null) return;
                            if (pinned != null && !pinned.isEmpty()) {
                                pinnedView.setText("📌 " + pinned);
                                pinnedContainer.setVisibility(View.VISIBLE);
                            } else {
                                pinnedContainer.setVisibility(View.GONE);
                            }
                        }
                        @Override public void onCancelled(DatabaseError e) {}
                    });
        }

        loadMessages();
    }

    private String getChatPath() {
        return isGroup ? "groupChats" : "chats";
    }

    private void openReceiverProfile() {
        if (isGroup || receiverId == null) return;
        Intent intent = new Intent(this, ViewProfileActivity.class);
        intent.putExtra("userId", receiverId);
        startActivity(intent);
    }

    private void showChatOptions() {
        try {
            android.app.Dialog dialog = new android.app.Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View view = LayoutInflater.from(this)
                    .inflate(R.layout.bottom_sheet_chat_options, null);
            PremiumUi.styleDynamic(view);
            dialog.setContentView(view);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(
                                android.graphics.Color.TRANSPARENT));
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setGravity(Gravity.BOTTOM);
                dialog.getWindow().getAttributes().windowAnimations =
                        android.R.style.Animation_InputMethod;
            }

            view.findViewById(R.id.optionSearch).setOnClickListener(v -> {
                dialog.dismiss();
                showSearchDialog();
            });

            view.findViewById(R.id.optionClear).setOnClickListener(v -> {
                dialog.dismiss();
                clearChat();
            });

            View optionBlock = view.findViewById(R.id.optionBlock);
            if (!isGroup && optionBlock != null) {
                optionBlock.setVisibility(View.VISIBLE);
                optionBlock.setOnClickListener(v -> {
                    dialog.dismiss();
                    blockUser();
                });
                TextView blockText = view.findViewById(R.id.blockText);
                if (blockText != null && receiverName != null)
                    blockText.setText("Bloquear " + receiverName);
            } else if (optionBlock != null) {
                optionBlock.setVisibility(View.GONE);
            }

            view.findViewById(R.id.optionCancel).setOnClickListener(v -> dialog.dismiss());
            dialog.show();

        } catch (Exception e) {
            String[] options = isGroup
                    ? new String[]{"Buscar mensagens", "Limpar conversa"}
                    : new String[]{"Buscar mensagens", "Limpar conversa",
                    "Bloquear " + receiverName};

            ZenegerDialog.on(this)
                    .icon(R.drawable.ic_zen_more)
                    .title("Opções da conversa")
                    .items(options, (index, label) -> {
                        if (isGroup) {
                            if (index == 0) showSearchDialog();
                            else if (index == 1) clearChat();
                        } else {
                            if (index == 0) showSearchDialog();
                            else if (index == 1) clearChat();
                            else if (index == 2) blockUser();
                        }
                    })
                    .show();
        }
    }

    private void showSearchDialog() {
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_search)
                .title("Buscar mensagens")
                .input("Buscar mensagem...")
                .confirmInput("Buscar", (d, query) -> {
                    searchMessages(query);
                    d.dismiss();
                })
                .cancel("Limpar", d -> searchMessages(""))
                .show();
    }

    private void searchMessages(String query) {
        if (query.isEmpty()) {
            messageList.clear();
            messageList.addAll(allMessages);
            adapter.notifyDataSetChanged();
            return;
        }
        messageList.clear();
        for (Message msg : allMessages) {
            if (msg.text != null && msg.text.toLowerCase().contains(query.toLowerCase())) {
                messageList.add(msg);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void clearChat() {
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_delete)
                .title("Limpar conversa")
                .message("Apagar todas as mensagens para você?")
                .confirm("Limpar", d -> {
                    if (chatId == null) return;
                    mDatabase.child(getChatPath()).child(chatId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    for (DataSnapshot ds : snapshot.getChildren()) {
                                        ds.getRef().child("deletedFor").child(myUid).setValue(true);
                                    }
                                    Toast.makeText(ChatActivity.this,
                                            "Conversa limpa!", Toast.LENGTH_SHORT).show();
                                    d.dismiss();
                                }
                                @Override public void onCancelled(DatabaseError e) {}
                            });
                })
                .cancel("Cancelar")
                .show();
    }

    private void blockUser() {
        if (receiverId == null) return;
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_block)
                .title("Bloquear " + receiverName + "?")
                .message("Você não receberá mais mensagens desta pessoa.")
                .confirm("Bloquear", d -> {
                    mDatabase.child("blocked").child(myUid).child(receiverId).setValue(true);
                    mDatabase.child("contacts").child(myUid).child(receiverId).removeValue();
                    Toast.makeText(this, receiverName + " bloqueado.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .cancel("Cancelar")
                .show();
    }

    private void showDeleteOptions(String messageKey, int position) {
        if (position < 0 || position >= messageList.size()) return;
        Message msg    = messageList.get(position);
        boolean isMine = msg.senderId != null && msg.senderId.equals(myUid);

        String[] options = isMine
                ? new String[]{"↩️ Responder", "😀 Reagir", "📌 Fixar",
                "🗑 Apagar para mim", "🗑 Apagar para todos"}
                : new String[]{"↩️ Responder", "😀 Reagir", "📌 Fixar",
                "🗑 Apagar para mim"};

        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_more)
                .title("Mensagem")
                .items(options, (index, label) -> {
                    if (isMine) {
                        if (index == 0) setReply(msg.text);
                        else if (index == 1) showReactionPicker(messageKey);
                        else if (index == 2) pinMessage(msg.text);
                        else if (index == 3) deleteForMe(messageKey);
                        else if (index == 4) deleteForAll(messageKey);
                    } else {
                        if (index == 0) setReply(msg.text);
                        else if (index == 1) showReactionPicker(messageKey);
                        else if (index == 2) pinMessage(msg.text);
                        else if (index == 3) deleteForMe(messageKey);
                    }
                })
                .show();
    }

    private void setReply(String text) {
        replyToText = text;
        if (replyTextView != null) replyTextView.setText("Respondendo: " + text);
        if (replyContainer != null) {
            replyContainer.setVisibility(View.VISIBLE);
            try {
                replyContainer.startAnimation(
                        AnimationUtils.loadAnimation(this, R.anim.slide_up));
            } catch (Exception ignored) {}
        }
    }

    private void showReactionPicker(String messageKey) {
        String[] emojis = {"❤️", "😂", "👍", "😮", "😢", "🔥"};
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_smile)
                .title("Reagir")
                .items(emojis, (index, emoji) -> {
                    if (chatId == null) return;
                    mDatabase.child(getChatPath()).child(chatId)
                            .child(messageKey).child("reactions")
                            .child(myUid).setValue(emoji);
                    Toast.makeText(this, "Reação adicionada!", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void pinMessage(String text) {
        if (chatId == null) return;
        mDatabase.child("pinnedMessages").child(chatId).setValue(text);
        Toast.makeText(this, "Mensagem fixada 📌", Toast.LENGTH_SHORT).show();
    }

    private void deleteForMe(String messageKey) {
        if (chatId == null) return;
        mDatabase.child(getChatPath()).child(chatId).child(messageKey)
                .child("deletedFor").child(myUid).setValue(true);
        Toast.makeText(this, "Mensagem apagada para você", Toast.LENGTH_SHORT).show();
    }

    private void deleteForAll(String messageKey) {
        if (chatId == null) return;
        mDatabase.child(getChatPath()).child(chatId).child(messageKey)
                .child("text").setValue("🚫 Mensagem apagada");
        mDatabase.child(getChatPath()).child(chatId).child(messageKey)
                .child("deletedForAll").setValue(true);
        Toast.makeText(this, "Mensagem apagada para todos", Toast.LENGTH_SHORT).show();
    }

    private void sendMessage() {
        if (messageEditText == null || chatId == null) return;
        String text = messageEditText.getText().toString().trim();
        if (text.isEmpty()) return;

        String timestamp = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date());

        Message message = new Message(myUid, text, timestamp);

        if (replyToText != null) {
            message.text = "↩️ " + replyToText + "\n\n" + text;
            replyToText  = null;
            if (replyContainer != null) replyContainer.setVisibility(View.GONE);
        }

        mDatabase.child(getChatPath()).child(chatId).push().setValue(message);
        messageEditText.setText("");

        if (isGroup) {
            mDatabase.child("groups").child(chatId).child("lastMessage").setValue(text);
            mDatabase.child("groups").child(chatId).child("lastMessageTime")
                    .setValue(String.valueOf(System.currentTimeMillis()));
        }

        mDatabase.child("typing").child(chatId).child(myUid).setValue(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 88 && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            uploadChatImage(data.getData());
        }
    }

    private void uploadChatImage(android.net.Uri imageUri) {
        Toast.makeText(this, "Enviando imagem...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                java.io.InputStream is = getContentResolver().openInputStream(imageUri);
                if (is == null) return;
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) buffer.write(buf, 0, n);
                byte[] imageBytes = buffer.toByteArray();

                String fileName  = "chat_" + myUid + "_" + System.currentTimeMillis() + ".jpg";
                String uploadUrl = SupabaseConfig.SUPABASE_URL
                        + "/storage/v1/object/" + SupabaseConfig.BUCKET + "/" + fileName;

                java.net.URL url = new java.net.URL(uploadUrl);
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization",
                        "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY);
                conn.setRequestProperty("Content-Type", "image/jpeg");
                conn.setRequestProperty("x-upsert", "true");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.getOutputStream().write(imageBytes);

                int code = conn.getResponseCode();
                if (code == 200 || code == 201) {
                    String publicUrl = SupabaseConfig.SUPABASE_URL
                            + "/storage/v1/object/public/"
                            + SupabaseConfig.BUCKET + "/" + fileName;

                    String timestamp = new SimpleDateFormat("HH:mm",
                            Locale.getDefault()).format(new Date());

                    Message message = new Message(myUid, publicUrl, timestamp);
                    mDatabase.child(getChatPath()).child(chatId).push().setValue(message);

                    runOnUiThread(() ->
                            Toast.makeText(this, "Imagem enviada! 📷",
                                    Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Erro ao enviar imagem",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void loadMessages() {
        if (chatId == null) return;

        mDatabase.child(getChatPath()).child(chatId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        messageList.clear();
                        allMessages.clear();
                        messageKeys.clear();

                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Message msg = ds.getValue(Message.class);
                                if (msg == null) continue;
                                if (ds.child("deletedFor").hasChild(myUid)) continue;

                                if (ds.child("reactions").exists()) {
                                    Map<String, String> reactions = new HashMap<>();
                                    for (DataSnapshot r : ds.child("reactions").getChildren()) {
                                        if (r.getKey() != null)
                                            reactions.put(r.getKey(), r.getValue(String.class));
                                    }
                                    msg.reactions = reactions;
                                }

                                allMessages.add(msg);
                                messageList.add(msg);
                                messageKeys.add(ds.getKey());
                            } catch (Exception ignored) {}
                        }

                        adapter.setKeys(messageKeys);
                        adapter.notifyDataSetChanged();

                        if (recyclerView != null && !messageList.isEmpty()) {
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }

                        markMessagesAsRead();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void markMessagesAsRead() {
        if (isGroup || chatId == null) return;
        mDatabase.child("chats").child(chatId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            try {
                                Message msg = ds.getValue(Message.class);
                                if (msg != null && msg.senderId != null
                                        && !msg.senderId.equals(myUid) && !msg.read) {
                                    ds.getRef().child("read").setValue(true);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                    @Override public void onCancelled(DatabaseError e) {}
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZenegerApp.activityStarted();
        markMessagesAsRead();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZenegerApp.activityStopped();
        if (chatId != null) {
            mDatabase.child("typing").child(chatId).child(myUid).setValue(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        typingHandler.removeCallbacksAndMessages(null);
    }
}
