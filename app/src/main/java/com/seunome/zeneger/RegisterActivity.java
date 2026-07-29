package com.seunome.zeneger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class RegisterActivity extends AppCompatActivity {

    EditText nameEditText, emailEditText, passwordEditText;
    Button registerButton;
    TextView loginLink;
    FirebaseAuth mAuth;
    DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        nameEditText     = findViewById(R.id.nameEditText);
        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton   = findViewById(R.id.registerButton);
        loginLink        = findViewById(R.id.loginLink);

        View content = findViewById(android.R.id.content);
        content.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));

        registerButton.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.bounce));

            String name  = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String pass  = passwordEditText.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.length() < 6) {
                Toast.makeText(this, "Senha deve ter pelo menos 6 caracteres",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            registerButton.setEnabled(false);
            registerButton.setText("Criando conta...");

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(result -> {
                        String uid = mAuth.getCurrentUser().getUid();
                        User user  = new User(uid, name, email);

                        mDatabase.child("users").child(uid).setValue(user)
                                .addOnSuccessListener(unused -> {
                                    Intent intent = new Intent(this, UsersActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    registerButton.setEnabled(true);
                                    registerButton.setText("CADASTRAR");
                                    Toast.makeText(this, "Erro ao salvar dados: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        registerButton.setEnabled(true);
                        registerButton.setText("CADASTRAR");
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        loginLink.setOnClickListener(v -> finish());
    }
}