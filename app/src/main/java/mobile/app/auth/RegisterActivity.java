package mobile.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import mobile.app.MainActivity;
import mobile.app.R;

public class RegisterActivity extends AppCompatActivity {
    
    private static final String TAG = "RegisterActivity";

    private EditText emailField, passwordField, nameField;
    private Button registerButton;
    private TextView goLoginButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        nameField = findViewById(R.id.name);
        registerButton = findViewById(R.id.btn_register);
        goLoginButton = findViewById(R.id.btn_go_login);
        progressBar = findViewById(R.id.progress);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String name = nameField.getText().toString().trim();
            
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(RegisterActivity.this, "Email and password are required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Attempting to register user with email: " + email);
            
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Registration successful, user ID: " + user.getUid());
                            
                            // Create user document in Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", user.getUid());
                            userData.put("email", email.toLowerCase()); // Store email in lowercase
                            userData.put("displayName", name.isEmpty() ? email.split("@")[0] : name);
                            userData.put("status", "Hey there! I am using WhatsApp");
                            userData.put("online", true);
                            
                            Log.d(TAG, "Creating user document with email: " + email.toLowerCase());
                            
                            db.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "User document created successfully");
                                    // Register FCM token
                                    mobile.app.fcm.FCMTokenManager.registerTokenToFirestore();
                                    progressBar.setVisibility(View.GONE);
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Failed to create user document", e);
                                });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Registration failed", task.getException());
                        }
                    });
        });

        goLoginButton.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }
}