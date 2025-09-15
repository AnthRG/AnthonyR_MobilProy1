package mobile.app.fcm;

import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
            String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (uid == null) return;

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            FirebaseFirestore.getInstance().collection("fcm_tokens").document(uid).set(data)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to Firestore"))
                    .addOnFailureListener(e -> Log.w(TAG, "Token save failed", e));
        });
    }
}
