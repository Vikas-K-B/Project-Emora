import os

file_path = r'C:\Users\vikas\OneDrive\Documents\musicZ\app\src\main\java\com\example\genzmusicapp\MainActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Replace getString(R.string.default_web_client_id) with the hardcoded ID to prevent build issues
content = content.replace('getString(R.string.default_web_client_id)', '"445817305343-i4vfcjsttniaqvd94patok905r5um0hd.apps.googleusercontent.com"')

# 2. Add configureLoginScreen method
configure_login_screen = """
    private void configureLoginScreen(View view) {
        View btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                android.content.Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }
    }
"""

if 'private void configureLoginScreen(View view)' not in content:
    content = content.replace('private void configureWellnessScreen(View view) {', configure_login_screen + '\n    private void configureWellnessScreen(View view) {')

# 3. Call configureLoginScreen in showScreen inside the SCREEN_LOGIN case
if 'case SCREEN_LOGIN:' in content:
    # Need to find the case SCREEN_LOGIN: and inject the view configuration
    # Currently it looks like:
    #             case SCREEN_LOGIN:
    #                 currentContent = inflater.inflate(R.layout.screen_login, screenContainer, false);
    #                 break;
    # But wait, there is no R.layout.screen_login if I only just created it. Let's see how showScreen is structured.
    # Ah, I replaced case SCREEN_ONBOARDING: with case SCREEN_LOGIN:
    # Let's replace the inflating logic
    old_case = '''            case SCREEN_LOGIN:
                currentContent = inflater.inflate(R.layout.screen_onboarding, screenContainer, false);
                break;'''
    new_case = '''            case SCREEN_LOGIN:
                currentContent = inflater.inflate(R.layout.screen_login, screenContainer, false);
                configureLoginScreen(currentContent);
                break;'''
    
    if old_case in content:
        content = content.replace(old_case, new_case)
    else:
        # Maybe it's formatted differently
        import re
        content = re.sub(
            r'case SCREEN_LOGIN:\s+currentContent = inflater\.inflate\(R\.layout\.screen_onboarding, screenContainer, false\);\s+break;',
            r'case SCREEN_LOGIN:\n                currentContent = inflater.inflate(R.layout.screen_login, screenContainer, false);\n                configureLoginScreen(currentContent);\n                break;',
            content
        )

# 4. Also handle logout in configureProfileSettingsScreen or Profile hub
# Find where logout is handled in profile hub
# Usually there's a logout button
# Let's add a robust replace for logout
logout_logic = """
            android.widget.Toast.makeText(this, "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show();
            // Implement real logout logic
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                showScreen(SCREEN_LOGIN);
            });
"""
# Assuming the existing code is something like: Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
if 'Toast.makeText(this, "Logged out successfully"' in content:
    content = re.sub(r'Toast\.makeText\(this,\s*"Logged out successfully"[^;]+;', logout_logic, content)


with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainActivity refactored for auth.")
