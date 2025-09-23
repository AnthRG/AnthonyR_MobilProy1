package mobile.app;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;

public class WhatsAppApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Sign out user on app start to require fresh login
        FirebaseAuth.getInstance().signOut();
    }
}