package mobile.app.chat;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import mobile.app.R;
import mobile.app.models.Contact;

public class AddContactActivity extends AppCompatActivity {
    
    private static final String TAG = "AddContactActivity";

    private EditText emailField, nicknameField;
    private Button addButton;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add Contact");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        emailField = findViewById(R.id.contact_email);
        nicknameField = findViewById(R.id.contact_nickname);
        addButton = findViewById(R.id.btn_add_contact);
        progressBar = findViewById(R.id.progress);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        addButton.setOnClickListener(v -> addContact());
    }

    private void addContact() {
        String email = emailField.getText().toString().trim().toLowerCase(); // Convert to lowercase
        String nickname = nicknameField.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter an email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(nickname)) {
            Toast.makeText(this, "Please enter a nickname", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.equalsIgnoreCase(currentUserEmail)) {
            Toast.makeText(this, "You cannot add yourself as a contact", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        addButton.setEnabled(false);

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                addButton.setEnabled(true);

                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String foundUserId = document.getId();
                        // Check if the found user is not the current user
                        if (!foundUserId.equals(currentUserId)) {
                            checkIfContactExists(foundUserId, nickname, email);
                            return; // Exit after finding the first match
                        }
                    }
                    // If loop completes, it means only the current user was found
                    Toast.makeText(this, "You cannot add yourself as a contact.", Toast.LENGTH_SHORT).show();
                } else if (task.isSuccessful()) {
                    // Task was successful but no user was found
                    Toast.makeText(this, "No user found with this email.", Toast.LENGTH_SHORT).show();
                } else {
                    // Task failed
                    Log.w(TAG, "Error getting documents: ", task.getException());
                    Toast.makeText(this, "Error finding user: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void checkIfContactExists(String contactId, String nickname, String email) {
        db.collection("users").document(currentUserId)
            .collection("contacts").document(contactId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    Toast.makeText(this, "This user is already in your contacts.", Toast.LENGTH_SHORT).show();
                } else {
                    // Contact does not exist, proceed to add
                    addNewContact(contactId, nickname, email);
                }
            });
    }

    private void addNewContact(String contactId, String nickname, String email) {
        Map<String, Object> contactData = new HashMap<>();
        contactData.put("nickname", nickname);
        contactData.put("userId", contactId);
        contactData.put("email", email);

        progressBar.setVisibility(View.VISIBLE);
        addButton.setEnabled(false);

        db.collection("users").document(currentUserId)
            .collection("contacts").document(contactId)
            .set(contactData)
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                addButton.setEnabled(true);

                if (task.isSuccessful()) {
                    Toast.makeText(this, "Contact added successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the previous activity
                } else {
                    Log.w(TAG, "Error adding contact", task.getException());
                    Toast.makeText(this, "Failed to add contact: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}