package mobile.app.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mobile.app.R;

public class ChatActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1234;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText messageField;
    private ImageButton sendButton, attachButton;
    private ProgressBar progressBar;
    private TextView onlineStatus;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    
    private String chatId;
    private String otherUserId;
    private String otherUserEmail;
    private String contactName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get chat info from intent
        chatId = getIntent().getStringExtra("chatId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserEmail = getIntent().getStringExtra("otherUserEmail");
        contactName = getIntent().getStringExtra("contactName");
        
        if (chatId == null || otherUserId == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Set title to contact name or email
        String displayName = contactName != null && !contactName.isEmpty() ? 
            contactName : (otherUserEmail != null ? otherUserEmail.split("@")[0] : "User");
        getSupportActionBar().setTitle(displayName);

        recyclerView = findViewById(R.id.recycler_messages);
        messageField = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.btn_send);
        attachButton = findViewById(R.id.btn_attach);
        progressBar = findViewById(R.id.progress);
        onlineStatus = findViewById(R.id.online_status);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference().child("chat_images");

        adapter = new ChatAdapter(this, currentUser.getUid());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Listen for messages in this specific chat
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener((snapshots, error) -> {
                if (error != null) return;
                if (snapshots != null) {
                    adapter.setMessages(snapshots.toObjects(Message.class));
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                }
            });

        // Listen for online status
        db.collection("users").document(otherUserId)
            .addSnapshotListener((snapshot, error) -> {
                if (error != null || snapshot == null) return;
                Boolean online = snapshot.getBoolean("online");
                if (online != null && online) {
                    onlineStatus.setText("online");
                    onlineStatus.setVisibility(View.VISIBLE);
                } else {
                    Date lastSeen = snapshot.getDate("lastSeen");
                    if (lastSeen != null) {
                        // Format last seen time
                        long diff = System.currentTimeMillis() - lastSeen.getTime();
                        String lastSeenText;
                        if (diff < 60000) {
                            lastSeenText = "last seen just now";
                        } else if (diff < 3600000) {
                            lastSeenText = "last seen " + (diff / 60000) + " min ago";
                        } else if (diff < 86400000) {
                            lastSeenText = "last seen " + (diff / 3600000) + " hours ago";
                        } else {
                            lastSeenText = "last seen " + (diff / 86400000) + " days ago";
                        }
                        onlineStatus.setText(lastSeenText);
                        onlineStatus.setVisibility(View.VISIBLE);
                    } else {
                        onlineStatus.setVisibility(View.GONE);
                    }
                }
            });

        sendButton.setOnClickListener(v -> sendTextMessage());
        attachButton.setOnClickListener(v -> openImagePicker());
        
        // Enable send button only when there's text
        messageField.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                sendButton.setEnabled(s.length() > 0);
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void sendTextMessage() {
        String text = messageField.getText().toString().trim();
        if (text.isEmpty() || currentUser == null) return;
        
        progressBar.setVisibility(View.VISIBLE);
        
        // Create message
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUser.getUid());
        message.put("text", text);
        message.put("timestamp", new Date());
        message.put("read", false);
        
        // Add to messages subcollection
        db.collection("chats").document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener(documentReference -> {
                // Update chat's last message
                updateLastMessage(text);
                messageField.setText("");
                progressBar.setVisibility(View.GONE);
            })
            .addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }

    private void updateLastMessage(String text) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", text);
        updates.put("lastMessageTime", new Date());
        updates.put("lastMessageSenderId", currentUser.getUid());
        
        db.collection("chats").document(chatId).update(updates);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageMessage(imageUri);
        }
    }

    private void uploadImageMessage(Uri imageUri) {
        if (currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);
        
        try {
            // Convert image to Base64
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                progressBar.setVisibility(View.GONE);
                return;
            }
            
            // Compress and convert to bitmap
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // Resize bitmap to reduce size (max width/height 800px)
            int maxDimension = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scale = Math.min(((float) maxDimension / width), ((float) maxDimension / height));
            
            if (scale < 1) {
                int newWidth = Math.round(width * scale);
                int newHeight = Math.round(height * scale);
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            
            // Convert to Base64
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
            
            // Create message with Base64 image
            Map<String, Object> message = new HashMap<>();
            message.put("senderId", currentUser.getUid());
            message.put("senderEmail", currentUser.getEmail());
            message.put("imageBase64", base64Image);
            message.put("timestamp", new Date());
            message.put("read", false);
            
            db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    updateLastMessage("📷 Photo");
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    android.widget.Toast.makeText(this, "Failed to send image", android.widget.Toast.LENGTH_SHORT).show();
                });
                
        } catch (Exception e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            android.widget.Toast.makeText(this, "Error processing image", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Mark messages as read
        markMessagesAsRead();
    }
    
    private void markMessagesAsRead() {
        db.collection("chats").document(chatId)
            .collection("messages")
            .whereEqualTo("senderId", otherUserId)
            .whereEqualTo("read", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentReference doc : queryDocumentSnapshots.getDocuments().stream()
                        .map(d -> d.getReference()).toArray(DocumentReference[]::new)) {
                    doc.update("read", true);
                }
            });
    }
}