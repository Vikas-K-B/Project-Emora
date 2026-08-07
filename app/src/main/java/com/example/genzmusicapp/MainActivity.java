package com.example.genzmusicapp;

import android.annotation.SuppressLint;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.core.content.ContextCompat;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.app.PendingIntent;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.app.ActivityCompat;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.response.ReadRecordsResponse;
import androidx.health.connect.client.time.TimeRangeFilter;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.genzmusicapp.recommendation.RecommendationEngine;
import com.example.genzmusicapp.recommendation.RecommendationManager;
import com.example.genzmusicapp.recommendation.model.PlaylistResponse;
import com.example.genzmusicapp.recommendation.model.SongRecommendation;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SCREEN_SPLASH = "splash";
    private static final String SCREEN_LOGIN = "login";
    private static final String SCREEN_HOME = "home";
    private static final String SCREEN_LIVE = "live";
    private static final String SCREEN_PLAYER = "player";
    private static final String SCREEN_SETTINGS = "settings";
    private static final String SCREEN_WELLNESS = "wellness";
    private static final String SCREEN_PREFERENCES = "preferences";
    private static final String SCREEN_MAIN_SETTINGS = "main_settings";
    private static final String SCREEN_PROFILE = "profile";
    private static final String SCREEN_PROFILE_SETTINGS = "profile_settings";
    private static final String SCREEN_HEALTH_CONNECT = "health_connect";
    private static final String SCREEN_PRIVACY = "privacy";

    private static final String DEVICE_IMAGE_URL =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCwprrlfC4teTJ4gg_c0-xg_Ff94DbAN7TBpUFuDMPAeILcbS_2n4xN-e2PNM2hjeEba6SO9Fuv-CsOQ5dG2V034xMWo_3hUEbo75Ui-6Lc53Q2PF8hlj72yG6L89d6u_wHU-T6-piKm_IPGhKvp5gjUquChSZhNKjxSijVhmdmk__GPkuSWZdtvE2TXhlC4iaFdKKjX0CFjzdHvUsf2T5e9gWvrAUeFkk8mts6gRVvg5xhkkDvDN_Q9OqOZa0wPbvdiyOBAKjCoDQ";
    private static final String HOME_AVATAR_URL =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuAZ5XV2o4sdyhxkrklSEi_dr3HLZk7ji6-3XwMT-VG9hp6EfOlqi7edYY_i3i27fa_SsN2m5cgLA6AxSlkF0E3pU85pgx8ghgsvtDEkWwhsR5PtAivB10X6mH3dNwV-r3dTCm6VTvlb-U-JAyw2feSgGIhQx-kbvRcOfsPYgie0CLSr0i2C5eXpzBR1uw0yk4yiX6yZAhwO7CCQJeMIS5XEFAYuD6VFtbZphdQtv4En4l8VLKwdQWAfihFuT61yNmewbYPXJT6ZpPQ";
    private static final String LIVE_AVATAR_URL =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCctReJW_jX4H3EeK6umNq3JjY6DY-IRFnGhVTPe3FilYePSeyQdJeXC0EfpGQkQETbS4NUkxo8rCzbk-Xom6sVv4nhHB3-q8x6M9TlR4uoD_hZW5LtQhPsDIEMjcIwYhvxck4-v2k3u58ja80kHxK_GGys80u_xHZwdbjI69PRNvRPxoaGaAy37Tv_05dX7skoi5q2JSVzUTVxbNWhm2DWuWoZp-sFF2pz2k2McWX-ry6Tlzzl7hHefiI4XDNRwO51Svz9bWv8GV8";
    private static final String PLAYER_AVATAR_URL =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBDEwtNL4vHxXZtGz2yOFqrojNDD_5G7BVb2-wNZo6q-F5HsywRD4g4ZnKv4uX4EXxp8bMrpuRl1IWqBDpqGs4xJYfe-apO2jiqD506TOnwa-URRT53_DXisftAsSj3FouBjogG7Y7O76UVZLS_p6v5icjFdQm6d9qwH3swLyvjqFohGGyLkvyQxaPN56FhD09H3-N8wAK8QQ50zAo_KmP9IF99CSYxBy_-6Ok8ScAfSZnRp7CY3FZCD6S1ZNnUpDOza8qCQspIyXs";
    private static final String WELLNESS_AVATAR_URL =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuBXp1-_AyaWfBLpHX89dMU81U-8P74CQLgkn0_3xF17pySLCPkOvJrJbW4wOfQ_pdY1Qk9EpI26kC5Z7opT9QawcIBaHhW1BHhD7kbXYnovdxuAOlVrmVR_CrM44nap4oBg4Wq1Ze9AONX66xFwnD25DnmnEeiki93NZoFNb7TxMfoBTQo9Cr_YBVjyhJTh0UR6nTrxvHqEhryB35pA3EBuvm7qzLAY1rf9y4o9WFlRqtdNcDok1Rm_Sh9DZjRuCgnbRtaBvBKJMpw";
    private static final String ALBUM_URL =
            "https://lh3.googleusercontent.com/aida-public/AB6AXuCOLs25-7Tbysor_s0Dn7w4b8-Y--t2qVknGdecHxoLP6f3rz0WlDR8e30XSOPxgUWEH3ZpHgGo4wPDk-bI2YQaqd_AEzt8Q3Vj5Cow56Nxy5HX5wL38_JWOUenZDpARZlf6fzyXyG1jz3Q08PQByaTMjG1EHWA_kCYNoI2rvykkXW0gfy-bTps3nQ89I13ZGwl61lV_GVsN28mZ0utJm5hebT54xC5I3LwjReM2JwbZ9jiC_IUcPz-mDpEO3OhTDNz7hQGujp0PCY";
    private static final int BLE_SCAN_TIMEOUT_MS = 60000;
    private static final int BLE_LIST_REFRESH_MS = 1000;
    private static final UUID BLE_STANDARD_HEART_RATE_SERVICE_UUID =
            UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private static final UUID BLE_STANDARD_HEART_RATE_MEASUREMENT_UUID =
            UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    private static final UUID BLE_CUSTOM_LIVE_SERVICE_UUID =
            UUID.fromString("000001ff-3c17-d293-8e48-14fe2e4da212");
    private static final UUID BLE_CUSTOM_NOTIFY_CHARACTERISTIC_UUID =
            UUID.fromString("0000ff03-0000-1000-8000-00805f9b34fb");
    private static final UUID BLE_CCCD_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final Set<String> HEALTH_PERMISSIONS = new HashSet<>(
            Arrays.asList(
                    HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(HeartRateRecord.class)),
                    HealthPermission.getReadPermission(JvmClassMappingKt.getKotlinClass(StepsRecord.class))));

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    private BluetoothForegroundService bluetoothService;
    private boolean isServiceBound = false;
    private String currentMoodLabel = "Calibrating...";
    private String lastRecommendedMood = "";
    
    private RecommendationManager recommendationManager;
    private String previousScreenBeforeSettings = SCREEN_HOME;
    private int devModeTapCount = 0;
    private long lastDevModeTapTime = 0;

    private final Map<String, Bitmap> imageCache = new HashMap<>();

    private FrameLayout screenContainer;
    private LinearLayout bottomNav;
    private ActivityResultLauncher<Set<String>> healthPermissionLauncher;
    private ActivityResultLauncher<String[]> blePermissionLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private HealthConnectClient healthConnectClient;
    private HealthSnapshot latestHealthSnapshot = HealthSnapshot.defaultValues();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;

    private final BluetoothForegroundService.BleDataCallback bleDataCallback = new BluetoothForegroundService.BleDataCallback() {
        @Override
        public void onStatusUpdated(String status) {
            updateBleStatus(status);
        }
        @Override
        public void onBiometricsUpdated(int bpm, long steps) {
            latestHealthSnapshot = latestHealthSnapshot.withBpm(String.valueOf(bpm))
                                                       .withSteps(String.valueOf(steps))
                                                       .withWellnessScore(String.valueOf(calculateWellnessScore(bpm, steps)));
            handler.post(() -> applyHealthSnapshot(currentContent));
        }
        @Override
        public void onMoodPredicted(String mood, int confidence) {
            currentMoodLabel = mood;
            handler.post(() -> {
                applyHealthSnapshot(currentContent);
                if (SCREEN_PLAYER.equals(currentScreen)) {
                    generateRecommendations(false);
                }
            });
        }
    };

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BluetoothForegroundService.LocalBinder binder = (BluetoothForegroundService.LocalBinder) service;
            bluetoothService = binder.getService();
            isServiceBound = true;
            bluetoothService.addCallback(bleDataCallback);
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isServiceBound = false;
            bluetoothService = null;
        }
    };
    private boolean bleScanning;
    private String bleStatusText = "Scan and choose your watch for instant live data.";
    private String selectedBleDeviceLabel = "watch";
    private final Map<String, BleDeviceCandidate> discoveredBleDevices = new LinkedHashMap<>();

    private final Runnable bleListRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (bleScanning) {
                renderBleDeviceList(currentContent);
                handler.postDelayed(this, BLE_LIST_REFRESH_MS);
            }
        }
    };
    private View currentContent;
    private String currentScreen = SCREEN_SPLASH;

    private final ScanCallback bleScanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String advertisedName = result.getScanRecord() == null
                    ? null
                    : result.getScanRecord().getDeviceName();
            String deviceName = advertisedName;
            if (deviceName == null && hasBleConnectPermission()) {
                deviceName = device.getName();
            }

            String address = device.getAddress();
            if (isEmpty(deviceName) && isEmpty(address)) {
                return;
            }

            BleDeviceCandidate candidate = new BleDeviceCandidate(
                    device,
                    isEmpty(deviceName) ? "Unknown BLE device" : deviceName,
                    address,
                    result.getRssi(),
                    scanResultHasService(result, BLE_STANDARD_HEART_RATE_SERVICE_UUID),
                    scanResultHasService(result, BLE_CUSTOM_LIVE_SERVICE_UUID),
                    false,
                    System.currentTimeMillis());
            addOrUpdateBleDevice(candidate);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            bleScanning = false;
            updateBleStatus(scanErrorMessage(errorCode));
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        recommendationManager = new RecommendationManager(this);
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!SCREEN_HOME.equals(currentScreen) && !SCREEN_LOGIN.equals(currentScreen)) {
                    showScreen(SCREEN_HOME);
                } else {
                    finish();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        screenContainer = findViewById(R.id.screenContainer);
        bottomNav = findViewById(R.id.bottomNav);
        setupHealthConnectPermissionLauncher();
        setupBlePermissionLauncher();
        
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("445817305343-i4vfcjsttniaqvd94patok905r5um0hd.apps.googleusercontent.com") // We don't have the web client ID, using string trick or default, wait, I need the actual Web Client ID. Wait, I will use "445817305343-i4vfcjsttniaqvd94patok905r5um0hd.apps.googleusercontent.com"
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

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        getSharedPreferences("MusicZPrefs", MODE_PRIVATE).edit()
                                .putString("profileImageUri", uri.toString()).apply();
                        if (SCREEN_MAIN_SETTINGS.equals(currentScreen)) {
                            configureMainSettingsScreen(currentContent);
                        } else if (SCREEN_PROFILE_SETTINGS.equals(currentScreen)) {
                            configureProfileSettingsScreen(currentContent);
                        }
                    }
                });
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        setupBottomNavigation();
        setupBackNavigation();
        
        scheduleSummaryAlarms();
        requestNotificationPermission();
        handleIntent(getIntent());
        
        if (SCREEN_SPLASH.equals(currentScreen)) {
            if (mAuth.getCurrentUser() != null) {
                showScreen(SCREEN_HOME);
            } else {
                showScreen(SCREEN_LOGIN);
            }
        }

    }
    
    private void scheduleSummaryAlarms() {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        
        Intent dailyIntent = new Intent(this, WellnessNotificationReceiver.class);
        dailyIntent.setAction(WellnessNotificationReceiver.ACTION_DAILY_SUMMARY);
        PendingIntent dailyPendingIntent = PendingIntent.getBroadcast(this, 100, dailyIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 20); // 8 PM
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1);
        }
        
        alarmManager.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), android.app.AlarmManager.INTERVAL_DAY, dailyPendingIntent);
        
        Intent weeklyIntent = new Intent(this, WellnessNotificationReceiver.class);
        weeklyIntent.setAction(WellnessNotificationReceiver.ACTION_WEEKLY_SUMMARY);
        PendingIntent weeklyPendingIntent = PendingIntent.getBroadcast(this, 101, weeklyIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        
        java.util.Calendar weeklyCalendar = java.util.Calendar.getInstance();
        weeklyCalendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY);
        weeklyCalendar.set(java.util.Calendar.HOUR_OF_DAY, 20); // 8 PM Sunday
        weeklyCalendar.set(java.util.Calendar.MINUTE, 0);
        weeklyCalendar.set(java.util.Calendar.SECOND, 0);
        if (weeklyCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            weeklyCalendar.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        }
        
        alarmManager.setInexactRepeating(android.app.AlarmManager.RTC_WAKEUP, weeklyCalendar.getTimeInMillis(), android.app.AlarmManager.INTERVAL_DAY * 7, weeklyPendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isServiceBound) {
            if (bluetoothService != null) bluetoothService.removeCallback(bleDataCallback);
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        stopBleScan();
        imageExecutor.shutdownNow();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("OPEN_PLAYER", false)) {
            showScreen(SCREEN_PLAYER);
            generateRecommendations(true);
        }
    }

    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            if (user.getDisplayName() != null) editor.putString("profileName", user.getDisplayName());
                            if (user.getEmail() != null) editor.putString("profileEmail", user.getEmail());
                            if (user.getPhotoUrl() != null && !prefs.contains("profileImageUri")) {
                                editor.putString("profileImageUri", user.getPhotoUrl().toString());
                            }
                            editor.apply();
                        }
                        showScreen(SCREEN_HOME);
                    } else {
                        android.widget.Toast.makeText(this, "Authentication Failed.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(view -> showScreen(SCREEN_HOME));
        findViewById(R.id.navLive).setOnClickListener(view -> showScreen(SCREEN_LIVE));
        findViewById(R.id.navPlayer).setOnClickListener(view -> showScreen(SCREEN_PLAYER));
        findViewById(R.id.navWellness).setOnClickListener(view -> showScreen(SCREEN_WELLNESS));
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SCREEN_LIVE.equals(currentScreen)
                        || SCREEN_PLAYER.equals(currentScreen)
                        || SCREEN_WELLNESS.equals(currentScreen)) {
                    showScreen(SCREEN_HOME);
                } else if (SCREEN_MAIN_SETTINGS.equals(currentScreen)) {
                    showScreen(previousScreenBeforeSettings != null ? previousScreenBeforeSettings : SCREEN_HOME);
                } else if (SCREEN_PREFERENCES.equals(currentScreen) || SCREEN_SETTINGS.equals(currentScreen) || SCREEN_PROFILE_SETTINGS.equals(currentScreen)) {
                    showScreen(SCREEN_MAIN_SETTINGS);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    public void showDiagnosticsDialog() {
        View overlay = findViewById(R.id.diagnosticsOverlay);
        if (overlay == null) return;
        overlay.setVisibility(View.VISIBLE);
        
        View closeBtn = findViewById(R.id.diagnosticsCloseButton);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> overlay.setVisibility(View.GONE));
        }
        
        refreshDiagnosticsUI();
    }
    
    public void refreshDiagnosticsUI() {
        View overlay = findViewById(R.id.diagnosticsOverlay);
        if (overlay == null || overlay.getVisibility() != View.VISIBLE) return;
        
        TextView balanceText = findViewById(R.id.diagnosticsBalanceText);
        TextView scoresText = findViewById(R.id.diagnosticsScoresText);
        ProgressBar loading = findViewById(R.id.diagnosticsLoading);
        
        if (loading != null) loading.setVisibility(View.GONE);
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String statsJson = prefs.getString("diagnostics_json", "{}");
        
        try {
            org.json.JSONObject stats = new org.json.JSONObject(statsJson);
            StringBuilder balanceMsg = new StringBuilder("--- Playlist Balance ---\n");
            StringBuilder scoresMsg = new StringBuilder("--- Top Tracks Breakdown ---\n");
            
            org.json.JSONObject artistCounts = stats.optJSONObject("artistCounts");
            if (artistCounts != null) {
                java.util.Iterator<String> keys = artistCounts.keys();
                while (keys.hasNext()) {
                    String artist = keys.next();
                    balanceMsg.append(artist).append(": ").append(artistCounts.getInt(artist)).append(" songs\n");
                }
            } else {
                balanceMsg.append("No balance data available.");
            }
            
            org.json.JSONArray scores = stats.optJSONArray("scores");
            if (scores != null && scores.length() > 0) {
                for (int i = 0; i < Math.min(20, scores.length()); i++) {
                    org.json.JSONObject s = scores.getJSONObject(i);
                    scoresMsg.append(s.getInt("rank")).append(". ").append(s.getString("name")).append("\n");
                    scoresMsg.append("   Score: ").append(s.getInt("totalScore"))
                       .append(" (M:").append(s.getInt("mood"))
                       .append(" L:").append(s.getInt("lang"))
                       .append(" A:").append(s.getInt("artistScore"))
                       .append(" G:").append(s.getInt("genre"))
                       .append(" H:").append(s.getInt("history"))
                       .append(" P:-").append(s.getInt("penalty")).append(")\n\n");
                }
            } else {
                scoresMsg.append("No recent recommendations found.");
            }
            
            if (balanceText != null) balanceText.setText(balanceMsg.toString());
            if (scoresText != null) scoresText.setText(scoresMsg.toString());
            
        } catch (Exception e) {
            if (balanceText != null) balanceText.setText("Diagnostics unavailable");
            if (scoresText != null) scoresText.setText("");
        }
    }

    private void showScreen(String screen) {
        currentScreen = screen;
        LayoutInflater inflater = LayoutInflater.from(this);
        int layoutId = getLayoutForScreen(screen);
        View content = inflater.inflate(layoutId, screenContainer, false);

        screenContainer.removeAllViews();
        screenContainer.addView(content);
        currentContent = content;

        configureScreen(content, screen);
        updateBottomNav(screen);
    }

    private int getLayoutForScreen(String screen) {
        switch (screen) {
            case SCREEN_LOGIN:
                return R.layout.screen_login;
            case SCREEN_HOME:
                return R.layout.screen_home;
            case SCREEN_LIVE:
                return R.layout.screen_live_mood;
            case SCREEN_PLAYER:
                return R.layout.screen_player;
            case SCREEN_SETTINGS:
                return R.layout.screen_settings;
            case SCREEN_PROFILE_SETTINGS:
                return R.layout.screen_profile_settings;
            case SCREEN_WELLNESS:
                return R.layout.screen_wellness;
            case SCREEN_PREFERENCES:
                return R.layout.screen_preferences;
            case SCREEN_MAIN_SETTINGS:
                return R.layout.screen_main_settings;
            case SCREEN_HEALTH_CONNECT:
                return R.layout.screen_health_connect;
            case SCREEN_PRIVACY:
                return R.layout.screen_privacy;
            case SCREEN_PROFILE:
                return R.layout.screen_profile;
            default:
                return R.layout.screen_splash;
        }
    }

    private void configureScreen(View content, String screen) {
        View topAppIcon = content.findViewById(R.id.topAppIcon);

        TextView topBrand = content.findViewById(R.id.topBrand);
        if (topBrand != null) {
            topBrand.setOnClickListener(view -> {
                long now = System.currentTimeMillis();
                if (now - lastDevModeTapTime > 1000) {
                    devModeTapCount = 0;
                }
                lastDevModeTapTime = now;
                devModeTapCount++;
                
                SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
                boolean isDevMode = prefs.getBoolean("developer_mode", false);
                
                if (isDevMode) {
                    showDiagnosticsDialog();
                } else {
                    if (devModeTapCount >= 7) {
                        prefs.edit().putBoolean("developer_mode", true).apply();
                        Toast.makeText(this, "Developer Mode Unlocked!", Toast.LENGTH_SHORT).show();
                        showDiagnosticsDialog();
                    } else if (devModeTapCount >= 4) {
                        Toast.makeText(this, "Tap " + (7 - devModeTapCount) + " more times to unlock developer mode.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        ImageView settingsButton = content.findViewById(R.id.settingsButton);
        if (settingsButton != null) {
            SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
            String imageUriStr = prefs.getString("profileImageUri", null);
            if (imageUriStr != null) {
                loadImage(imageUriStr, settingsButton);
            }
            settingsButton.setOnClickListener(view -> {
                previousScreenBeforeSettings = currentScreen;
                showScreen(SCREEN_MAIN_SETTINGS);
            });
        }

        View getStartedButton = content.findViewById(R.id.getStartedButton);
        if (getStartedButton != null) {
            getStartedButton.setOnClickListener(view -> {
                SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
                prefs.edit().putBoolean("hasCompletedOnboarding", true).apply();
                showScreen(SCREEN_HOME);
            });
        }



        View viewDetailsButton = content.findViewById(R.id.viewDetailsButton);
        if (viewDetailsButton != null) {
            viewDetailsButton.setOnClickListener(view -> showScreen(SCREEN_LIVE));
        }

        View healthConnectButton = content.findViewById(R.id.healthConnectButton);
        if (healthConnectButton != null) {
            healthConnectButton.setOnClickListener(view -> syncHealthConnect(true));
            if ("Connect to read today's heart rate and steps.".equals(latestHealthSnapshot.statusText)) {
                syncHealthConnect(false);
            }
        }

        View bleConnectButton = content.findViewById(R.id.bleConnectButton);
        if (bleConnectButton != null) {
            bleConnectButton.setOnClickListener(view -> connectLiveBleWatch());
        }
        

        ImageView onboardingImage = content.findViewById(R.id.onboardingDeviceImage);
        if (onboardingImage != null) {
            loadImage(DEVICE_IMAGE_URL, onboardingImage);
        }


        if (SCREEN_PLAYER.equals(screen)) {
            configurePlayerScreen(content);
        } else if (SCREEN_LOGIN.equals(screen)) {
            configureLoginScreen(content);
        } else if (SCREEN_PREFERENCES.equals(screen)) {
            configurePreferencesScreen(content);
        } else if (SCREEN_HEALTH_CONNECT.equals(screen)) {
            configureHealthConnectScreen(content);
        } else if (SCREEN_PRIVACY.equals(screen)) {
            configurePrivacyScreen(content);
        } else if (SCREEN_PROFILE.equals(screen)) {
            configureProfileScreen(content);
        } else if (SCREEN_WELLNESS.equals(screen)) {
            configureWellnessScreen(content);
        } else if (SCREEN_MAIN_SETTINGS.equals(screen)) {
            configureMainSettingsScreen(content);
        } else if (SCREEN_PROFILE_SETTINGS.equals(screen)) {
            configureProfileSettingsScreen(content);
        } else if (SCREEN_SETTINGS.equals(screen)) {
            configureSettingsScreen(content);
        } else if (SCREEN_HOME.equals(screen)) {
            configureHomeScreen(content);
        }

        applyHealthSnapshot(content);
        applyBleStatus(content);
    }

    private void setupChipGroup(ChipGroup group, EditText otherEdit, String prefValue) {
        if (group == null) return;
        List<String> prefItems = Arrays.asList(prefValue.split(","));
        List<String> customItems = new ArrayList<>();
        
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                String text = chip.getText().toString();
                if ("Other".equalsIgnoreCase(text)) {
                    chip.setOnCheckedChangeListener((btn, isChecked) -> {
                        if (otherEdit != null) otherEdit.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                    });
                } else {
                    boolean hasPref = false;
                    for (String pref : prefItems) {
                        if (pref.trim().equalsIgnoreCase(text)) {
                            hasPref = true;
                            break;
                        }
                    }
                    chip.setChecked(hasPref);
                }
            }
        }
        
        for (String pref : prefItems) {
            String p = pref.trim();
            if (p.isEmpty()) continue;
            boolean matched = false;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof Chip) {
                    Chip chip = (Chip) child;
                    if (p.equalsIgnoreCase(chip.getText().toString()) && !"Other".equalsIgnoreCase(chip.getText().toString())) {
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                customItems.add(p);
            }
        }
        
        if (!customItems.isEmpty() && otherEdit != null) {
            otherEdit.setText(android.text.TextUtils.join(", ", customItems));
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof Chip && "Other".equalsIgnoreCase(((Chip) child).getText().toString())) {
                    ((Chip) child).setChecked(true);
                }
            }
        }
    }

    private String getSelectedChips(ChipGroup group, EditText otherEdit) {
        if (group == null) return "";
        List<String> selected = new ArrayList<>();
        boolean otherChecked = false;
        
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    if ("Other".equalsIgnoreCase(chip.getText().toString())) {
                        otherChecked = true;
                    } else {
                        selected.add(chip.getText().toString());
                    }
                }
            }
        }
        
        if (otherChecked && otherEdit != null) {
            String otherText = otherEdit.getText().toString().trim();
            if (!otherText.isEmpty()) {
                selected.add(otherText);
            }
        }
        
        return android.text.TextUtils.join(", ", selected);
    }

    private void configurePreferencesScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromPreferencesBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(SCREEN_MAIN_SETTINGS));
        }

        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        
        ChipGroup groupLang = content.findViewById(R.id.chipGroupLanguage);
        EditText editLang = content.findViewById(R.id.editLanguage);
        setupChipGroup(groupLang, editLang, prefs.getString("prefLanguage", "English"));
        
        ChipGroup groupGenres = content.findViewById(R.id.chipGroupGenres);
        EditText editGenres = content.findViewById(R.id.editGenres);
        setupChipGroup(groupGenres, editGenres, prefs.getString("prefGenres", "Pop"));

        EditText editArtists = content.findViewById(R.id.editArtists);
        if (editArtists != null) editArtists.setText(prefs.getString("prefArtists", ""));

        ChipGroup groupMusicType = content.findViewById(R.id.chipGroupMusicType);
        EditText editMusicType = content.findViewById(R.id.editMusicType);
        setupChipGroup(groupMusicType, editMusicType, prefs.getString("prefMusicType", "Relaxation"));

        android.widget.Spinner spinnerMusicApp = content.findViewById(R.id.spinnerMusicApp);
        java.util.List<String> musicAppNames = new java.util.ArrayList<>();
        final java.util.List<String> musicAppPackages = new java.util.ArrayList<>();
        musicAppNames.add("System Default (Ask every time)");
        musicAppPackages.add("");

        android.content.Intent searchIntent = new android.content.Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        java.util.List<android.content.pm.ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(searchIntent, 0);
        for (android.content.pm.ResolveInfo info : resolveInfos) {
            musicAppNames.add(info.loadLabel(getPackageManager()).toString());
            musicAppPackages.add(info.activityInfo.packageName);
        }

        if (spinnerMusicApp != null) {
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, musicAppNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMusicApp.setAdapter(adapter);

            String savedPackage = prefs.getString("prefMusicApp", "");
            int savedIndex = musicAppPackages.indexOf(savedPackage);
            if (savedIndex >= 0) {
                spinnerMusicApp.setSelection(savedIndex);
            }
        }

        View saveBtn = content.findViewById(R.id.saveSettingsButton);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("prefLanguage", getSelectedChips(groupLang, editLang));
                editor.putString("prefGenres", getSelectedChips(groupGenres, editGenres));
                if (editArtists != null) editor.putString("prefArtists", editArtists.getText().toString());
                editor.putString("prefMusicType", getSelectedChips(groupMusicType, editMusicType));
                if (spinnerMusicApp != null) {
                    int selectedIndex = spinnerMusicApp.getSelectedItemPosition();
                    if (selectedIndex >= 0 && selectedIndex < musicAppPackages.size()) {
                        editor.putString("prefMusicApp", musicAppPackages.get(selectedIndex));
                    }
                }
                editor.apply();
                Toast.makeText(MainActivity.this, "Preferences Saved!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void configureProfileScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromProfileBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(SCREEN_HOME));
        }
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        setTextIfPresent(content, R.id.profileWatch, selectedBleDeviceLabel);
        
        int totalSessions = prefs.getInt("totalSyncSessions", 1);
        setTextIfPresent(content, R.id.profileSessions, String.valueOf(totalSessions));
        
        setTextIfPresent(content, R.id.profileAvgWellness, latestHealthSnapshot.wellnessScoreText);
        
        String lang = prefs.getString("prefLanguage", "English");
        String genre = prefs.getString("prefGenres", "Pop");
        setTextIfPresent(content, R.id.profileLanguage, lang.isEmpty() ? "None" : lang);
        setTextIfPresent(content, R.id.profileGenre, genre.isEmpty() ? "None" : genre);
    }
    private void configureMainSettingsScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromMainSettingsButton);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(previousScreenBeforeSettings != null ? previousScreenBeforeSettings : SCREEN_HOME));
        }

        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String name = prefs.getString("profileName", "User Name");
        String email = prefs.getString("profileEmail", "Set up your profile");
        
        TextView profileName = content.findViewById(R.id.profileHubName);
        if (profileName != null) profileName.setText(name);

        TextView profileEmail = content.findViewById(R.id.profileHubEmail);
        if (profileEmail != null) profileEmail.setText(email);

        ImageView profileAvatar = content.findViewById(R.id.profileHubAvatar);
        if (profileAvatar != null) {
            String imageUriStr = prefs.getString("profileImageUri", null);
            if (imageUriStr != null) {
                loadImage(imageUriStr, profileAvatar);
            }
        }

        View btnProfileSettings = content.findViewById(R.id.btnOpenProfileSettings);
        if (btnProfileSettings != null) {
            btnProfileSettings.setOnClickListener(v -> showScreen(SCREEN_PROFILE_SETTINGS));
        }

        View btnPreferences = content.findViewById(R.id.btnOpenPreferences);
        if (btnPreferences != null) {
            btnPreferences.setOnClickListener(v -> showScreen(SCREEN_PREFERENCES));
        }

        View btnNotifications = content.findViewById(R.id.btnOpenNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> showScreen(SCREEN_SETTINGS));
        }

        View btnConnectedDevice = content.findViewById(R.id.btnOpenConnectedDevice);
        if (btnConnectedDevice != null) {
            btnConnectedDevice.setOnClickListener(v -> android.widget.Toast.makeText(this, "Connected Device settings coming soon", android.widget.Toast.LENGTH_SHORT).show());
        }

        View btnHealthConnect = content.findViewById(R.id.btnOpenHealthConnect);
        if (btnHealthConnect != null) {
            btnHealthConnect.setOnClickListener(v -> showScreen(SCREEN_HEALTH_CONNECT));
        }

        View btnAppearance = content.findViewById(R.id.btnOpenAppearance);
        if (btnAppearance != null) {
            btnAppearance.setOnClickListener(v -> android.widget.Toast.makeText(this, "Appearance settings coming soon", android.widget.Toast.LENGTH_SHORT).show());
        }

        View btnPrivacy = content.findViewById(R.id.btnOpenPrivacy);
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v -> showScreen(SCREEN_PRIVACY));
        }

        View btnAbout = content.findViewById(R.id.btnOpenAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> android.widget.Toast.makeText(this, "About Emora coming soon", android.widget.Toast.LENGTH_SHORT).show());
        }

        View btnLogout = content.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Logged out successfully", android.widget.Toast.LENGTH_SHORT).show();
                mAuth.signOut();
                mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                    showScreen(SCREEN_LOGIN);
                });
            });
        }
    }

    private void configureSettingsScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromSettingsButton);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(SCREEN_MAIN_SETTINGS));
        }

        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        
        Switch switchStressAlerts = content.findViewById(R.id.switchStressAlerts);
        Switch switchDailySummary = content.findViewById(R.id.switchDailySummary);
        Switch switchWeeklySummary = content.findViewById(R.id.switchWeeklySummary);
        Switch switchAchievements = content.findViewById(R.id.switchAchievements);
        
        if (switchStressAlerts != null) {
            switchStressAlerts.setChecked(prefs.getBoolean("prefStressAlerts", true));
            switchStressAlerts.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("prefStressAlerts", isChecked).apply());
        }
        if (switchDailySummary != null) {
            switchDailySummary.setChecked(prefs.getBoolean("prefDailySummary", true));
            switchDailySummary.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("prefDailySummary", isChecked).apply());
        }
        if (switchWeeklySummary != null) {
            switchWeeklySummary.setChecked(prefs.getBoolean("prefWeeklySummary", true));
            switchWeeklySummary.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("prefWeeklySummary", isChecked).apply());
        }
        if (switchAchievements != null) {
            switchAchievements.setChecked(prefs.getBoolean("prefAchievements", true));
            switchAchievements.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("prefAchievements", isChecked).apply());
        }
    }

    private void configureProfileSettingsScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromProfileSettingsBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(SCREEN_MAIN_SETTINGS));
        }

        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        EditText editName = content.findViewById(R.id.editProfileName);
        EditText editEmail = content.findViewById(R.id.editProfileEmail);
        
        if (editName != null) editName.setText(prefs.getString("profileName", ""));
        if (editEmail != null) editEmail.setText(prefs.getString("profileEmail", ""));
        ImageView editAvatar = content.findViewById(R.id.profileSettingsAvatar);
        if (editAvatar != null) {
            String imageUriStr = prefs.getString("profileImageUri", null);
            if (imageUriStr != null) {
                loadImage(imageUriStr, editAvatar);
            }
            editAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        }
        
        View btnSave = content.findViewById(R.id.btnSaveProfile);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                SharedPreferences.Editor editor = prefs.edit();
                if (editName != null) editor.putString("profileName", editName.getText().toString());
                if (editEmail != null) editor.putString("profileEmail", editEmail.getText().toString());
                editor.apply();
                
                Toast.makeText(MainActivity.this, "Profile Saved", Toast.LENGTH_SHORT).show();
                showScreen(SCREEN_MAIN_SETTINGS);
            });
        }
    }

    private void configureHealthConnectScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromHealthConnectBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(SCREEN_MAIN_SETTINGS));
        }
        
        Switch switchHealthConnect = content.findViewById(R.id.switchHealthConnect);
        if (switchHealthConnect != null) {
            SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
            switchHealthConnect.setChecked(prefs.getBoolean("prefHealthConnectEnabled", false));
            
            switchHealthConnect.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) {
                    int sdkStatus = androidx.health.connect.client.HealthConnectClient.getSdkStatus(this);
                    if (sdkStatus != androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
                        switchHealthConnect.setChecked(false);
                        Toast.makeText(this, "Health Connect is not supported or not installed on this device.", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                prefs.edit().putBoolean("prefHealthConnectEnabled", isChecked).apply();
            });
        }
    }

    private void configurePrivacyScreen(View content) {
        View backBtn = content.findViewById(R.id.backFromPrivacyBtn);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> showScreen(SCREEN_MAIN_SETTINGS));
        }
    }

    private void configureHomeScreen(View content) {
        TextView welcomeText = content.findViewById(R.id.homeWelcomeText);
        if (welcomeText != null) {
            SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
            String name = prefs.getString("profileName", "Alex");
            if (name.isEmpty()) name = "there";

            java.util.Calendar c = java.util.Calendar.getInstance();
            int timeOfDay = c.get(java.util.Calendar.HOUR_OF_DAY);

            String greeting = "Good evening";
            if(timeOfDay >= 0 && timeOfDay < 12){
                greeting = "Good morning";
            } else if(timeOfDay >= 12 && timeOfDay < 16){
                greeting = "Good afternoon";
            } else if(timeOfDay >= 16 && timeOfDay < 21){
                greeting = "Good evening";
            } else if(timeOfDay >= 21 && timeOfDay < 24){
                greeting = "Welcome back";
            }

            welcomeText.setText(greeting + ", " + name + "!");
        }
    }

    private void configureLoginScreen(View view) {
        View btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn);
        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                android.content.Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        }
    }

    private void configureWellnessScreen(View content) {
        TextView tvScoreValue = content.findViewById(R.id.tvEmotionalScoreValue);
        TextView tvScoreTrend = content.findViewById(R.id.tvEmotionalScoreTrend);
        TextView tvScoreLabel = content.findViewById(R.id.tvEmotionalScoreLabel);
        TextView tvScoreSubLabel = content.findViewById(R.id.tvEmotionalScoreSubLabel);
        TextView tvBaselineHR = content.findViewById(R.id.tvBaselineHR);
        TextView tvWeeklyLogs = content.findViewById(R.id.tvWeeklyLogs);
        com.example.genzmusicapp.StressChartView stressChart = content.findViewById(R.id.stressChart);
        TextView tvMoodDist = content.findViewById(R.id.tvMoodDistribution);
        TextView tvAiTitle = content.findViewById(R.id.tvExplainableAnalysisTitle);
        TextView tvAiDesc = content.findViewById(R.id.tvExplainableAnalysisDesc);

        if (tvScoreValue == null) return; // Layout not loaded properly

        // Hide the stress timeline initially until a sync occurs
        if (stressChart != null) {
            View timelineCard = (View) stressChart.getParent();
            if (timelineCard != null && timelineCard instanceof android.widget.LinearLayout) {
                timelineCard.setVisibility(View.GONE);
            }
        }

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "guest";
                int totalLogs = com.example.genzmusicapp.db.AppDatabase.getDatabase(this).wellnessDao().getTotalLogsCount(userId);
                java.util.List<com.example.genzmusicapp.db.WellnessHistory> history =
                        com.example.genzmusicapp.db.AppDatabase.getDatabase(this).wellnessDao().getRecentHistory(userId, 500);
                runOnUiThread(() -> {
                    if (history == null || history.isEmpty()) {
                        tvScoreValue.setText("-- / 100");
                        tvScoreTrend.setText("No data yet");
                        tvScoreLabel.setText("Need More Data");
                        tvScoreSubLabel.setText("Wear watch to start collecting");
                        tvBaselineHR.setText("-- bpm");
                        tvWeeklyLogs.setText("0 logs");
                        tvMoodDist.setText("Not enough data to calculate mood distribution.");
                        tvAiTitle.setText("Gathering Baseline...");
                        tvAiDesc.setText("Keep syncing your watch. Analytics require at least a few entries to generate an explainable analysis.");
                        return;
                    }

                    // 1. Weekly Logs
                    tvWeeklyLogs.setText(totalLogs + " logs");
                    
                    if (stressChart != null) {
                        View timelineCard = (View) stressChart.getParent();
                        if (timelineCard != null && timelineCard instanceof android.widget.LinearLayout) {
                            timelineCard.setVisibility(View.VISIBLE);
                        }
                    }

                    // 2. Calculate Baseline HR and Variance
                    long totalHr = 0;
                    for (com.example.genzmusicapp.db.WellnessHistory h : history) {
                        totalHr += h.bpm;
                    }
                    double avgHr = (double) totalHr / history.size();
                    tvBaselineHR.setText(Math.round(avgHr) + " bpm");

                    double varianceSum = 0;
                    for (com.example.genzmusicapp.db.WellnessHistory h : history) {
                        varianceSum += Math.pow(h.bpm - avgHr, 2);
                    }
                    double stdDevHr = Math.sqrt(varianceSum / history.size());

                    // 3. Mood Distribution
                    java.util.Map<String, Integer> moodCounts = new java.util.HashMap<>();
                    int stressedCount = 0;
                    int calmCount = 0;
                    int validMoodCount = 0;
                    for (com.example.genzmusicapp.db.WellnessHistory h : history) {
                        String mood = h.calculatedMood;
                        if (mood != null && !mood.isEmpty() && !mood.equalsIgnoreCase("Unknown")) {
                            moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
                            validMoodCount++;
                        }
                        if (mood != null && (mood.equalsIgnoreCase("Stressed") || mood.equalsIgnoreCase("Anxious"))) stressedCount++;
                        if (mood != null && (mood.equalsIgnoreCase("Relax") || mood.equalsIgnoreCase("Calm"))) calmCount++;
                    }

                    StringBuilder distStr = new StringBuilder();
                    if (validMoodCount > 0) {
                        for (java.util.Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
                            int percent = (int) Math.round((entry.getValue() * 100.0) / validMoodCount);
                            if (percent > 0) {
                                distStr.append("• ").append(entry.getKey()).append(": ").append(percent).append("%\n");
                            }
                        }
                    }
                    if (distStr.length() == 0) distStr.append("Not enough mood data yet.");
                    tvMoodDist.setText(distStr.toString().trim());

                    // 4. Calculate Emotional Stability Score
                    // Base 100
                    // - Penalty for high HR variance (stdDev > 10)
                    // - Penalty for high stress %
                    // - Bonus for high calm %
                    double stressRatio = (double) stressedCount / history.size();
                    double calmRatio = (double) calmCount / history.size();
                    
                    double score = 100.0;
                    if (stdDevHr > 10) score -= (stdDevHr - 10) * 1.5;
                    score -= (stressRatio * 30.0);
                    score += (calmRatio * 15.0);
                    
                    if (score > 100) score = 100;
                    if (score < 10) score = 10;
                    
                    int finalScore = (int) Math.round(score);
                    tvScoreValue.setText(finalScore + " / 100");

                    // Trend
                    tvScoreTrend.setText(finalScore >= 80 ? "Optimal Range" : (finalScore >= 50 ? "Moderate Fluctuation" : "High Volatility"));

                    if (finalScore >= 80) {
                        tvScoreLabel.setText("Highly Stable");
                        tvScoreLabel.setTextColor(android.graphics.Color.parseColor("#00dce5")); // Cyan
                        tvScoreSubLabel.setText("Excellent cardiovascular consistency.");
                    } else if (finalScore >= 50) {
                        tvScoreLabel.setText("Balanced");
                        tvScoreLabel.setTextColor(android.graphics.Color.parseColor("#ffafd3")); // Pink
                        tvScoreSubLabel.setText("Normal daily fluctuations.");
                    } else {
                        tvScoreLabel.setText("Elevated Stress");
                        tvScoreLabel.setTextColor(android.graphics.Color.parseColor("#ffb4ab")); // Red
                        tvScoreSubLabel.setText("Higher than normal variability.");
                    }

                    // 5. Explainable AI Insight (DYNAMIC via Gemini)
                    tvAiTitle.setText("Analyzing patterns...");
                    tvAiDesc.setText("Consulting AI Wellness Coach...");
                    
                    String prompt = "You are an AI wellness coach. Provide a short 2-sentence encouraging insight directly to the user based on these stats: Average HR is " + Math.round(avgHr) + " bpm. " + Math.round(stressRatio * 100) + "% of recent logs indicate stress. " + Math.round(calmRatio * 100) + "% indicate calm.";
                    
                    new com.example.genzmusicapp.recommendation.GeminiService().generatePlaylist("", prompt, new com.example.genzmusicapp.recommendation.GeminiService.GeminiCallback() {
                        @Override
                        public void onSuccess(String text) {
                            runOnUiThread(() -> {
                                tvAiTitle.setText(finalScore >= 80 ? "Optimal Recovery State" : (stressRatio > 0.4 ? "High Stress Load Detected" : "AI Wellness Insight"));
                                tvAiDesc.setText(text);
                            });
                        }
                        @Override
                        public void onFailure(String error) {
                            runOnUiThread(() -> {
                                tvAiTitle.setText("AI Insight Unavailable");
                                tvAiDesc.setText("Could not fetch real-time insight from Gemini. Ensure you have network connectivity.");
                            });
                        }
                    });

                    // 6. Weekly Stress Timeline (Chart)
                    // Aggregate by day of week
                    float[] chartValues = new float[7];
                    String[] chartLabels = new String[7];
                    
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEE", java.util.Locale.US);
                    
                    for (int i = 6; i >= 0; i--) {
                        java.util.Calendar dayCal = java.util.Calendar.getInstance();
                        dayCal.add(java.util.Calendar.DAY_OF_YEAR, -i);
                        
                        // Set start and end of day
                        dayCal.set(java.util.Calendar.HOUR_OF_DAY, 0);
                        dayCal.set(java.util.Calendar.MINUTE, 0);
                        dayCal.set(java.util.Calendar.SECOND, 0);
                        long startOfDay = dayCal.getTimeInMillis();
                        
                        dayCal.set(java.util.Calendar.HOUR_OF_DAY, 23);
                        dayCal.set(java.util.Calendar.MINUTE, 59);
                        dayCal.set(java.util.Calendar.SECOND, 59);
                        long endOfDay = dayCal.getTimeInMillis();
                        
                        // Find entries for this day
                        long dayHrSum = 0;
                        int dayEntries = 0;
                        for (com.example.genzmusicapp.db.WellnessHistory h : history) {
                            if (h.timestamp >= startOfDay && h.timestamp <= endOfDay) {
                                dayHrSum += h.bpm;
                                dayEntries++;
                            }
                        }
                        
                        float dayStress = 0f;
                        if (dayEntries > 0) {
                            float dayAvgHr = (float) dayHrSum / dayEntries;
                            // Convert HR to a 0.0 - 1.0 "stress" value (assuming 60 is lowest, 120+ is max)
                            dayStress = (dayAvgHr - 60f) / 60f;
                            if (dayStress < 0.1f) dayStress = 0.1f;
                            if (dayStress > 1.0f) dayStress = 1.0f;
                        }
                        
                        chartValues[6 - i] = dayStress;
                        chartLabels[6 - i] = dayFormat.format(dayCal.getTime());
                    }
                    
                    if (stressChart != null) {
                        stressChart.setData(chartValues, chartLabels);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void configurePlayerScreen(View content) {
        TextView refreshButton = content.findViewById(R.id.playerAiRecButton);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> generateRecommendations(true));
        }
        
        generateRecommendations(false);
    }

    private void generateRecommendations(boolean forceRefresh) {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Syncing biometrics and preferences...");
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String prefLanguage = prefs.getString("prefLanguage", "").trim();
        String prefGenres = prefs.getString("prefGenres", "").trim();
        
        if (prefLanguage.isEmpty() && prefGenres.isEmpty()) {
            if (subtitle != null) subtitle.setText("Please set your preferences in Wellness first.");
            return;
        }

        long bpm = parseLongSafe(latestHealthSnapshot.bpmText);
        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        
        if (bpm <= 0) {
            if (subtitle != null) subtitle.setText("Awaiting stable biometric readings...");
            return;
        }

        String stressLevel = currentMoodLabel;
        String moodQuery = "pop";
        if (stressLevel.equals("Stressed")) moodQuery = "relaxing chill";
        else if (stressLevel.equals("Relaxed") || stressLevel.equals("Calm")) moodQuery = "acoustic soft";
        else if (stressLevel.equals("Energetic")) moodQuery = "upbeat energy";

        String prefArtists = prefs.getString("prefArtists", "").trim();
        String prefType = prefs.getString("prefMusicType", "").trim();

        String finalStressLevel = stressLevel;
        
        // --- NEW LOGIC: Only fetch if forced or mood changed ---
        if (!forceRefresh && RecommendationEngine.cachedRecommendations != null && !RecommendationEngine.cachedRecommendations.isEmpty() && finalStressLevel.equals(lastRecommendedMood)) {
            renderRecommendations(RecommendationEngine.cachedRecommendations, finalStressLevel, prefLanguage);
            return;
        }
        // -------------------------------------------------------

        lastRecommendedMood = finalStressLevel;
        if (subtitle != null) subtitle.setText("Curating " + finalStressLevel + " recommendations...");
        
        // Use a broader query for iTunes to ensure we get results (iTunes 'term' acts as strict AND)
        
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(android.view.View.VISIBLE);
        
        View diagOverlay = findViewById(R.id.diagnosticsOverlay);
        if (diagOverlay != null && diagOverlay.getVisibility() == View.VISIBLE) {
            ProgressBar diagLoading = findViewById(R.id.diagnosticsLoading);
            if (diagLoading != null) diagLoading.setVisibility(View.VISIBLE);
            TextView bText = findViewById(R.id.diagnosticsBalanceText);
            TextView sText = findViewById(R.id.diagnosticsScoresText);
            if (bText != null) bText.setText("Generating recommendations...");
            if (sText != null) sText.setText("");
        }

        // Fetch recent and skipped lists
        String recentTracksStr = prefs.getString("recently_recommended_tracks", "");
        List<String> recent = new ArrayList<>(Arrays.asList(recentTracksStr.split(",")));
        List<String> skipped = new ArrayList<>();
        List<String> favorites = new ArrayList<>();

        recommendationManager.fetchRecommendations(finalStressLevel, "", "Daytime", prefLanguage, prefGenres, prefArtists, recent, skipped, favorites, forceRefresh, new RecommendationManager.ManagerCallback() {
            @Override
            public void onSuccess(PlaylistResponse playlist) {
                handler.post(() -> {
                    renderPlaylist(playlist);
                    refreshDiagnosticsUI();
                });
            }

            @Override
            public void onFallback(String reason) {
                // Fallback to existing rule-based engine
                RecommendationEngine.fetchAndRankLegacy(MainActivity.this, finalStressLevel, prefLanguage, prefGenres, prefArtists, new RecommendationEngine.RecommendationCallback() {
                    @Override
                    public void onSuccess(List<RecommendationEngine.RankedSong> recommendations) {
                        handler.post(() -> {
                            renderRecommendations(recommendations, finalStressLevel, prefLanguage);
                            refreshDiagnosticsUI();
                        });
                    }

                    @Override
                    public void onFailure(String error, List<RecommendationEngine.RankedSong> cachedRecommendations) {
                        handler.post(() -> {
                            android.widget.Toast.makeText(MainActivity.this, "Fallback failed: " + error, android.widget.Toast.LENGTH_SHORT).show();
                            if (cachedRecommendations != null && !cachedRecommendations.isEmpty()) {
                                renderRecommendations(cachedRecommendations, finalStressLevel, prefLanguage);
                            } else {
                                if (loading != null) loading.setVisibility(android.view.View.GONE);
                            }
                            refreshDiagnosticsUI();
                        });
                    }
                });
            }
        });
    }

    private void renderPlaylist(PlaylistResponse playlist) {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;

        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(View.GONE);

        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText(playlist.playlistTitle + " - " + playlist.playlistDescription);

        LinearLayout container = currentContent.findViewById(R.id.playerPlaylistContainer);
        if (container == null) return;
        container.removeAllViews();
        
        // Show the overall reason
        if (playlist.overallReason != null && !playlist.overallReason.isEmpty()) {
            TextView reasonView = new TextView(this);
            reasonView.setText(playlist.overallReason);
            reasonView.setTextColor(android.graphics.Color.CYAN);
            reasonView.setPadding(10, 20, 10, 40);
            reasonView.setTextSize(14f);
            container.addView(reasonView);
        }

        if (playlist.songs == null || playlist.songs.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No songs found. Please change your preference settings.");
            emptyView.setTextColor(android.graphics.Color.WHITE);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(0, 50, 0, 50);
            container.addView(emptyView);
            return;
        }

        for (SongRecommendation song : playlist.songs) {
            try {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_recommendation, container, false);

                setTextIfPresent(itemView, R.id.itemSongName, song.title);
                setTextIfPresent(itemView, R.id.itemArtistName, song.artist);
                setTextIfPresent(itemView, R.id.itemGenre, song.genre);
                setTextIfPresent(itemView, R.id.itemLanguage, "AI Selected");
                setTextIfPresent(itemView, R.id.itemReason, song.reason);
                setTextIfPresent(itemView, R.id.itemMatchScore, song.confidence + "% Match");

                TextView matchScoreView = itemView.findViewById(R.id.itemMatchScore);
                if (matchScoreView != null && song.confidence > 80) {
                    matchScoreView.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                }

                ImageView artView = itemView.findViewById(R.id.itemAlbumArt);
                if (artView != null && song.artworkUrl != null && !song.artworkUrl.isEmpty()) {
                    loadImage(song.artworkUrl, artView);
                }

                itemView.setOnClickListener(v -> {
                    resolveMusicTrack(song.trackId, song.title, song.artist);
                });

                container.addView(itemView);
            } catch (Exception e) {
                Log.e(TAG, "Error rendering song", e);
            }
        }
    }
    



    private void renderRecommendations(List<RecommendationEngine.RankedSong> songs, String stressLevel, String langPref) {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(View.GONE);
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Found " + songs.size() + " matches for your " + stressLevel + " state.");

        LinearLayout container = currentContent.findViewById(R.id.playerPlaylistContainer);
        if (container == null) return;
        container.removeAllViews();
        
        if (songs.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No songs found. Please change your preference settings.");
            emptyView.setTextColor(android.graphics.Color.WHITE);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setPadding(0, 50, 0, 50);
            container.addView(emptyView);
            return;
        }

        for (RecommendationEngine.RankedSong song : songs) {
            try {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_recommendation, container, false);
                
                setTextIfPresent(itemView, R.id.itemSongName, song.trackName);
                setTextIfPresent(itemView, R.id.itemArtistName, song.artistName);
                setTextIfPresent(itemView, R.id.itemGenre, song.genre);
                setTextIfPresent(itemView, R.id.itemLanguage, langPref.isEmpty() ? "Global" : langPref);
                setTextIfPresent(itemView, R.id.itemReason, song.explanation);
                setTextIfPresent(itemView, R.id.itemMatchScore, song.finalScore + "% Match");
                
                TextView matchScoreView = itemView.findViewById(R.id.itemMatchScore);
                if (matchScoreView != null) {
                    if (song.finalScore >= 90) matchScoreView.setTextColor(android.graphics.Color.parseColor("#a5d6a7"));
                    else if (song.finalScore >= 70) matchScoreView.setTextColor(android.graphics.Color.parseColor("#fff59d"));
                    else matchScoreView.setTextColor(android.graphics.Color.parseColor("#ffcc80"));
                }

                TextView notInterested = itemView.findViewById(R.id.itemNotInterested);
                if (notInterested != null) {
                    notInterested.setOnClickListener(v -> {
                        container.removeView(itemView);
                        android.widget.Toast.makeText(MainActivity.this, "We won't recommend this again.", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }

                android.widget.ImageView albumArt = itemView.findViewById(R.id.itemAlbumArt);
                if (albumArt != null && !song.artworkUrl.isEmpty()) {
                    loadImage(song.artworkUrl, albumArt);
                }

                String finalLang = langPref.isEmpty() ? "Global" : langPref;
                itemView.setOnClickListener(v -> {
                    logUserInteraction(song, finalLang);
                    resolveMusicTrack(song.trackId, song.trackName, song.artistName);
                });
                container.addView(itemView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void logUserInteraction(RecommendationEngine.RankedSong song, String lang) {
        String genre = song.genre;
        String artist = song.artistName;
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        try {
            org.json.JSONObject freqMap = new org.json.JSONObject(prefs.getString("user_learned_preferences", "{}"));
            if (!genre.isEmpty() && !"Unknown".equals(genre)) {
                freqMap.put(genre, freqMap.optInt(genre, 0) + 1);
            }
            if (!lang.isEmpty() && !"Global".equals(lang)) {
                freqMap.put(lang, freqMap.optInt(lang, 0) + 1);
            }
            if (!artist.isEmpty() && !"Unknown Artist".equals(artist)) {
                freqMap.put("artist_" + artist.toLowerCase(), freqMap.optInt("artist_" + artist.toLowerCase(), 0) + 1);
            }
            prefs.edit().putString("user_learned_preferences", freqMap.toString()).apply();
            
            // Save to Room DB
            dbExecutor.execute(() -> {
                try {
                    com.example.genzmusicapp.db.AppDatabase db = com.example.genzmusicapp.db.AppDatabase.getDatabase(this);
                    com.example.genzmusicapp.db.SongHistory sh = db.songDao().getSong(song.trackId);
                    if (sh == null) {
                        sh = new com.example.genzmusicapp.db.SongHistory();
                        sh.trackId = song.trackId;
                        sh.trackName = song.trackName;
                        sh.artistName = song.artistName;
                        sh.genre = song.genre;
                        sh.playCount = 0;
                        sh.userFeedback = 0;
                    }
                    sh.playCount += 1;
                    sh.lastPlayedTimestamp = System.currentTimeMillis();
                    db.songDao().insertOrUpdate(sh);
                } catch (Exception ignored) {}
            });
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resolveMusicTrack(String itunesTrackId, String trackName, String artistName) {
        try {
            SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
            String preferredApp = prefs.getString("prefMusicApp", "");

            android.content.Intent intent = new android.content.Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
            if (!preferredApp.isEmpty()) {
                intent.setPackage(preferredApp);
            }
            
            intent.putExtra(android.app.SearchManager.QUERY, trackName + " " + artistName);
            intent.putExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio");
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback to youtube search if no app handles the intent
            try {
                String query = java.net.URLEncoder.encode(trackName + " " + artistName, "UTF-8");
                android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, 
                        android.net.Uri.parse("https://www.youtube.com/results?search_query=" + query));
                browserIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(browserIntent);
            } catch (Exception ex) {
                Toast.makeText(this, "Could not open music app.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String avatarForScreen(String screen) {
        switch (screen) {
            case SCREEN_LIVE:
                return LIVE_AVATAR_URL;
            case SCREEN_PLAYER:
                return PLAYER_AVATAR_URL;
            case SCREEN_SETTINGS:
            case SCREEN_WELLNESS:
                return WELLNESS_AVATAR_URL;
            case SCREEN_HOME:
            default:
                return HOME_AVATAR_URL;
        }
    }

    private void updateBottomNav(String screen) {
        boolean showNav = SCREEN_HOME.equals(screen)
                || SCREEN_LIVE.equals(screen)
                || SCREEN_PLAYER.equals(screen)
                || SCREEN_SETTINGS.equals(screen)
                || SCREEN_WELLNESS.equals(screen);
        bottomNav.setVisibility(showNav ? View.VISIBLE : View.GONE);

        setNavSelected(R.id.navHome, SCREEN_HOME.equals(screen));
        setNavSelected(R.id.navLive, SCREEN_LIVE.equals(screen));
        setNavSelected(R.id.navPlayer, SCREEN_PLAYER.equals(screen));
        setNavSelected(R.id.navWellness, SCREEN_WELLNESS.equals(screen));
    }

    private void setNavSelected(int navId, boolean selected) {
        View navItem = findViewById(navId);
        navItem.setSelected(selected);
        if (navItem instanceof LinearLayout) {
            LinearLayout container = (LinearLayout) navItem;
            for (int i = 0; i < container.getChildCount(); i++) {
                container.getChildAt(i).setSelected(selected);
            }
        }
    }

    private void setupBlePermissionLauncher() {
        blePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (hasRequiredBlePermissions()) {
                        startBleScan();
                    } else {
                        updateBleStatus("Bluetooth permission was not granted.");
                    }
                });
    }

    private void connectLiveBleWatch() {
        if (bluetoothAdapter == null) {
            updateBleStatus("Bluetooth is not available on this phone.");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            updateBleStatus("Turn on Bluetooth, then tap Connect again.");
            return;
        }

        if (!hasRequiredBlePermissions()) {
            blePermissionLauncher.launch(requiredBlePermissions());
            return;
        }

        if (!isLocationReadyForBleScan()) {
            openLocationSettingsForBleScan();
            return;
        }

        startBleScan();
    }

    @SuppressLint("MissingPermission")
    private void startBleScan() {
        if (!hasRequiredBlePermissions()) {
            blePermissionLauncher.launch(requiredBlePermissions());
            return;
        }

        if (!isLocationReadyForBleScan()) {
            openLocationSettingsForBleScan();
            return;
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            updateBleStatus("Turn on Bluetooth, then tap Connect again.");
            return;
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bluetoothLeScanner == null) {
            updateBleStatus("BLE scanner is not available.");
            return;
        }

        bleScanning = true;
        if (isServiceBound && bluetoothService != null) {
            bluetoothService.disconnectAndStop();
        }
        clearBleDeviceList();
        updateBleStatus("Scanning nearby BLE watches. Tap your watch when it appears.");
        addBondedBleDevices();
        handler.removeCallbacks(bleListRefreshRunnable);
        handler.post(bleListRefreshRunnable);
        try {
            bluetoothLeScanner.startScan(null, buildBleScanSettings(), bleScanCallback);
        } catch (SecurityException exception) {
            bleScanning = false;
            handler.removeCallbacks(bleListRefreshRunnable);
            updateBleStatus("Bluetooth scan permission is missing. Allow Nearby devices and try again.");
            return;
        } catch (IllegalStateException exception) {
            bleScanning = false;
            handler.removeCallbacks(bleListRefreshRunnable);
            updateBleStatus("Bluetooth scanner could not start. Toggle Bluetooth and try again.");
            return;
        }
        handler.postDelayed(() -> {
            if (bleScanning) {
                stopBleScan();
                updateBleStatus(discoveredBleDeviceCount() == 0
                        ? "No watches found. Keep it nearby and disconnect it from other apps."
                        : "Live scan paused. Tap your watch, or tap Scan to refresh.");
            }
        }, BLE_SCAN_TIMEOUT_MS);
    }

    @SuppressLint("MissingPermission")
    private void stopBleScan() {
        if (!bleScanning || bluetoothLeScanner == null || !hasBleScanPermission()) {
            bleScanning = false;
            return;
        }

        try {
            bluetoothLeScanner.stopScan(bleScanCallback);
        } catch (SecurityException ignored) {
            // Permission can be revoked while the scan is running.
        }
        bleScanning = false;
        handler.removeCallbacks(bleListRefreshRunnable);
    }

    private ScanSettings buildBleScanSettings() {
        return new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(0)
                .build();
    }

    @SuppressLint("MissingPermission")
    private void addBondedBleDevices() {
        if (bluetoothAdapter == null || !hasBleConnectPermission()) {
            return;
        }

        Set<BluetoothDevice> bondedDevices;
        try {
            bondedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException exception) {
            return;
        }

        if (bondedDevices == null) {
            return;
        }

        for (BluetoothDevice device : bondedDevices) {
            String name = device.getName();
            String address = device.getAddress();
            if (isEmpty(name) && isEmpty(address)) {
                continue;
            }

            int type = device.getType();
            if (type != BluetoothDevice.DEVICE_TYPE_LE
                    && type != BluetoothDevice.DEVICE_TYPE_DUAL
                    && type != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
                continue;
            }

            BleDeviceCandidate candidate = new BleDeviceCandidate(
                    device,
                    isEmpty(name) ? "Paired BLE device" : name,
                    address,
                    0,
                    false,
                    false,
                    true,
                    0L);
            addOrUpdateBleDevice(candidate);
        }
    }

    private String scanErrorMessage(int errorCode) {
        switch (errorCode) {
            case ScanCallback.SCAN_FAILED_ALREADY_STARTED:
                return "BLE scan is already running. Wait a moment and try again.";
            case ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                return "Phone could not register BLE scan. Toggle Bluetooth and reopen the app.";
            case ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                return "BLE scanning is not supported on this phone.";
            case ScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                return "Bluetooth scan internal error. Toggle Bluetooth and try again.";
            default:
                return "BLE scan failed: " + errorCode;
        }
    }

    private void connectSelectedBleDevice(BleDeviceCandidate candidate) {
        selectedBleDeviceLabel = candidate.name;
        stopBleScan();
        updateBleStatus("Connecting to " + selectedBleDeviceLabel + "...");
        Intent serviceIntent = new Intent(this, BluetoothForegroundService.class);
        serviceIntent.putExtra("MAC_ADDRESS", candidate.device.getAddress());
        serviceIntent.putExtra("DEVICE_NAME", candidate.name);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private void addOrUpdateBleDevice(BleDeviceCandidate candidate) {
        int deviceCount;
        int liveDeviceCount = 0;
        synchronized (discoveredBleDevices) {
            BleDeviceCandidate existing = discoveredBleDevices.get(candidate.address);
            discoveredBleDevices.put(candidate.address, candidate.merge(existing));
            deviceCount = discoveredBleDevices.size();
            for (BleDeviceCandidate deviceCandidate : discoveredBleDevices.values()) {
                if (deviceCandidate.isLive()) {
                    liveDeviceCount++;
                }
            }
        }

        int count = deviceCount;
        int liveCount = liveDeviceCount;
        handler.post(() -> {
            if (bleScanning) {
                bleStatusText = liveCount > 0
                        ? "Live scan: " + liveCount + " nearby, " + count + " total. Tap your watch."
                        : "Found " + count + " paired device" + (count == 1 ? "" : "s")
                                + ". Waiting for nearby live scan...";
            }
            applyBleStatus(currentContent);
        });
    }

    private void clearBleDeviceList() {
        synchronized (discoveredBleDevices) {
            discoveredBleDevices.clear();
        }
        handler.post(() -> renderBleDeviceList(currentContent));
    }

    private int discoveredBleDeviceCount() {
        synchronized (discoveredBleDevices) {
            return discoveredBleDevices.size();
        }
    }

    private void renderBleDeviceList(View root) {
        if (root == null) return;

        TextView deviceSelector = root.findViewById(R.id.bleDeviceSelector);
        if (deviceSelector == null) return;

        List<BleDeviceCandidate> candidates;
        synchronized (discoveredBleDevices) {
            candidates = new ArrayList<>(discoveredBleDevices.values());
        }
        candidates.sort(Comparator
                .comparing((BleDeviceCandidate candidate) -> !candidate.isLive())
                .thenComparing(candidate -> !candidate.hasCustomLiveService)
                .thenComparing(candidate -> !candidate.hasStandardHeartRateService)
                .thenComparing(Comparator.comparingLong((BleDeviceCandidate candidate) ->
                        candidate.lastSeenMillis).reversed())
                .thenComparing(Comparator.comparingInt((BleDeviceCandidate candidate) -> candidate.rssi).reversed()));

        if (candidates.isEmpty()) {
            if (bleScanning) {
                deviceSelector.setText("Searching for nearby BLE watches...");
                deviceSelector.setVisibility(View.VISIBLE);
                deviceSelector.setOnClickListener(null);
            } else {
                deviceSelector.setVisibility(View.GONE);
            }
        } else {
            deviceSelector.setVisibility(View.VISIBLE);
            deviceSelector.setText("Select a device (" + candidates.size() + " found) ▾");
            
            deviceSelector.setOnClickListener(v -> {
                com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                    new com.google.android.material.bottomsheet.BottomSheetDialog(this);
                
                // Create a container layout
                LinearLayout bottomSheetView = new LinearLayout(this);
                bottomSheetView.setOrientation(LinearLayout.VERTICAL);
                bottomSheetView.setBackgroundResource(R.drawable.glass_panel_rounded_32);
                bottomSheetView.setPadding(dp(20), dp(20), dp(20), dp(20));

                TextView title = new TextView(this);
                title.setText("Select Bluetooth Device");
                title.setTextColor(ContextCompat.getColor(this, R.color.mood_cyan));
                title.setTextSize(18);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setPadding(0, 0, 0, dp(16));
                bottomSheetView.addView(title);

                android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
                LinearLayout listContainer = new LinearLayout(this);
                listContainer.setOrientation(LinearLayout.VERTICAL);

                int limit = Math.min(candidates.size(), 40);
                for (int i = 0; i < limit; i++) {
                    BleDeviceCandidate candidate = candidates.get(i);
                    TextView row = createBleInfoRow(candidate.displayText());
                    row.setOnClickListener(view -> {
                        bottomSheetDialog.dismiss();
                        deviceSelector.setText(candidate.name + " selected ▾");
                        connectSelectedBleDevice(candidate);
                    });
                    listContainer.addView(row);
                }

                scrollView.addView(listContainer);
                
                // Set max height for ScrollView so it doesn't cover the whole screen if there are 40 devices
                LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    dp(400)
                );
                scrollView.setLayoutParams(scrollParams);
                
                bottomSheetView.addView(scrollView);
                bottomSheetDialog.setContentView(bottomSheetView);
                
                // Set background of bottom sheet itself to transparent so our custom rounded corners show
                View parent = (View) bottomSheetView.getParent();
                if (parent != null) {
                    parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                }
                
                bottomSheetDialog.show();
            });
        }
    }

    private TextView createBleInfoRow(String text) {
        TextView row = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        row.setLayoutParams(params);
        row.setBackgroundResource(R.drawable.chip_bg);
        row.setClickable(true);
        row.setFocusable(true);
        row.setLineSpacing(dp(2), 1.0f);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setText(text);
        row.setTextColor(ContextCompat.getColor(this, R.color.mood_text));
        row.setTextSize(13);
        return row;
    }

    private boolean scanResultHasService(ScanResult result, UUID serviceUuid) {
        if (result.getScanRecord() == null || result.getScanRecord().getServiceUuids() == null) {
            return false;
        }

        for (ParcelUuid parcelUuid : result.getScanRecord().getServiceUuids()) {
            if (serviceUuid.equals(parcelUuid.getUuid())) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private boolean hasRequiredBlePermissions() {
        for (String permission : requiredBlePermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBleScanPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && hasFineLocationPermission();
        }

        return hasFineLocationPermission();
    }

    private boolean hasBleConnectPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isLocationReadyForBleScan() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        }

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void openLocationSettingsForBleScan() {
        updateBleStatus("Turn on phone Location, then return and tap Scan. Android needs it for BLE discovery.");
        try {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (Exception ignored) {
            // Some devices block direct settings launch; the status text still tells the user what to fix.
        }
    }

    private String[] requiredBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }

        return new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }

    private void updateBleStatus(String message) {
        bleStatusText = message;
        handler.post(() -> applyBleStatus(currentContent));
    }

    private void applyBleStatus(View root) {
        if (root == null) {
            return;
        }

        setTextIfPresent(root, R.id.bleStatusText, bleStatusText);
        setTextIfPresent(root, R.id.liveBleStatusText, bleStatusText);
        renderBleDeviceList(root);
    }

    private void setupHealthConnectPermissionLauncher() {
        healthPermissionLauncher = registerForActivityResult(
                PermissionController.createRequestPermissionResultContract(),
                grantedPermissions -> {
                    if (grantedPermissions != null && grantedPermissions.containsAll(HEALTH_PERMISSIONS)) {
                        readHealthDataAsync();
                    } else {
                        updateHealthStatus("Health Connect permission was not granted.");
                    }
                });
    }

    private void syncHealthConnect(boolean requestIfMissing) {
        int sdkStatus = HealthConnectClient.getSdkStatus(this);
        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            updateHealthStatus("Health Connect is not available on this device.");
            return;
        }

        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            updateHealthStatus("Install or update Health Connect, then sync again.");
            return;
        }

        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("prefHealthConnectEnabled", false);
        if (!isEnabled) {
            if (requestIfMissing) {
                android.widget.Toast.makeText(this, "Please enable Health Sync in Settings first.", android.widget.Toast.LENGTH_LONG).show();
            }
            return;
        }

        if (healthConnectClient == null) {
            healthConnectClient = HealthConnectClient.getOrCreate(this);
        }

        imageExecutor.execute(() -> {
            try {
                Set<String> grantedPermissions = runBlockingResult(
                        continuation -> healthConnectClient.getPermissionController()
                                .getGrantedPermissions(continuation));

                if (grantedPermissions.containsAll(HEALTH_PERMISSIONS)) {
                    readHealthDataAsync();
                } else if (requestIfMissing) {
                    handler.post(() -> healthPermissionLauncher.launch(HEALTH_PERMISSIONS));
                } else {
                    updateHealthStatus("Connect to read today's heart rate and steps.");
                }
            } catch (Exception exception) {
                updateHealthStatus("Health Connect could not be checked.");
            }
        });
    }

    private void readHealthDataAsync() {
        updateHealthStatus("Syncing today's Health Connect data...");
        imageExecutor.execute(() -> {
            try {
                HealthSnapshot snapshot = readHealthDataBlocking();
                latestHealthSnapshot = snapshot;
                handler.post(() -> applyHealthSnapshot(currentContent));
            } catch (Exception exception) {
                updateHealthStatus("Could not read Health Connect data.");
            }
        });
    }

    private HealthSnapshot readHealthDataBlocking() {
        Instant endTime = Instant.now();
        Instant startTime = ZonedDateTime.now()
                .toLocalDate()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();
        TimeRangeFilter today = TimeRangeFilter.between(startTime, endTime);

        ReadRecordsRequest<HeartRateRecord> heartRequest = new ReadRecordsRequest<>(
                JvmClassMappingKt.getKotlinClass(HeartRateRecord.class),
                today,
                Collections.emptySet(),
                false,
                1000,
                null);
        ReadRecordsResponse<HeartRateRecord> heartResponse =
                this.<ReadRecordsResponse<HeartRateRecord>>runBlockingResult(
                        continuation -> healthConnectClient.readRecords(heartRequest, continuation));

        ReadRecordsRequest<StepsRecord> stepsRequest = new ReadRecordsRequest<>(
                JvmClassMappingKt.getKotlinClass(StepsRecord.class),
                today,
                Collections.emptySet(),
                false,
                1000,
                null);
        ReadRecordsResponse<StepsRecord> stepsResponse =
                this.<ReadRecordsResponse<StepsRecord>>runBlockingResult(
                        continuation -> healthConnectClient.readRecords(stepsRequest, continuation));

        long latestBpm = 0L;
        long bpmTotal = 0L;
        long bpmCount = 0L;
        Instant latestSampleTime = Instant.MIN;
        for (HeartRateRecord record : heartResponse.getRecords()) {
            for (HeartRateRecord.Sample sample : record.getSamples()) {
                long bpm = sample.getBeatsPerMinute();
                bpmTotal += bpm;
                bpmCount++;
                if (sample.getTime().isAfter(latestSampleTime)) {
                    latestSampleTime = sample.getTime();
                    latestBpm = bpm;
                }
            }
        }

        long steps = 0L;
        for (StepsRecord record : stepsResponse.getRecords()) {
            steps += record.getCount();
        }

        if (bpmCount == 0L && steps == 0L) {
            return new HealthSnapshot("--", "0", "84", "No Health Connect records found for today.");
        }

        long averageBpm = bpmCount == 0L ? 0L : Math.round((double) bpmTotal / bpmCount);
        String bpmText = bpmCount == 0L ? "--" : String.valueOf(latestBpm);
        String stepsText = String.valueOf(steps);
        String wellnessText = String.valueOf(calculateWellnessScore(averageBpm, steps));
        String status = bpmCount == 0L
                ? "Synced today: " + formatNumber(steps) + " steps. No heart-rate samples yet."
                : "Synced today: " + latestBpm + " BPM latest, " + averageBpm
                        + " BPM avg, " + formatNumber(steps) + " steps.";

        return new HealthSnapshot(bpmText, stepsText, wellnessText, status);
    }

    private int calculateWellnessScore(long averageBpm, long steps) {
        int score = 62 + (int) Math.min(24, steps / 450);
        if (averageBpm > 0L) {
            score += averageBpm >= 55L && averageBpm <= 85L ? 12 : 4;
            score -= (int) Math.max(0, averageBpm - 92L) / 2;
        }
        return Math.max(35, Math.min(98, score));
    }

    private String inferStressLabel(long averageBpm, long steps) {
        return currentMoodLabel;
    }

    private String formatNumber(long value) {
        return String.format("%,d", value);
    }

    private void updateHealthStatus(String message) {
        latestHealthSnapshot = latestHealthSnapshot.withStatus(message);
        handler.post(() -> applyHealthSnapshot(currentContent));
    }

    private void applyHealthSnapshot(View root) {
        if (root == null) {
            return;
        }

        setTextIfPresent(root, R.id.healthStatusText, latestHealthSnapshot.statusText);
        setTextIfPresent(root, R.id.homePulseValue, latestHealthSnapshot.bpmText);
        setTextIfPresent(root, R.id.livePulseValue, latestHealthSnapshot.bpmText);
        setTextIfPresent(root, R.id.homeStepsValue, latestHealthSnapshot.stepsText);
        setTextIfPresent(root, R.id.liveStepsValue, latestHealthSnapshot.stepsText);
        setTextIfPresent(root, R.id.homeWellnessScore, latestHealthSnapshot.wellnessScoreText);
        setTextIfPresent(root, R.id.tvEmotionalScoreValue, latestHealthSnapshot.wellnessScoreText + " / 100");

        long bpm = parseLongSafe(latestHealthSnapshot.bpmText);
        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        setTextIfPresent(root, R.id.liveMoodText, calculateLiveMood(bpm, steps));
        
        String emoji = "⏳";
        if (currentMoodLabel != null) {
            String moodLower = currentMoodLabel.toLowerCase();
            if (moodLower.contains("calm") || moodLower.contains("resting") || moodLower.contains("relax")) {
                emoji = "😌";
            } else if (moodLower.contains("focus") || moodLower.contains("energetic") || moodLower.contains("active")) {
                emoji = "⚡";
            } else if (moodLower.contains("stress") || moodLower.contains("anxious") || moodLower.contains("high")) {
                emoji = "😰";
            } else if (!moodLower.contains("calibrating")) {
                emoji = "🎵";
            }
        }
        setTextIfPresent(root, R.id.liveMoodEmoji, emoji);
        
        PulseScannerView pulseScanner = root.findViewById(R.id.pulseScanner);
        if (pulseScanner != null && bpm > 0) {
            pulseScanner.setBpm((int) bpm);
        }
        
        // Save to Room DB
        if (bpm > 0) {
            long score = calculateWellnessScore(bpm, steps);
            dbExecutor.execute(() -> {
                try {
                    com.example.genzmusicapp.db.AppDatabase db = com.example.genzmusicapp.db.AppDatabase.getDatabase(this);
                    com.example.genzmusicapp.db.WellnessHistory wh = new com.example.genzmusicapp.db.WellnessHistory();
                    wh.bpm = bpm;
                    wh.steps = steps;
                    wh.calculatedScore = score;
                    wh.calculatedMood = currentMoodLabel;
                    wh.timestamp = System.currentTimeMillis();
                    wh.userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "guest";
                    db.wellnessDao().insert(wh);
                } catch (Exception ignored) {}
            });
        }
        

    }

    private String calculateLiveMood(long bpm, long steps) {
        if (bpm <= 0) {
            if (steps > 0) return "Activity logged: " + formatNumber(steps) + " steps. Awaiting live heart rate data...";
            return "Calibrating neural biosensors. Waiting for stable biometric readings...";
        }
        return "Mood: " + currentMoodLabel;
    }

    private void setTextIfPresent(View root, int viewId, String text) {
        TextView textView = root.findViewById(viewId);
        if (textView != null) {
            textView.setText(text);
        }
    }



    private <T> T runBlockingResult(SuspendingBlock<T> block) {
        Function2<CoroutineScope, Continuation<? super T>, Object> function =
                (scope, continuation) -> block.run(continuation);
        try {
            return (T) BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE, function);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Health Connect coroutine was interrupted.", exception);
        }
    }

    private void loadImage(String url, ImageView imageView) {
        Bitmap cached = imageCache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        imageExecutor.execute(() -> {
            Bitmap bitmap = null;
            if (url.startsWith("content://") || url.startsWith("file://")) {
                try {
                    android.net.Uri uri = android.net.Uri.parse(url);
                    java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                    if (inputStream != null) {
                        bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                        inputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                bitmap = downloadBitmap(url);
            }
            
            if (bitmap == null) {
                return;
            }

            imageCache.put(url, bitmap);
            Bitmap finalBitmap = bitmap;
            handler.post(() -> imageView.setImageBitmap(finalBitmap));
        });
    }

    private Bitmap downloadBitmap(String urlValue) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlValue);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.connect();
            try (InputStream stream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(stream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private interface SuspendingBlock<T> {
        Object run(Continuation<? super T> continuation);
    }


    private static final class BleDeviceCandidate {
        private final BluetoothDevice device;
        private final String name;
        private final String address;
        private final int rssi;
        private final boolean hasStandardHeartRateService;
        private final boolean hasCustomLiveService;
        private final boolean bonded;
        private final long lastSeenMillis;

        private BleDeviceCandidate(
                BluetoothDevice device,
                String name,
                String address,
                int rssi,
                boolean hasStandardHeartRateService,
                boolean hasCustomLiveService,
                boolean bonded,
                long lastSeenMillis) {
            this.device = device;
            this.name = name;
            this.address = address == null ? name : address;
            this.rssi = rssi;
            this.hasStandardHeartRateService = hasStandardHeartRateService;
            this.hasCustomLiveService = hasCustomLiveService;
            this.bonded = bonded;
            this.lastSeenMillis = lastSeenMillis;
        }

        private BleDeviceCandidate merge(BleDeviceCandidate existing) {
            if (existing == null) {
                return this;
            }

            String mergedName = isGenericName(name) && !isGenericName(existing.name)
                    ? existing.name
                    : name;
            return new BleDeviceCandidate(
                    device,
                    mergedName,
                    address,
                    isLive() ? rssi : existing.rssi,
                    hasStandardHeartRateService || existing.hasStandardHeartRateService,
                    hasCustomLiveService || existing.hasCustomLiveService,
                    bonded || existing.bonded,
                    Math.max(lastSeenMillis, existing.lastSeenMillis));
        }

        private String displayText() {
            String signalText = isLive() ? "nearby now, " + rssi + " dBm" : "paired";
            return name + "\n" + address + " - " + signalText + " - " + serviceLabel();
        }

        private String serviceLabel() {
            if (hasCustomLiveService) {
                return "live watch data";
            }
            if (hasStandardHeartRateService) {
                return "standard heart rate";
            }
            if (bonded) {
                return "paired watch/device";
            }
            return "BLE device";
        }

        private boolean isLive() {
            return lastSeenMillis > 0L;
        }

        private static boolean isGenericName(String value) {
            return value == null
                    || value.trim().isEmpty()
                    || "Unknown BLE device".equals(value)
                    || "Paired BLE device".equals(value);
        }
    }

    private static final class HealthSnapshot {
        private final String bpmText;
        private final String stepsText;
        private final String wellnessScoreText;
        private final String statusText;

        private HealthSnapshot(String bpmText, String stepsText, String wellnessScoreText, String statusText) {
            this.bpmText = bpmText;
            this.stepsText = stepsText;
            this.wellnessScoreText = wellnessScoreText;
            this.statusText = statusText;
        }

        private static HealthSnapshot defaultValues() {
            return new HealthSnapshot("--", "--", "--", "Connect a device to read today's heart rate.");
        }

        private HealthSnapshot withStatus(String statusText) {
            return new HealthSnapshot(bpmText, stepsText, wellnessScoreText, statusText);
        }

        private HealthSnapshot withBpm(String bpmText) {
            return new HealthSnapshot(bpmText, stepsText, wellnessScoreText, statusText);
        }

        private HealthSnapshot withSteps(String stepsText) {
            return new HealthSnapshot(bpmText, stepsText, wellnessScoreText, statusText);
        }

        private HealthSnapshot withWellnessScore(String wellnessScoreText) {
            return new HealthSnapshot(bpmText, stepsText, wellnessScoreText, statusText);
        }
    }
}
