package mobile.app.chat;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import mobile.app.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private List<Message> messages = new ArrayList<>();
    private Context context;
    private String currentUserId;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatAdapter(Context context, String currentUserId) {
        this.context = context;
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message_whatsapp, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isCurrentUser = message.getSenderId().equals(currentUserId);
        
        // Set message alignment
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.messageCard.getLayoutParams();
        if (isCurrentUser) {
            params.gravity = Gravity.END;
            params.setMargins(100, 8, 16, 8);
            holder.messageCard.setCardBackgroundColor(context.getColor(R.color.message_sent_bg));
        } else {
            params.gravity = Gravity.START;
            params.setMargins(16, 8, 100, 8);
            holder.messageCard.setCardBackgroundColor(context.getColor(R.color.message_received_bg));
        }
        holder.messageCard.setLayoutParams(params);
        
        // Set message content
        if (message.getText() != null) {
            holder.messageText.setVisibility(View.VISIBLE);
            holder.messageText.setText(message.getText());
            holder.messageImage.setVisibility(View.GONE);
        } else if (message.getImageBase64() != null) {
            // Handle Base64 image
            holder.messageImage.setVisibility(View.VISIBLE);
            try {
                byte[] decodedBytes = android.util.Base64.decode(message.getImageBase64(), android.util.Base64.DEFAULT);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.messageImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.messageImage.setImageResource(R.drawable.ic_image_placeholder);
            }
            holder.messageText.setVisibility(View.GONE);
        } else if (message.getImageUrl() != null) {
            // Handle URL image 
            holder.messageImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(message.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.messageImage);
            holder.messageText.setVisibility(View.GONE);
        }
        
        // Set time
        Date timestamp = message.getTimestamp();
        if (timestamp != null) {
            holder.messageTime.setText(timeFormat.format(timestamp));
        }
        
        // Show read status for sent messages
        if (isCurrentUser) {
            holder.readStatus.setVisibility(View.VISIBLE);
            if (message.isRead()) {
                holder.readStatus.setText("✓✓");
                holder.readStatus.setTextColor(context.getColor(R.color.read_blue));
            } else {
                holder.readStatus.setText("✓");
                holder.readStatus.setTextColor(context.getColor(R.color.sent_grey));
            }
        } else {
            holder.readStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        CardView messageCard;
        TextView messageText;
        ImageView messageImage;
        TextView messageTime;
        TextView readStatus;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
            messageText = itemView.findViewById(R.id.message_text);
            messageImage = itemView.findViewById(R.id.message_image);
            messageTime = itemView.findViewById(R.id.message_time);
            readStatus = itemView.findViewById(R.id.read_status);
        }
    }
}