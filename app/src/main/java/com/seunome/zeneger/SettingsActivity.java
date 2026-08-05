package com.seunome.zeneger;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    DatabaseReference mDatabase;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        PremiumUi.apply(this);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        prefs     = getSharedPreferences("zeneger_prefs", MODE_PRIVATE);

        try {
            findViewById(android.R.id.content)
                    .startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        } catch (Exception ignored) {}

        View backBtn = findViewById(R.id.backBtn);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        View profileOption  = findViewById(R.id.profileOption);
        View myProfileOption = findViewById(R.id.myProfileOption);
        View editNameOption = findViewById(R.id.editNameOption);
        View editBioOption  = findViewById(R.id.editBioOption);
        View editEmailOption    = findViewById(R.id.editEmailOption);
        View editPasswordOption = findViewById(R.id.editPasswordOption);
        View lastSeenOption     = findViewById(R.id.lastSeenOption);
        View blockedUsersOption = findViewById(R.id.blockedUsersOption);
        View wallpaperOption    = findViewById(R.id.wallpaperOption);
        View backupOption       = findViewById(R.id.backupOption);
        View clearCacheOption   = findViewById(R.id.clearCacheOption);
        View logoutOption       = findViewById(R.id.logoutOption);

        if (profileOption != null)
            profileOption.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));

        if (myProfileOption != null)
            myProfileOption.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));

        if (editNameOption != null)
            editNameOption.setOnClickListener(v -> showEditNameDialog());

        if (editBioOption != null)
            editBioOption.setOnClickListener(v -> showEditBioDialog());

        if (editEmailOption != null)
            editEmailOption.setOnClickListener(v -> showEditEmailDialog());

        if (editPasswordOption != null)
            editPasswordOption.setOnClickListener(v -> showEditPasswordDialog());

        if (lastSeenOption != null)
            lastSeenOption.setOnClickListener(v -> showLastSeenDialog());

        if (blockedUsersOption != null)
            blockedUsersOption.setOnClickListener(v ->
                    startActivity(new Intent(this, PrivacyActivity.class)));

        if (wallpaperOption != null)
            wallpaperOption.setOnClickListener(v -> showWallpaperDialog());

        if (backupOption != null)
            backupOption.setOnClickListener(v -> doBackup());

        if (clearCacheOption != null)
            clearCacheOption.setOnClickListener(v -> showClearCacheDialog());

        if (logoutOption != null)
            logoutOption.setOnClickListener(v -> showLogoutDialog());

        // Switches
        CompoundButton notifSwitch       = findViewById(R.id.notificationsSwitch);
        CompoundButton groupNotifSwitch  = findViewById(R.id.groupNotifSwitch);
        CompoundButton soundSwitch       = findViewById(R.id.soundSwitch);
        CompoundButton readReceiptSwitch = findViewById(R.id.readReceiptSwitch);
        CompoundButton themeModeSwitch   = findViewById(R.id.themeModeSwitch);

        if (themeModeSwitch != null) {
            boolean darkMode = (getResources().getConfiguration().uiMode
                    & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            themeModeSwitch.setChecked(darkMode);
            themeModeSwitch.setOnCheckedChangeListener((btn, checked) -> {
                prefs.edit().putBoolean("dark_mode_override", checked).apply();
                AppCompatDelegate.setDefaultNightMode(checked
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
            });
        }

        if (notifSwitch != null) {
            notifSwitch.setChecked(prefs.getBoolean("notifications_enabled", true));
            notifSwitch.setOnCheckedChangeListener((btn, checked) ->
                    prefs.edit().putBoolean("notifications_enabled", checked).apply());
        }
        if (groupNotifSwitch != null) {
            groupNotifSwitch.setChecked(prefs.getBoolean("group_notif_enabled", true));
            groupNotifSwitch.setOnCheckedChangeListener((btn, checked) ->
                    prefs.edit().putBoolean("group_notif_enabled", checked).apply());
        }
        if (soundSwitch != null) {
            soundSwitch.setChecked(prefs.getBoolean("sound_enabled", true));
            soundSwitch.setOnCheckedChangeListener((btn, checked) ->
                    prefs.edit().putBoolean("sound_enabled", checked).apply());
        }
        if (readReceiptSwitch != null) {
            readReceiptSwitch.setChecked(prefs.getBoolean("read_receipts", true));
            readReceiptSwitch.setOnCheckedChangeListener((btn, checked) ->
                    prefs.edit().putBoolean("read_receipts", checked).apply());
        }

        loadUserData();
        loadBlockedCount();
        calculateStorage();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user == null) return;

                        TextView nameView   = findViewById(R.id.settingsUserName);
                        TextView bioView    = findViewById(R.id.settingsUserBio);
                        TextView letterView = findViewById(R.id.settingsAvatarLetter);
                        CircleImageView avatarView = findViewById(R.id.settingsAvatar);

                        if (nameView != null)
                            nameView.setText(user.name != null ? user.name : "Usuário");
                        if (bioView != null)
                            bioView.setText(user.bio != null && !user.bio.isEmpty()
                                    ? user.bio : "Disponível");

                        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
                            if (avatarView != null) {
                                avatarView.setVisibility(View.VISIBLE);
                                try {
                                    Glide.with(SettingsActivity.this)
                                            .load(user.photoUrl)
                                            .placeholder(R.drawable.bg_avatar)
                                            .circleCrop()
                                            .into(avatarView);
                                } catch (Exception ignored) {}
                            }
                            if (letterView != null) letterView.setVisibility(View.GONE);
                        } else {
                            if (avatarView != null) avatarView.setVisibility(View.GONE);
                            if (letterView != null) {
                                letterView.setVisibility(View.VISIBLE);
                                letterView.setText(user.name != null && !user.name.isEmpty()
                                        ? String.valueOf(user.name.charAt(0)).toUpperCase() : "U");
                            }
                        }

                        TextView lastSeenValue = findViewById(R.id.lastSeenValue);
                        if (lastSeenValue != null)
                            lastSeenValue.setText(prefs.getString("last_seen_privacy", "Todos"));
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void loadBlockedCount() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();
        mDatabase.child("blocked").child(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long count = snapshot.getChildrenCount();
                        TextView blockedText = findViewById(R.id.blockedCountText);
                        if (blockedText != null) {
                            blockedText.setText(count == 0 ? "Nenhum bloqueado"
                                    : count + " contato" + (count > 1 ? "s" : "") + " bloqueado" + (count > 1 ? "s" : ""));
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void calculateStorage() {
        new Thread(() -> {
            try {
                long size = getDirSize(getCacheDir());
                long mb   = size / (1024 * 1024);
                String text = mb < 1 ? "< 1 MB em cache" : mb + " MB em cache";
                runOnUiThread(() -> {
                    TextView storageText = findViewById(R.id.storageText);
                    if (storageText != null) storageText.setText(text);
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private long getDirSize(java.io.File dir) {
        long size = 0;
        if (dir != null && dir.listFiles() != null) {
            for (java.io.File file : dir.listFiles()) {
                size += file.isDirectory() ? getDirSize(file) : file.length();
            }
        }
        return size;
    }

    private void showEditNameDialog() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_edit)
                .title("Alterar nome")
                .message("Digite seu novo nome de exibição")
                .input("Novo nome")
                .confirmInput("Salvar", (d, newName) -> {
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Nome não pode ser vazio", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mDatabase.child("users").child(myUid).child("name").setValue(newName)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Nome atualizado! ✅", Toast.LENGTH_SHORT).show();
                                d.dismiss();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .cancel("Cancelar")
                .show();
    }

    private void showEditBioDialog() {
        if (mAuth.getCurrentUser() == null) return;
        String myUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("users").child(myUid).child("bio")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snap) {
                        String bio = snap.getValue(String.class);
                        ZenegerDialog.Builder builder = ZenegerDialog.on(SettingsActivity.this)
                                .icon(R.drawable.ic_zen_edit)
                                .title("Alterar bio")
                                .message("Escreva algo sobre você")
                                .input("Sobre você...", InputType.TYPE_CLASS_TEXT
                                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                                .inputLines(3, 5)
                                .confirmInput("Salvar", (d, newBio) -> {
                                    mDatabase.child("users").child(myUid).child("bio").setValue(newBio)
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(SettingsActivity.this,
                                                        "Bio atualizada! ✅", Toast.LENGTH_SHORT).show();
                                                d.dismiss();
                                            });
                                })
                                .cancel("Cancelar");
                        if (bio != null) builder.prefill(bio);
                        builder.show();
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void showEditEmailDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_email)
                .title("Alterar email")
                .field("Novo email", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                .field("Senha atual (para confirmar)", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .confirmFields("Salvar", (d, fields) -> {
                    String newEmail = fields.get(0).getText().toString().trim();
                    String password = fields.get(1).getText().toString().trim();

                    if (newEmail.isEmpty() || password.isEmpty()) {
                        Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider.getCredential(
                            user.getEmail() != null ? user.getEmail() : "", password);

                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused ->
                                    user.updateEmail(newEmail)
                                            .addOnSuccessListener(unused2 -> {
                                                mDatabase.child("users").child(user.getUid())
                                                        .child("email").setValue(newEmail);
                                                Toast.makeText(this, "Email atualizado! ✅",
                                                        Toast.LENGTH_SHORT).show();
                                                d.dismiss();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Erro: " + e.getMessage(),
                                                            Toast.LENGTH_LONG).show()))
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Senha incorreta",
                                            Toast.LENGTH_SHORT).show());
                })
                .cancel("Cancelar")
                .show();
    }

    private void showEditPasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_lock)
                .title("Alterar senha")
                .field("Senha atual", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .field("Nova senha (mínimo 6 caracteres)", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                .confirmFields("Salvar", (d, fields) -> {
                    String currentPass = fields.get(0).getText().toString().trim();
                    String newPass     = fields.get(1).getText().toString().trim();

                    if (currentPass.isEmpty() || newPass.isEmpty()) {
                        Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPass.length() < 6) {
                        Toast.makeText(this, "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AuthCredential credential = EmailAuthProvider.getCredential(
                            user.getEmail() != null ? user.getEmail() : "", currentPass);

                    user.reauthenticate(credential)
                            .addOnSuccessListener(unused ->
                                    user.updatePassword(newPass)
                                            .addOnSuccessListener(unused2 -> {
                                                Toast.makeText(this, "Senha atualizada! ✅",
                                                        Toast.LENGTH_SHORT).show();
                                                d.dismiss();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Erro: " + e.getMessage(),
                                                            Toast.LENGTH_LONG).show()))
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Senha atual incorreta",
                                            Toast.LENGTH_SHORT).show());
                })
                .cancel("Cancelar")
                .show();
    }

    private void showLastSeenDialog() {
        String[] options = {"Todos", "Meus contatos", "Ninguém"};
        String current   = prefs.getString("last_seen_privacy", "Todos");
        int selected = 0;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(current)) { selected = i; break; }
        }

        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_visibility)
                .title("Quem pode ver sua última visualização?")
                .singleChoice(options, selected, (index, label) -> {
                    prefs.edit().putString("last_seen_privacy", label).apply();
                    TextView lastSeenValue = findViewById(R.id.lastSeenValue);
                    if (lastSeenValue != null) lastSeenValue.setText(label);
                })
                .show();
    }

    private void showWallpaperDialog() {
        String[] options = {"Padrão", "Claro", "Azul suave", "Cinza"};
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_theme)
                .title("Papel de parede")
                .items(options, (index, label) -> {
                    prefs.edit().putInt("wallpaper_choice", index).apply();
                    Toast.makeText(this, "Papel de parede alterado! ✅",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void doBackup() {
        if (mAuth.getCurrentUser() == null) return;
        Toast.makeText(this, "Iniciando backup...", Toast.LENGTH_SHORT).show();
        String myUid = mAuth.getCurrentUser().getUid();
        mDatabase.child("backups").child(myUid).child("lastBackup")
                .setValue(System.currentTimeMillis())
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Backup realizado com sucesso! ✅",
                                Toast.LENGTH_LONG).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erro no backup: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void showClearCacheDialog() {
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_storage)
                .title("Limpar cache")
                .message("Isso removerá arquivos temporários. Deseja continuar?")
                .confirm("Limpar", d -> {
                    try {
                        deleteDir(getCacheDir());
                        Toast.makeText(this, "Cache limpo! 🧹", Toast.LENGTH_SHORT).show();
                        calculateStorage();
                        d.dismiss();
                    } catch (Exception e) {
                        Toast.makeText(this, "Erro ao limpar cache", Toast.LENGTH_SHORT).show();
                    }
                })
                .cancel("Cancelar")
                .show();
    }

    private boolean deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) deleteDir(new java.io.File(dir, child));
            }
        }
        return dir != null && dir.delete();
    }

    private void showLogoutDialog() {
        ZenegerDialog.on(this)
                .icon(R.drawable.ic_zen_logout)
                .title("Sair da conta")
                .message("Tem certeza que deseja encerrar sua sessão no Zeneger?")
                .confirm("Sair", d -> {
                    try {
                        if (mAuth.getCurrentUser() != null) {
                            String uid = mAuth.getCurrentUser().getUid();
                            mDatabase.child("users").child(uid).child("online").setValue(false);
                            mDatabase.child("users").child(uid).child("lastSeen")
                                    .setValue(String.valueOf(System.currentTimeMillis()));
                            mDatabase.child("users").child(uid).child("fcmToken").removeValue();
                        }
                    } catch (Exception ignored) {}
                    mAuth.signOut();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .cancel("Cancelar")
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZenegerApp.activityStarted();
        loadUserData();
        loadBlockedCount();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ZenegerApp.activityStopped();
    }
}
