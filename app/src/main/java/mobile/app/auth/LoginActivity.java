package mobile.app.auth;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.google.firebase.firestore.Source;

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
        
        // Check if user is already logged in
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User already logged in, redirecting to MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_login);

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        loginButton = findViewById(R.id.btn_login);
        goRegisterButton = findViewById(R.id.btn_go_register);
        progressBar = findViewById(R.id.progress);

        db = FirebaseFirestore.getInstance();

        goRegisterButton.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "Email and password are required", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (!isNetworkAvailable()) {
                Toast.makeText(LoginActivity.this, "No internet connection.", Toast.LENGTH_LONG).show();
                return;
            }
            
            progressBar.setVisibility(View.VISIBLE);
            loginButton.setEnabled(false);
            goRegisterButton.setEnabled(false);
            
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // Always re-enable buttons and hide progress bar in the end
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        goRegisterButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful.");
                            // Sync user data to Firestore before proceeding
                            syncUserDocumentOnLogin(task.getResult().getUser());
                        } else {
                            Log.w(TAG, "Login failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }

    private void syncUserDocumentOnLogin(FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().exists()) {
                    // User document doesn't exist, create it
                    Log.d(TAG, "User document not found, creating one for " + user.getEmail());
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", user.getUid());
                    userData.put("email", user.getEmail().toLowerCase());
                    userData.put("displayName", user.getEmail().split("@")[0]);
                    userData.put("status", "Hey there! I am using WhatsApp");
                    userData.put("online", true);

                    db.collection("users").document(user.getUid()).set(userData)
                        .addOnCompleteListener(setTask -> {
                            if (setTask.isSuccessful()) {
                                Log.d(TAG, "User document created on login.");
                            } else {
                                Log.w(TAG, "Failed to create user document on login.", setTask.getException());
                            }
                            // Proceed whether it fails or not, as auth succeeded
                            proceedToMainActivity();
                        });
                } else if (!task.isSuccessful()) {
                    Log.w(TAG, "Failed to check for user document.", task.getException());
                    proceedToMainActivity(); // Proceed anyway
                } else {
                    // User document exists, just proceed
                    Log.d(TAG, "User document already exists.");
                    proceedToMainActivity();
                }
            });
    }

    private void proceedToMainActivity() {
        // Register FCM token after successful login
        mobile.app.fcm.FCMTokenManager.registerTokenToFirestore();
        
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}