package mobile.app.fcm;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";

    public static void registerTokenToFirestore() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                return;
            }

            String token = task.getResult();
            saveTokenToFirestore(token);
        });
    }

    public static void saveTokenToFirestore(String token) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Attempted to save empty FCM token");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.w(TAG, "No authenticated user to associate FCM token with");
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("fcmToken", token);
        data.put("lastTokenRefresh", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved/updated for user"))
                .addOnFailureListener(e -> Log.w(TAG, "Failed to persist FCM token", e));
    }
}
