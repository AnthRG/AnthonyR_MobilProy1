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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        
        Log.d(TAG, "Searching for user with email: " + email);

        // First, let's check all users to debug
        db.collection("users")
            .get()
            .addOnSuccessListener(allUsersSnapshot -> {
                Log.d(TAG, "Total users in database: " + allUsersSnapshot.size());
                
                String foundUserId = null;
                String foundEmail = null;
                
                for (QueryDocumentSnapshot doc : allUsersSnapshot) {
                    String userEmail = doc.getString("email");
                    Log.d(TAG, "Found user with email: " + userEmail);
                    
                    if (userEmail != null && userEmail.equalsIgnoreCase(email)) {
                        foundUserId = doc.getId();
                        foundEmail = userEmail;
                        Log.d(TAG, "Match found! User ID: " + foundUserId);
                        break;
                    }
                }
                
                if (foundUserId == null) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "No user found with email: " + email, Toast.LENGTH_LONG).show();
                    Log.e(TAG, "No user found with email: " + email);
                    return;
                }
                
                final String contactUserId = foundUserId;
                final String contactEmail = foundEmail;
                
                // Check if contact already exists
                db.collection("users").document(currentUserId)
                    .collection("contacts")
                    .whereEqualTo("email", contactEmail)
                    .get()
                    .addOnSuccessListener(contactSnapshots -> {
                        if (!contactSnapshots.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Contact already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Add contact to user's contacts collection
                        Contact contact = new Contact(contactEmail, nickname, contactUserId);
                        db.collection("users").document(currentUserId)
                            .collection("contacts")
                            .add(contact)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Contact added successfully");
                                // Now check if chat exists or create one
                                checkOrCreateChat(contactUserId, contactEmail, nickname);
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed to add contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to add contact", e);
                            });
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Error checking existing contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error checking existing contacts", e);
                    });
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error searching for user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error searching for user", e);
            });
    }

    private void checkOrCreateChat(String contactUserId, String contactEmail, String nickname) {
        Log.d(TAG, "Checking for existing chat with user: " + contactUserId);
        
        // Check if chat already exists between these two users
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                String existingChatId = null;
                
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    List<String> participants = (List<String>) doc.get("participants");
                    if (participants != null && participants.contains(contactUserId)) {
                        existingChatId = doc.getId();
                        Log.d(TAG, "Found existing chat: " + existingChatId);
                        break;
                    }
                }
                
                if (existingChatId != null) {
                    // Chat exists, open it
                    openChat(existingChatId, contactUserId, contactEmail, nickname);
                } else {
                    // Create new chat
                    Log.d(TAG, "Creating new chat");
                    createNewChat(contactUserId, contactEmail, nickname);
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Error creating chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error checking for existing chat", e);
            });
    }

    private void createNewChat(String contactUserId, String contactEmail, String nickname) {
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", Arrays.asList(currentUserId, contactUserId));
        chat.put("participantEmails", Arrays.asList(currentUserEmail, contactEmail));
        chat.put("lastMessage", "");
        chat.put("lastMessageTime", null);
        chat.put("lastMessageSenderId", "");
        
        Log.d(TAG, "Creating chat with participants: " + currentUserId + ", " + contactUserId);
        
        db.collection("chats")
            .add(chat)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Chat created successfully: " + documentReference.getId());
                openChat(documentReference.getId(), contactUserId, contactEmail, nickname);
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Failed to create chat: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Failed to create chat", e);
            });
    }

    private void openChat(String chatId, String contactUserId, String contactEmail, String nickname) {
        progressBar.setVisibility(View.GONE);
        Log.d(TAG, "Opening chat: " + chatId);
        
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", contactUserId);
        intent.putExtra("otherUserEmail", contactEmail);
        intent.putExtra("contactName", nickname);
        startActivity(intent);
        finish();
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