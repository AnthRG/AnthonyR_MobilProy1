package mobile.app.auth;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import mobile.app.R;

public class FirebaseDiagnosticActivity extends AppCompatActivity {
    
    private static final String TAG = "FirebaseDiagnostic";
    private TextView diagnosticText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple layout with a TextView
        diagnosticText = new TextView(this);
        diagnosticText.setPadding(20, 20, 20, 20);
        setContentView(diagnosticText);
        
        runDiagnostics();
    }
    
    private void runDiagnostics() {
        StringBuilder report = new StringBuilder();
        report.append("Firebase Diagnostic Report\n");
        report.append("==========================\n\n");
        
        // Check Firebase App initialization
        try {
            FirebaseApp app = FirebaseApp.getInstance();
            report.append("✓ Firebase App initialized\n");
            report.append("  - Name: ").append(app.getName()).append("\n");
            report.append("  - Project ID: ").append(app.getOptions().getProjectId()).append("\n");
            report.append("  - Application ID: ").append(app.getOptions().getApplicationId()).append("\n");
            report.append("  - API Key: ").append(app.getOptions().getApiKey().substring(0, 10)).append("...\n\n");
        } catch (Exception e) {
            report.append("✗ Firebase App NOT initialized\n");
            report.append("  Error: ").append(e.getMessage()).append("\n\n");
            Log.e(TAG, "Firebase App not initialized", e);
        }
        
        // Check Firebase Auth
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            report.append("✓ Firebase Auth available\n");
            if (auth.getCurrentUser() != null) {
                report.append("  - Current user: ").append(auth.getCurrentUser().getEmail()).append("\n\n");
            } else {
                report.append("  - No user logged in\n\n");
            }
        } catch (Exception e) {
            report.append("✗ Firebase Auth error\n");
            report.append("  Error: ").append(e.getMessage()).append("\n\n");
            Log.e(TAG, "Firebase Auth error", e);
        }
        
        // Check Firestore
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            report.append("✓ Firestore instance created\n");
            
            // Try a simple read operation
            db.collection("test").document("test").get()
                .addOnSuccessListener(doc -> {
                    String currentText = diagnosticText.getText().toString();
                    diagnosticText.setText(currentText + "  - Firestore connection: SUCCESS\n");
                    Log.d(TAG, "Firestore connection successful");
                })
                .addOnFailureListener(e -> {
                    String currentText = diagnosticText.getText().toString();
                    diagnosticText.setText(currentText + "  - Firestore connection: FAILED\n" +
                            "    Error: " + e.getMessage() + "\n");
                    Log.e(TAG, "Firestore connection failed", e);
                });
                
        } catch (Exception e) {
            report.append("✗ Firestore error\n");
            report.append("  Error: ").append(e.getMessage()).append("\n\n");
            Log.e(TAG, "Firestore error", e);
        }
        
        // Check network connectivity
        report.append("Network Status:\n");
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) 
            getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        report.append("  - Connected: ").append(isConnected).append("\n");
        if (activeNetwork != null) {
            report.append("  - Type: ").append(activeNetwork.getTypeName()).append("\n");
            report.append("  - State: ").append(activeNetwork.getState()).append("\n");
        }
        
        diagnosticText.setText(report.toString());
        Log.d(TAG, report.toString());
    }
}