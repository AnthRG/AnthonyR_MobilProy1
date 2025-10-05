package mobile.app.chat;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Message {
    private String senderId;
    private String senderEmail;
    private String text;
    private String imageUrl;
    private String imageBase64;
    private boolean read;
    @ServerTimestamp
    private Date timestamp;

    public Message() {}

    public String getSenderId() { return senderId; }
    public String getSenderEmail() { return senderEmail; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }
    public String getImageBase64() { return imageBase64; }
    public Date getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }

    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }
    public void setText(String text) { this.text = text; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public void setRead(boolean read) { this.read = read; }
}
