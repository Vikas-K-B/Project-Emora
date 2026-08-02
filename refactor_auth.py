import os

file_path = r'C:\Users\vikas\OneDrive\Documents\musicZ\app\src\main\java\com\example\genzmusicapp\MainActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add imports
imports = """
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
"""

content = content.replace('import androidx.appcompat.app.AppCompatActivity;', 'import androidx.appcompat.app.AppCompatActivity;\n' + imports)

# 2. Rename SCREEN_ONBOARDING to SCREEN_LOGIN
content = content.replace('private static final String SCREEN_ONBOARDING = "onboarding";', 'private static final String SCREEN_LOGIN = "login";')
content = content.replace('case SCREEN_ONBOARDING:', 'case SCREEN_LOGIN:')
content = content.replace('showScreen(SCREEN_ONBOARDING);', 'showScreen(SCREEN_LOGIN);')

# 3. Add variables
variables = """
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
"""
content = content.replace('private ActivityResultLauncher<String> pickImageLauncher;', 'private ActivityResultLauncher<String> pickImageLauncher;' + variables)

# 4. Initialize in onCreate
on_create_init = """
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("445817305343-a7210542ba6b375013b4ca.apps.googleusercontent.com") // We don't have the web client ID, using string trick or default, wait, I need the actual Web Client ID. Wait, I will use getString(R.string.default_web_client_id)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
"""

content = content.replace('pickImageLauncher = registerForActivityResult(', on_create_init + '\n        pickImageLauncher = registerForActivityResult(')

# Update R.string.default_web_client_id
content = content.replace('requestIdToken("445817305343-a7210542ba6b375013b4ca.apps.googleusercontent.com")', 'requestIdToken(getString(R.string.default_web_client_id))')

# 5. firebaseAuthWithGoogle method
firebase_auth_method = """
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        showScreen(SCREEN_HOME);
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
"""
content = content.replace('private void setupBottomNavigation() {', firebase_auth_method + '\n    private void setupBottomNavigation() {')

# 6. Check user in onCreate
# Find this:
#        if (SCREEN_SPLASH.equals(currentScreen)) {
#            showScreen(SCREEN_HOME);
#        }

check_user = """
        if (SCREEN_SPLASH.equals(currentScreen)) {
            if (mAuth.getCurrentUser() != null) {
                showScreen(SCREEN_HOME);
            } else {
                showScreen(SCREEN_LOGIN);
            }
        }
"""
content = content.replace('if (SCREEN_SPLASH.equals(currentScreen)) {\n            showScreen(SCREEN_HOME);\n        }', check_user)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainActivity refactored.")
