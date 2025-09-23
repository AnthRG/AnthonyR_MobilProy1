package mobile.app.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import mobile.app.R;
import mobile.app.models.Chat;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(Chat chat);
    }

    private List<Chat> chats = new ArrayList<>();
    private Context context;
    private OnChatClickListener listener;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ChatListAdapter(Context context, OnChatClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_list, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Chat chat = chats.get(position);
        
        // Set contact name or email
        String displayName = chat.getContactName() != null && !chat.getContactName().isEmpty() ? 
            chat.getContactName() : (chat.getOtherUserEmail() != null ? 
            chat.getOtherUserEmail().split("@")[0] : "User");
        holder.userName.setText(displayName);
        
        // Set last message
        String lastMessage = chat.getLastMessage();
        if (lastMessage != null && !lastMessage.isEmpty()) {
            holder.lastMessage.setText(lastMessage);
            holder.lastMessage.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessage.setVisibility(View.GONE);
        }
        
        // Set time
        Date lastMessageTime = chat.getLastMessageTime();
        if (lastMessageTime != null) {
            long diff = System.currentTimeMillis() - lastMessageTime.getTime();
            String timeText;
            if (diff < 86400000) { // Less than 24 hours
                timeText = timeFormat.format(lastMessageTime);
            } else {
                timeText = dateFormat.format(lastMessageTime);
            }
            holder.time.setText(timeText);
            holder.time.setVisibility(View.VISIBLE);
        } else {
            holder.time.setVisibility(View.GONE);
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        TextView lastMessage;
        TextView time;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.user_name);
            lastMessage = itemView.findViewById(R.id.last_message);
            time = itemView.findViewById(R.id.time);
        }
    }
}