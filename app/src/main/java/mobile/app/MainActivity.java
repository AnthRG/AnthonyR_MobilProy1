package mobile.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobile.app.auth.LoginActivity;
import mobile.app.chat.AddContactActivity;
import mobile.app.chat.ChatActivity;
import mobile.app.chat.ChatListAdapter;
import mobile.app.models.Chat;
import mobile.app.models.Contact;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private TextView emptyView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        
        // Check if user is signed in (no auto-remember, so this should be null on app restart)
        currentUser = mAuth.getCurrentUser();
        
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("WhatsApp");
        
        recyclerView = findViewById(R.id.recycler_chats);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fab = findViewById(R.id.fab_new_chat);
        
        db = FirebaseFirestore.getInstance();
        
        adapter = new ChatListAdapter(this, chat -> {
            Intent intent = new Intent(MainActivity.this, ChatActivity.class);
            intent.putExtra("chatId", chat.getChatId());
            intent.putExtra("otherUserId", chat.getOtherUserId());
            intent.putExtra("otherUserEmail", chat.getOtherUserEmail());
            intent.putExtra("contactName", chat.getContactName());
            startActivity(intent);
        });
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, mobile.app.chat.SelectContactActivity.class);
            startActivity(intent);
        });
        
        loadChats();
        
        // Register FCM token
        mobile.app.fcm.FCMTokenManager.registerTokenToFirestore();
        
        // Request notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Re-check authentication on resume
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Set user as online
        db.collection("users").document(currentUser.getUid())
            .update("online", true)
            .addOnFailureListener(e -> android.util.Log.e("MainActivity", "Failed to set online status", e));
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Set user as offline when app goes to background
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                .update("online", false, "lastSeen", new Date())
                .addOnFailureListener(e -> android.util.Log.e("MainActivity", "Failed to set offline status", e));
        }
    }    private void loadChats() {
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        
        db.collection("users").document(userId)
            .collection("contacts")
            .get()
            .addOnSuccessListener(contactSnapshots -> {
                db.collection("chats")
                    .whereArrayContains("participants", userId)
                    .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) return;
                        
                        List<Chat> chats = new ArrayList<>();
                        if (snapshots != null) {
                            for (QueryDocumentSnapshot doc : snapshots) {
                                Chat chat = doc.toObject(Chat.class);
                                chat.setChatId(doc.getId());
                                
                                // Set the other user's info
                                List<String> participants = chat.getParticipants();
                                if (participants != null && participants.size() == 2) {
                                    String otherUserId = participants.get(0).equals(userId) ? 
                                        participants.get(1) : participants.get(0);
                                    chat.setOtherUserId(otherUserId);
                                    
                                    // Get other user's email from participantEmails
                                    List<String> emails = chat.getParticipantEmails();
                                    if (emails != null && emails.size() == 2) {
                                        String otherEmail = emails.get(0).equals(currentUser.getEmail()) ?
                                            emails.get(1) : emails.get(0);
                                        chat.setOtherUserEmail(otherEmail);
                                        
                                        // Find contact name for this email
                                        for (QueryDocumentSnapshot contactDoc : contactSnapshots) {
                                            Contact contact = contactDoc.toObject(Contact.class);
                                            if (contact.getEmail() != null && contact.getEmail().equals(otherEmail)) {
                                                chat.setContactName(contact.getNickname());
                                                break;
                                            }
                                        }
                                        
                                        // If no contact name found, use email prefix
                                        if (chat.getContactName() == null) {
                                            chat.setContactName(otherEmail.split("@")[0]);
                                        }
                                    }
                                }
                                
                                chats.add(chat);
                            }
                        }
                        
                        adapter.setChats(chats);
                        emptyView.setVisibility(chats.isEmpty() ? View.VISIBLE : View.GONE);
                    });
            });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}