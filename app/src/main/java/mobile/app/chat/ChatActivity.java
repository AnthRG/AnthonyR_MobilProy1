package mobile.app.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import mobile.app.R;

public class ChatActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1234;

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private EditText messageField;
    private ImageButton sendButton, imageButton;
    private ProgressBar progressBar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private CollectionReference messagesRef;
    private StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recycler_messages);
        messageField = findViewById(R.id.message_text);
        sendButton = findViewById(R.id.btn_send);
        imageButton = findViewById(R.id.btn_image);
        progressBar = findViewById(R.id.progress);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        messagesRef = db.collection("messages");
        storageRef = FirebaseStorage.getInstance().getReference().child("message_images");

        adapter = new ChatAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Listen for messages (simple query ordering by timestamp)
        messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots != null) {
                        adapter.setMessages(snapshots.toObjects(Message.class));
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    }
                });

        sendButton.setOnClickListener(v -> sendTextMessage());
        imageButton.setOnClickListener(v -> openImagePicker());
    }

    private void sendTextMessage() {
        String text = messageField.getText().toString().trim();
        if (text.isEmpty() || currentUser == null) return;
        progressBar.setVisibility(View.VISIBLE);
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", currentUser.getUid());
        data.put("senderEmail", currentUser.getEmail());
        data.put("text", text);
        data.put("timestamp", new Date());
        messagesRef.add(data).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                messageField.setText("");
            }
        });
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
        StorageReference fileRef = storageRef.child(System.currentTimeMillis() + "_img");
        UploadTask uploadTask = fileRef.putFile(imageUri);
        uploadTask.addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            Map<String, Object> data = new HashMap<>();
            data.put("senderId", currentUser.getUid());
            data.put("senderEmail", currentUser.getEmail());
            data.put("imageUrl", uri.toString());
            data.put("timestamp", new Date());
            messagesRef.add(data).addOnCompleteListener(task -> progressBar.setVisibility(View.GONE));
        })).addOnFailureListener(e -> progressBar.setVisibility(View.GONE));
    }
}
