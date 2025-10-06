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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import mobile.app.auth.LoginActivity;
import mobile.app.chat.ChatActivity;
import mobile.app.chat.ChatListAdapter;
import mobile.app.chat.SelectContactActivity;
import mobile.app.fcm.FCMTokenManager;
import mobile.app.models.Chat;
import mobile.app.models.Contact;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private TextView emptyView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration chatsListener;
    private ListenerRegistration contactsListener;
    private final Map<String, Contact> contactCache = new HashMap<>();
    private List<Chat> cachedChats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("NotWApp");
        }

        recyclerView = findViewById(R.id.recycler_chats);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fab = findViewById(R.id.fab_new_chat);

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
            Intent intent = new Intent(MainActivity.this, SelectContactActivity.class);
            startActivity(intent);
        });

        FCMTokenManager.registerTokenToFirestore();

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

        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .update("online", true)
                .addOnFailureListener(e -> android.util.Log.e(TAG, "Failed to set online status", e));

        loadChats();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .update("online", false, "lastSeen", new Date())
                    .addOnFailureListener(e -> android.util.Log.e(TAG, "Failed to set offline status", e));
        }
    }

    @Override
    protected void onDestroy() {
        detachChatListeners();
        super.onDestroy();
    }

    private void loadChats() {
        if (currentUser == null) {
            return;
        }

        detachChatListeners();

        String userId = currentUser.getUid();

        contactsListener = db.collection("users").document(userId)
                .collection("contacts")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        android.util.Log.w(TAG, "Contacts listener error", error);
                        return;
                    }

                    contactCache.clear();
                    for (QueryDocumentSnapshot contactDoc : snapshot) {
                        Contact contact = contactDoc.toObject(Contact.class);
                        if (contact != null && contact.getEmail() != null) {
                            contactCache.put(contact.getEmail().toLowerCase(Locale.ROOT), contact);
                        }
                    }

                    refreshChatsWithContacts();
                });

        chatsListener = db.collection("chats")
                .whereArrayContains("participants", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        android.util.Log.e(TAG, "Failed to listen for chats", error);
                        return;
                    }

                    List<Chat> chats = new ArrayList<>();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Chat chat = doc.toObject(Chat.class);
                            chat.setChatId(doc.getId());
                            enrichChatWithMetadata(chat);
                            chats.add(chat);
                        }
                    }

                    cachedChats = chats;
                    adapter.setChats(new ArrayList<>(cachedChats));
                    emptyView.setVisibility(cachedChats.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void refreshChatsWithContacts() {
        if (adapter == null) {
            return;
        }

        List<Chat> updatedChats = new ArrayList<>();
        for (Chat chat : cachedChats) {
            enrichChatWithMetadata(chat);
            updatedChats.add(chat);
        }

        cachedChats = updatedChats;
        adapter.setChats(new ArrayList<>(cachedChats));
        emptyView.setVisibility(cachedChats.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void enrichChatWithMetadata(Chat chat) {
        if (chat == null || currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();

        List<String> participants = chat.getParticipants();
        if (participants != null && participants.size() >= 2) {
            String first = participants.get(0);
            String second = participants.get(1);
            String otherUserId = first.equals(userId) ? second : first;
            chat.setOtherUserId(otherUserId);
        }

        String currentEmail = currentUser.getEmail();
        String otherEmail = chat.getOtherUserEmail();

        List<String> participantEmails = chat.getParticipantEmails();
        if (participantEmails != null && !participantEmails.isEmpty()) {
            for (String email : participantEmails) {
                if (email == null) {
                    continue;
                }
                if (currentEmail != null && currentEmail.equalsIgnoreCase(email)) {
                    continue;
                }
                otherEmail = email;
                break;
            }
            if (otherEmail == null) {
                otherEmail = participantEmails.get(0);
            }
        }

        if (otherEmail != null) {
            chat.setOtherUserEmail(otherEmail);
            Contact contact = contactCache.get(otherEmail.toLowerCase(Locale.ROOT));
            if (contact != null && contact.getNickname() != null && !contact.getNickname().isEmpty()) {
                chat.setContactName(contact.getNickname());
            } else if (otherEmail.contains("@")) {
                chat.setContactName(otherEmail.substring(0, otherEmail.indexOf('@')));
            } else {
                chat.setContactName(otherEmail);
            }
        } else if (chat.getContactName() == null || chat.getContactName().isEmpty()) {
            chat.setContactName("User");
        }
    }

    private void detachChatListeners() {
        if (contactsListener != null) {
            contactsListener.remove();
            contactsListener = null;
        }
        if (chatsListener != null) {
            chatsListener.remove();
            chatsListener = null;
        }
        contactCache.clear();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            detachChatListeners();
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}