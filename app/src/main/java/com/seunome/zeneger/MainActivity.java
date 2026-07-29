package com.seunome.zeneger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    EditText emailEditText, passwordEditText;
    Button loginButton;
    TextView registerLink;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        emailEditText    = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton      = findViewById(R.id.loginButton);
        registerLink     = findViewById(R.id.registerLink);

        // Animação de entrada
        View content = findViewById(android.R.id.content);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        content.startAnimation(fadeIn);

        loginButton.setOnClickListener(v -> {
            Animation bounce = AnimationUtils.loadAnimation(this, R.anim.bounce);
            v.startAnimation(bounce);

            String email = emailEditText.getText().toString().trim();
            String pass  = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                shake(emailEditText);
                shake(passwordEditText);
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                return;
            }

            loginButton.setEnabled(false);
            loginButton.setText("Entrando...");

            mAuth.signInWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(result -> {
                        Intent intent = new Intent(this, UsersActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        loginButton.setEnabled(true);
                        loginButton.setText("ENTRAR");
                        shake(emailEditText);
                        Toast.makeText(this, "Email ou senha incorretos", Toast.LENGTH_LONG).show();
                    });
        });

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void shake(View view) {
        android.view.animation.TranslateAnimation shake =
                new android.view.animation.TranslateAnimation(0, 20, 0, 0);
        shake.setDuration(80);
        shake.setRepeatCount(4);
        shake.setRepeatMode(android.view.animation.Animation.REVERSE);
        view.startAnimation(shake);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(this, UsersActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}