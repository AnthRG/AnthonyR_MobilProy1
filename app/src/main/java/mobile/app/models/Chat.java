package mobile.app.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class Chat {
    private String chatId;
    private List<String> participants;
    private List<String> participantEmails;
    private String lastMessage;
    private String lastMessageSenderId;
    @ServerTimestamp
    private Date lastMessageTime;
    private String otherUserId; // Transient field for UI
    private String otherUserEmail; // Transient field for UI
    private String contactName; // Transient field for UI

    public Chat() {}

    public String getChatId() { return chatId; }
    public void setChatId(String chatId) { this.chatId = chatId; }

    public List<String> getParticipants() { return participants; }
    public void setParticipants(List<String> participants) { this.participants = participants; }

    public List<String> getParticipantEmails() { return participantEmails; }
    public void setParticipantEmails(List<String> participantEmails) { this.participantEmails = participantEmails; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) { this.lastMessageSenderId = lastMessageSenderId; }

    public Date getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Date lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) { this.otherUserId = otherUserId; }

    public String getOtherUserEmail() { return otherUserEmail; }
    public void setOtherUserEmail(String otherUserEmail) { this.otherUserEmail = otherUserEmail; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
}