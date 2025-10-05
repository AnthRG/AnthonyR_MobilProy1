package mobile.app;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class WhatsAppApplication extends Application {
    
    private static final String TAG = "WhatsAppApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase initialized");
        
        // Configure Firestore settings for better offline support
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build();
        firestore.setFirestoreSettings(settings);
        Log.d(TAG, "Firestore configured with offline persistence");
        
        // Check if user is already logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            Log.d(TAG, "User already logged in: " + auth.getCurrentUser().getEmail());
        } else {
            Log.d(TAG, "No user logged in");
        }
    }
}
