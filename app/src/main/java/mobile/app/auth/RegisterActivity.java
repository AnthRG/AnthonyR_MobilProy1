package mobile.app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import mobile.app.MainActivity;
import mobile.app.R;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailField, passwordField;
    private Button registerButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        registerButton = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress);

        mAuth = FirebaseAuth.getInstance();

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(RegisterActivity.this, "Email y contraseña son requeridos", Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Registrar token FCM en Firestore
                            mobile.app.fcm.FCMTokenManager.registerTokenToFirestore();
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registro fallido: " + task.getException(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
