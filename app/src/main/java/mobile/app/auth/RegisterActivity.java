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
        
        // Check if user is already logged in
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "User already logged in, redirecting to MainActivity");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_register);

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        nameField = findViewById(R.id.name);
        registerButton = findViewById(R.id.btn_register);
        goLoginButton = findViewById(R.id.btn_go_login);
        progressBar = findViewById(R.id.progress);

        db = FirebaseFirestore.getInstance();

        goLoginButton.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

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
            registerButton.setEnabled(false);
            goLoginButton.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Create user document in Firestore
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("uid", firebaseUser.getUid());
                                userData.put("email", email.toLowerCase());
                                userData.put("displayName", name.isEmpty() ? email.split("@")[0] : name);
                                userData.put("status", "Hey there! I am using WhatsApp.");
                                userData.put("online", true);

                                db.collection("users").document(firebaseUser.getUid())
                                        .set(userData)
                                        .addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                Log.d(TAG, "User document created in Firestore.");
                                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                finish();
                                            } else {
                                                Log.w(TAG, "Failed to create user document in Firestore.", dbTask.getException());
                                                Toast.makeText(RegisterActivity.this, "Registration succeeded, but failed to save profile.", Toast.LENGTH_LONG).show();
                                                // Rollback Auth user creation if Firestore fails
                                                firebaseUser.delete().addOnCompleteListener(deleteTask -> {
                                                    if (deleteTask.isSuccessful()) {
                                                        Log.d(TAG, "Auth user rolled back successfully.");
                                                    } else {
                                                        Log.w(TAG, "Failed to roll back Auth user.", deleteTask.getException());
                                                    }
                                                });
                                            }
                                        });
                            } else {
                                // This case is unlikely but good to handle
                                resetUI("Registration failed: User not found.", null);
                            }
                        } else {
                            resetUI("Registration failed: " + task.getException().getMessage(), task.getException());
                        }
                    });
        });
    }

    private void resetUI(String message, Exception e) {
        progressBar.setVisibility(View.GONE);
        registerButton.setEnabled(true);
        goLoginButton.setEnabled(true);
        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
        if (e != null) {
            Log.e(TAG, message, e);
        } else {
            Log.e(TAG, message);
        }
    }
}