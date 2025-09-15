package mobile.app.chat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import mobile.app.R;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private List<Message> messages = new ArrayList<>();
    private Context ctx;

    public ChatAdapter(Context ctx) { this.ctx = ctx; }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Message m = messages.get(position);
        holder.sender.setText(m.getSenderEmail() != null ? m.getSenderEmail() : "Anon");
        if (m.getText() != null) {
            holder.text.setVisibility(View.VISIBLE);
            holder.text.setText(m.getText());
        } else {
            holder.text.setVisibility(View.GONE);
        }
        if (m.getImageUrl() != null) {
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(ctx).load(m.getImageUrl()).into(holder.image);
        } else {
            holder.image.setVisibility(View.GONE);
        }
        Date d = m.getTimestamp();
        holder.time.setText(d != null ? DateFormat.getDateTimeInstance().format(d) : "");
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView sender, text, time;
        ImageView image;
        VH(@NonNull View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.msg_sender);
            text = itemView.findViewById(R.id.msg_text);
            time = itemView.findViewById(R.id.msg_time);
            image = itemView.findViewById(R.id.msg_image);
        }
    }
}
