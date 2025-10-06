package mobile.app.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import mobile.app.MainActivity;
import mobile.app.R;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        FCMTokenManager.saveTokenToFirestore(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);
        
        // Get data from message
        String title = message.getData().get("title");
        String body = message.getData().get("body");
        String chatId = message.getData().get("chatId");
        String otherUserId = message.getData().get("otherUserId");
        String otherUserEmail = message.getData().get("otherUserEmail");
        String contactName = message.getData().get("contactName");

        if (message.getNotification() != null) {
            if (title == null) {
                title = message.getNotification().getTitle();
            }
            if (body == null) {
                body = message.getNotification().getBody();
            }
        }
        
        if (title == null) title = "Nuevo mensaje";
        if (body == null) body = "Tienes un nuevo mensaje";
        
        sendNotification(title, body, chatId, otherUserId, otherUserEmail, contactName);
    }

    private void sendNotification(String title, String messageBody, String chatId, String otherUserId, String otherUserEmail, String contactName) {
        Intent intent;
        
        // If we have chat details, open the chat directly
        if (chatId != null && otherUserId != null) {
            intent = new Intent(this, mobile.app.chat.ChatActivity.class);
            intent.putExtra("chatId", chatId);
            intent.putExtra("otherUserId", otherUserId);
            intent.putExtra("otherUserEmail", otherUserEmail);
            intent.putExtra("contactName", contactName);
        } else {
            // Otherwise open main activity
            intent = new Intent(this, MainActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                chatId != null ? chatId.hashCode() : messageBody.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String channelId = "chat_messages";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Chat messages", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((chatId != null ? chatId : String.valueOf(System.currentTimeMillis())).hashCode(), notificationBuilder.build());
    }
}
