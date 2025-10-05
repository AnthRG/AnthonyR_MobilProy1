package mobile.app.chat;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import mobile.app.R;
import mobile.app.models.Contact;

public class SelectContactActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ContactListAdapter adapter;
    private TextView emptyView;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_contact);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Select a Contact");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_contacts);
        emptyView = findViewById(R.id.empty_view);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new ContactListAdapter(this, this::onContactSelected);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        loadContacts();
    }

    private void loadContacts() {
        db.collection("users").document(currentUserId)
            .collection("contacts")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Contact> contacts = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Contact contact = document.toObject(Contact.class);
                    contact.setContactId(document.getId()); // Use the document ID for the contactId field
                    contacts.add(contact);
                }
                adapter.setContacts(contacts);
                emptyView.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
            });
    }

    private void onContactSelected(Contact contact) {
        // Here we need to find or create a chat between the two users
        findOrCreateChat(contact);
    }

    private void findOrCreateChat(Contact contact) {
        String otherUserId = contact.getUserId();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // A consistent chat ID for any two users
        String chatId = currentUserId.compareTo(otherUserId) > 0 ?
                        currentUserId + "_" + otherUserId :
                        otherUserId + "_" + currentUserId;

        db.collection("chats").document(chatId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                // Chat doesn't exist, create it
                List<String> participants = new ArrayList<>();
                participants.add(currentUserId);
                participants.add(otherUserId);

                List<String> participantEmails = new ArrayList<>();
                participantEmails.add(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                // We need the other user's email. We'll fetch it.
                db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
                    String otherUserEmail = userDoc.getString("email");
                    participantEmails.add(otherUserEmail);

                    mobile.app.models.Chat newChat = new mobile.app.models.Chat();
                    newChat.setChatId(chatId);
                    newChat.setParticipants(participants);
                    newChat.setParticipantEmails(participantEmails);
                    newChat.setLastMessageTime(new Date());
                    newChat.setLastMessage("");


                    db.collection("chats").document(chatId).set(newChat).addOnSuccessListener(aVoid -> {
                        openChatActivity(chatId, otherUserId, otherUserEmail, contact.getNickname());
                    });
                });
            } else {
                // Chat exists, just open it
                db.collection("users").document(otherUserId).get().addOnSuccessListener(userDoc -> {
                    String otherUserEmail = userDoc.getString("email");
                    openChatActivity(chatId, otherUserId, otherUserEmail, contact.getNickname());
                });
            }
        });
    }

    private void openChatActivity(String chatId, String otherUserId, String otherUserEmail, String contactName) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("otherUserId", otherUserId);
        intent.putExtra("otherUserEmail", otherUserEmail);
        intent.putExtra("contactName", contactName);
        startActivity(intent);
        finish(); // Finish this activity so back button goes to MainActivity
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_select_contact, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_add_contact) {
            startActivity(new Intent(this, AddContactActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }
}
