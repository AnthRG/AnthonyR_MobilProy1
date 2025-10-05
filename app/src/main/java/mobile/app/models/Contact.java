package mobile.app.models;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Contact {
    private String contactId;
    private String email;
    private String nickname;
    private String userId; // The actual Firebase user ID of this contact
    @ServerTimestamp
    private Date addedAt;

    public Contact() {}

    public Contact(String email, String nickname, String userId) {
        this.email = email;
        this.nickname = nickname;
        this.userId = userId;
    }

    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Date getAddedAt() { return addedAt; }
    public void setAddedAt(Date addedAt) { this.addedAt = addedAt; }


}