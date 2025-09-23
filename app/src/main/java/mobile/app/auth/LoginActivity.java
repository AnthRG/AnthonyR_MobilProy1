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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import mobile.app.MainActivity;
import mobile.app.R;

public class LoginActivity extends AppCompatActivity {
    
    private static final String TAG = "LoginActivity";

    private EditText emailField, passwordField;
    private Button loginButton;
    private TextView goRegisterButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        loginButton = findViewById(R.id.btn_login);
        goRegisterButton = findViewById(R.id.btn_go_register);
        progressBar = findViewById(R.id.progress);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Email and password are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG, "Attempting login with email: " + email);
            
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Log.d(TAG, "Login successful for user: " + user.getUid());
                            
                            // Always update/create user document to ensure email is stored
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("uid", user.getUid());
                            userData.put("email", email.toLowerCase()); // Store email in lowercase
                            userData.put("online", true);
                            
                            // Check if user document exists to preserve displayName and status
                            db.collection("users").document(user.getUid()).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (!documentSnapshot.exists()) {
                                        Log.d(TAG, "Creating new user document");
                                        // New user document
                                        userData.put("displayName", email.split("@")[0]);
                                        userData.put("status", "Hey there! I am using WhatsApp");
                                    } else {
                                        Log.d(TAG, "Updating existing user document");
                                        // Preserve existing displayName and status if they exist
                                        String existingName = documentSnapshot.getString("displayName");
                                        String existingStatus = documentSnapshot.getString("status");
                                        if (existingName != null) userData.put("displayName", existingName);
                                        if (existingStatus != null) userData.put("status", existingStatus);
                                    }
                                    
                                    // Set or update the document
                                    db.collection("users").document(user.getUid())
                                        .set(userData, SetOptions.merge())
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "User document updated successfully");
                                            // Register FCM token
                                            mobile.app.fcm.FCMTokenManager.registerTokenToFirestore();
                                            progressBar.setVisibility(View.GONE);
                                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            progressBar.setVisibility(View.GONE);
                                            Toast.makeText(LoginActivity.this, "Failed to update user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                            Log.e(TAG, "Failed to update user document", e);
                                        });
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(LoginActivity.this, "Failed to check user profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    Log.e(TAG, "Failed to check user document", e);
                                });
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Login failed", task.getException());
                        }
                    });
        });

        goRegisterButton.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }
}