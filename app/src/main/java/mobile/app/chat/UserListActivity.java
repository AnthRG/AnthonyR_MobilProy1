package mobile.app.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mobile.app.R;
import mobile.app.models.User;

public class UserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserListAdapter adapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Select contact");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_users);
        
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        adapter = new UserListAdapter(this, user -> {
            // Check if chat already exists or create new one
            checkOrCreateChat(user);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<User> users = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    User user = doc.toObject(User.class);
                    user.setUid(doc.getId());
                    // Don't show current user in the list
                    if (!user.getUid().equals(currentUserId)) {
                        users.add(user);
                    }
                }
                adapter.setUsers(users);
            });
    }

    private void checkOrCreateChat(User otherUser) {
        // Check if chat already exists between these two users
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                String existingChatId = null;
                
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    List<String> participants = (List<String>) doc.get("participants");
                    if (participants != null && participants.contains(otherUser.getUid())) {
                        existingChatId = doc.getId();
                        break;
                    }
                }
                
                if (existingChatId != null) {
                    // Chat exists, open it
                    openChat(existingChatId, otherUser.getUid(), otherUser.getEmail());
                } else {
                    // Create new chat
                    createNewChat(otherUser);
                }
            });
    }

    private void createNewChat(User otherUser) {
        Map<String, Object> chat = new HashMap<>();
        chat.put("participants", Arrays.asList(currentUserId, otherUser.getUid()));
        chat.put("participantEmails", Arrays.asList(currentUserEmail, otherUser.getEmail()));
        chat.put("lastMessage", "");
        chat.put("lastMessageTime", null);
        chat.put("lastMessageSenderId", "");
        
        db.collection("chats")
            .add(chat)
            .addOnSuccessListener(documentReference -> {
                openChat(documentReference.getId(), otherUser.getUid(), otherUser.getEmail());
            });
    }

    private void openChat(String chatId, String otherUserId, String otherUserEmail) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserEmail", otherUserEmail);
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