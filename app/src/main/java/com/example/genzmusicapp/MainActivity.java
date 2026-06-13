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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.os.Build;
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
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.ProgressBar;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

import kotlin.coroutines.Continuation;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.JvmClassMappingKt;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineScope;

public class MainActivity extends AppCompatActivity {
    private static final String SCREEN_SPLASH = "splash";
    private static final String SCREEN_ONBOARDING = "onboarding";
    private static final String SCREEN_HOME = "home";
    private static final String SCREEN_LIVE = "live";
    private static final String SCREEN_PLAYER = "player";
    private static final String SCREEN_SETTINGS = "settings";

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
    private final Map<String, Bitmap> imageCache = new HashMap<>();

    private FrameLayout screenContainer;
    private LinearLayout bottomNav;
    private ActivityResultLauncher<Set<String>> healthPermissionLauncher;
    private ActivityResultLauncher<String[]> blePermissionLauncher;
    private HealthConnectClient healthConnectClient;
    private HealthSnapshot latestHealthSnapshot = HealthSnapshot.defaultValues();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt watchGatt;
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

    private final BluetoothGattCallback watchGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateBleStatus("BLE connection failed for " + selectedBleDeviceLabel
                        + " with GATT status " + status + ".");
                closeWatchGatt();
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                updateBleStatus("Connected to " + selectedBleDeviceLabel + ". Preparing BLE session...");
                handler.post(() -> syncHealthConnect(false));
                if (hasBleConnectPermission()) {
                    boolean mtuStarted = gatt.requestMtu(247);
                    if (!mtuStarted) {
                        gatt.discoverServices();
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                updateBleStatus(selectedBleDeviceLabel + " disconnected. Scan again to reconnect.");
                closeWatchGatt();
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if (hasBleConnectPermission()) {
                updateBleStatus("Discovering live services on " + selectedBleDeviceLabel + "...");
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                updateBleStatus("Service discovery failed: " + status);
                return;
            }

            int serviceCount = gatt.getServices() == null ? 0 : gatt.getServices().size();
            BluetoothGattService customService = gatt.getService(BLE_CUSTOM_LIVE_SERVICE_UUID);
            if (customService != null) {
                BluetoothGattCharacteristic customNotifyCharacteristic =
                        customService.getCharacteristic(BLE_CUSTOM_NOTIFY_CHARACTERISTIC_UUID);
                if (customNotifyCharacteristic != null) {
                    enableLiveNotifications(gatt, customNotifyCharacteristic);
                    return;
                }
            }

            BluetoothGattService heartRateService = gatt.getService(BLE_STANDARD_HEART_RATE_SERVICE_UUID);
            if (heartRateService != null) {
                BluetoothGattCharacteristic heartRateCharacteristic =
                        heartRateService.getCharacteristic(BLE_STANDARD_HEART_RATE_MEASUREMENT_UUID);
                if (heartRateCharacteristic != null) {
                    enableLiveNotifications(gatt, heartRateCharacteristic);
                    return;
                }
            }

            BluetoothGattCharacteristic fallbackNotifyCharacteristic = findFirstNotifyCharacteristic(gatt);
            if (fallbackNotifyCharacteristic != null) {
                updateBleStatus("Using generic notify channel "
                        + shortUuid(fallbackNotifyCharacteristic.getUuid()) + " on " + selectedBleDeviceLabel + ".");
                enableLiveNotifications(gatt, fallbackNotifyCharacteristic);
                return;
            }

            updateBleStatus("Connected to " + selectedBleDeviceLabel
                    + " and found " + serviceCount
                    + " services, but no supported live heart-rate channel was found.");
        }

        @Override
        public void onDescriptorWrite(
                BluetoothGatt gatt,
                BluetoothGattDescriptor descriptor,
                int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateBleStatus("Live BLE connected to " + selectedBleDeviceLabel + ". Waiting for data...");
            } else {
                updateBleStatus("BLE notify setup failed on " + selectedBleDeviceLabel
                        + " with status " + status + ".");
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleLiveCharacteristic(characteristic.getUuid(), characteristic.getValue());
        }

        @Override
        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                byte[] value) {
            handleLiveCharacteristic(characteristic.getUuid(), value);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        screenContainer = findViewById(R.id.screenContainer);
        bottomNav = findViewById(R.id.bottomNav);
        setupHealthConnectPermissionLauncher();
        setupBlePermissionLauncher();
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        setupBottomNavigation();
        setupBackNavigation();
        showScreen(SCREEN_SPLASH);

        handler.postDelayed(() -> {
            if (SCREEN_SPLASH.equals(currentScreen)) {
                showScreen(SCREEN_ONBOARDING);
            }
        }, 1700);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBleScan();
        closeWatchGatt();
        imageExecutor.shutdownNow();
    }

    private void setupBottomNavigation() {
        findViewById(R.id.navHome).setOnClickListener(view -> showScreen(SCREEN_HOME));
        findViewById(R.id.navLive).setOnClickListener(view -> showScreen(SCREEN_LIVE));
        findViewById(R.id.navPlayer).setOnClickListener(view -> showScreen(SCREEN_PLAYER));
        findViewById(R.id.navWellness).setOnClickListener(view -> showScreen(SCREEN_SETTINGS));
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (SCREEN_LIVE.equals(currentScreen)
                        || SCREEN_PLAYER.equals(currentScreen)
                        || SCREEN_SETTINGS.equals(currentScreen)) {
                    showScreen(SCREEN_HOME);
                    return;
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
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
            case SCREEN_ONBOARDING:
                return R.layout.screen_onboarding;
            case SCREEN_HOME:
                return R.layout.screen_home;
            case SCREEN_LIVE:
                return R.layout.screen_live_mood;
            case SCREEN_PLAYER:
                return R.layout.screen_player;
            case SCREEN_SETTINGS:
                return R.layout.screen_settings;
            case SCREEN_SPLASH:
            default:
                return R.layout.screen_splash;
        }
    }

    private void configureScreen(View content, String screen) {
        ImageView topAvatar = content.findViewById(R.id.topAvatar);
        if (topAvatar != null) {
            loadImage(avatarForScreen(screen), topAvatar);
        }

        TextView topBrand = content.findViewById(R.id.topBrand);
        if (topBrand != null) {
            topBrand.setOnClickListener(view -> showScreen(SCREEN_HOME));
        }

        ImageButton signalButton = content.findViewById(R.id.signalButton);
        if (signalButton != null) {
            signalButton.setOnClickListener(view -> showScreen(SCREEN_LIVE));
        }

        View getStartedButton = content.findViewById(R.id.getStartedButton);
        if (getStartedButton != null) {
            getStartedButton.setOnClickListener(view -> showScreen(SCREEN_HOME));
        }

        View recommendPlayButton = content.findViewById(R.id.recommendPlayButton);
        if (recommendPlayButton != null) {
            recommendPlayButton.setOnClickListener(view -> showScreen(SCREEN_PLAYER));
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
        } else if (SCREEN_SETTINGS.equals(screen)) {
            configureSettingsScreen(content);
        }

        applyHealthSnapshot(content);
        applyBleStatus(content);
    }

    private void configureSettingsScreen(View content) {
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        
        EditText editLanguage = content.findViewById(R.id.editLanguage);
        EditText editGenres = content.findViewById(R.id.editGenres);
        EditText editArtists = content.findViewById(R.id.editArtists);
        EditText editMusicType = content.findViewById(R.id.editMusicType);
        
        if (editLanguage != null) editLanguage.setText(prefs.getString("prefLanguage", "English"));
        if (editGenres != null) editGenres.setText(prefs.getString("prefGenres", "Pop, Lo-Fi"));
        if (editArtists != null) editArtists.setText(prefs.getString("prefArtists", "The Weeknd"));
        if (editMusicType != null) editMusicType.setText(prefs.getString("prefMusicType", "Vocal"));

        View saveBtn = content.findViewById(R.id.saveSettingsButton);
        if (saveBtn != null) {
            saveBtn.setOnClickListener(v -> {
                SharedPreferences.Editor editor = prefs.edit();
                if (editLanguage != null) editor.putString("prefLanguage", editLanguage.getText().toString());
                if (editGenres != null) editor.putString("prefGenres", editGenres.getText().toString());
                if (editArtists != null) editor.putString("prefArtists", editArtists.getText().toString());
                if (editMusicType != null) editor.putString("prefMusicType", editMusicType.getText().toString());
                editor.apply();
                Toast.makeText(MainActivity.this, "Preferences Saved!", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void configurePlayerScreen(View content) {
        TextView refreshButton = content.findViewById(R.id.playerAiRecButton);
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> generateRecommendations());
        }
        
        generateRecommendations();
    }

    private void generateRecommendations() {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Syncing biometrics and preferences...");
        
        SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        String prefLanguage = prefs.getString("prefLanguage", "").trim();
        String prefGenres = prefs.getString("prefGenres", "").trim();
        
        if (prefLanguage.isEmpty() && prefGenres.isEmpty()) {
            if (subtitle != null) subtitle.setText("Please set your preferences in Settings first.");
            return;
        }

        long bpm = parseLongSafe(latestHealthSnapshot.bpmText);
        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        
        if (bpm <= 0) {
            if (subtitle != null) subtitle.setText("Awaiting stable biometric readings...");
            return;
        }

        String stressLevel = "Balanced";
        String moodQuery = "focus";
        
        if (bpm > 100 && steps > 1000) {
            stressLevel = "Energetic";
            moodQuery = "workout energy";
        } else if (bpm > 100) {
            stressLevel = "High Stress";
            moodQuery = "relaxation meditation calm";
        } else if (bpm < 60) {
            stressLevel = "Deeply Relaxed";
            moodQuery = "sleep ambient";
        }

        String finalStressLevel = stressLevel;
        if (subtitle != null) subtitle.setText("Curating " + finalStressLevel + " recommendations...");
        
        String searchQuery = moodQuery + " " + prefGenres + " " + prefLanguage;
        fetchItunesSongs(searchQuery, finalStressLevel, prefGenres, prefLanguage);
    }
    
    private void fetchItunesSongs(String query, String stressLevel, String genre, String language) {
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(View.VISIBLE);
        LinearLayout container = currentContent.findViewById(R.id.playerPlaylistContainer);
        if (container != null) container.removeAllViews();
        
        imageExecutor.execute(() -> {
            try {
                String encodedQuery = java.net.URLEncoder.encode(query, "UTF-8");
                URL url = new URL("https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=40");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                org.json.JSONArray results = jsonResponse.getJSONArray("results");
                
                List<org.json.JSONObject> songList = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    songList.add(results.getJSONObject(i));
                }
                java.util.Collections.shuffle(songList);
                int count = Math.min(20, songList.size());
                List<org.json.JSONObject> finalSongs = new ArrayList<>(songList.subList(0, count));

                handler.post(() -> renderRecommendations(finalSongs, stressLevel, genre, language));
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    if (loading != null) loading.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, "Failed to fetch songs.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void renderRecommendations(List<org.json.JSONObject> songs, String stressLevel, String genrePref, String langPref) {
        if (currentContent == null || !SCREEN_PLAYER.equals(currentScreen)) return;
        
        ProgressBar loading = currentContent.findViewById(R.id.recommendationLoading);
        if (loading != null) loading.setVisibility(View.GONE);
        
        TextView subtitle = currentContent.findViewById(R.id.recommendationSubtitle);
        if (subtitle != null) subtitle.setText("Found " + songs.size() + " matches for your " + stressLevel + " state.");

        LinearLayout container = currentContent.findViewById(R.id.playerPlaylistContainer);
        if (container == null) return;
        container.removeAllViews();

        for (org.json.JSONObject song : songs) {
            try {
                View itemView = LayoutInflater.from(this).inflate(R.layout.item_recommendation, container, false);
                
                String trackName = song.optString("trackName", "Unknown Song");
                String artistName = song.optString("artistName", "Unknown Artist");
                String genre = song.optString("primaryGenreName", "Unknown");
                String artworkUrl = song.optString("artworkUrl100", "");
                String trackId = String.valueOf(song.optLong("trackId"));

                setTextIfPresent(itemView, R.id.itemSongName, trackName);
                setTextIfPresent(itemView, R.id.itemArtistName, artistName);
                setTextIfPresent(itemView, R.id.itemGenre, genre);
                setTextIfPresent(itemView, R.id.itemLanguage, langPref.isEmpty() ? "Global" : langPref);
                
                String reason = "Recommended because your current stress level is " + stressLevel.toLowerCase() + " and this matches your preferences.";
                setTextIfPresent(itemView, R.id.itemReason, reason);

                ImageView albumArt = itemView.findViewById(R.id.itemAlbumArt);
                if (albumArt != null && !artworkUrl.isEmpty()) {
                    loadImage(artworkUrl, albumArt);
                }

                itemView.setOnClickListener(v -> resolveSpotifyTrack(trackId, trackName, artistName));
                container.addView(itemView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void resolveSpotifyTrack(String itunesTrackId, String trackName, String artistName) {
        Toast.makeText(this, "Opening exact track in Spotify...", Toast.LENGTH_SHORT).show();
        imageExecutor.execute(() -> {
            try {
                URL url = new URL("https://api.song.link/v1-alpha.1/links?platform=itunes&type=song&id=" + itunesTrackId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    handler.post(() -> fallbackToSpotifySearchIntent(trackName, artistName));
                    return;
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();

                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                org.json.JSONObject links = jsonResponse.optJSONObject("linksByPlatform");
                if (links != null && links.has("spotify")) {
                    org.json.JSONObject spotify = links.getJSONObject("spotify");
                    String spotifyUrl = spotify.optString("url");
                    if (spotifyUrl != null && !spotifyUrl.isEmpty()) {
                        handler.post(() -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(spotifyUrl));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            try {
                                startActivity(intent);
                            } catch (Exception e) {
                                fallbackToSpotifySearchIntent(trackName, artistName);
                            }
                        });
                        return;
                    }
                }
                
                handler.post(() -> fallbackToSpotifySearchIntent(trackName, artistName));
            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> fallbackToSpotifySearchIntent(trackName, artistName));
            }
        });
    }

    private void fallbackToSpotifySearchIntent(String trackName, String artistName) {
        try {
            Intent intent = new Intent(android.provider.MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
            intent.putExtra(android.provider.MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio");
            intent.putExtra(android.app.SearchManager.QUERY, trackName + " " + artistName);
            intent.setPackage("com.spotify.music");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to launch Spotify. Please ensure the app is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private String avatarForScreen(String screen) {
        switch (screen) {
            case SCREEN_LIVE:
                return LIVE_AVATAR_URL;
            case SCREEN_PLAYER:
                return PLAYER_AVATAR_URL;
            case SCREEN_SETTINGS:
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
                || SCREEN_SETTINGS.equals(screen);
        bottomNav.setVisibility(showNav ? View.VISIBLE : View.GONE);

        setNavSelected(R.id.navHome, SCREEN_HOME.equals(screen));
        setNavSelected(R.id.navLive, SCREEN_LIVE.equals(screen));
        setNavSelected(R.id.navPlayer, SCREEN_PLAYER.equals(screen));
        setNavSelected(R.id.navWellness, SCREEN_SETTINGS.equals(screen));
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
        closeWatchGatt();
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
        connectGattDevice(candidate.device);
    }

    @SuppressLint("MissingPermission")
    private void connectGattDevice(BluetoothDevice device) {
        if (!hasBleConnectPermission()) {
            updateBleStatus("Bluetooth connect permission is missing.");
            return;
        }

        closeWatchGatt();
        watchGatt = device.connectGatt(this, false, watchGattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    @SuppressLint("MissingPermission")
    private void enableLiveNotifications(
            BluetoothGatt gatt,
            BluetoothGattCharacteristic notifyCharacteristic) {
        if (!hasBleConnectPermission()) {
            updateBleStatus("Bluetooth connect permission is missing.");
            return;
        }

        int properties = notifyCharacteristic.getProperties();
        boolean supportsNotify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        boolean supportsIndicate = (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
        if (!supportsNotify && !supportsIndicate) {
            updateBleStatus("Live channel " + shortUuid(notifyCharacteristic.getUuid())
                    + " does not support notifications.");
            return;
        }

        boolean enabled = gatt.setCharacteristicNotification(notifyCharacteristic, true);
        BluetoothGattDescriptor descriptor = notifyCharacteristic.getDescriptor(BLE_CCCD_UUID);
        if (!enabled || descriptor == null) {
            updateBleStatus("Could not enable notifications on "
                    + shortUuid(notifyCharacteristic.getUuid()) + ".");
            return;
        }

        descriptor.setValue(supportsIndicate
                ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean writeStarted = gatt.writeDescriptor(descriptor);
        updateBleStatus(writeStarted
                ? "Subscribed to " + shortUuid(notifyCharacteristic.getUuid())
                        + " on " + selectedBleDeviceLabel + ". Waiting for data..."
                : "Could not write notification descriptor.");
    }

    @SuppressLint("MissingPermission")
    private void closeWatchGatt() {
        if (watchGatt == null || !hasBleConnectPermission()) {
            watchGatt = null;
            return;
        }

        watchGatt.disconnect();
        watchGatt.close();
        watchGatt = null;
    }

    private void handleLiveCharacteristic(UUID characteristicUuid, byte[] value) {
        if (BLE_STANDARD_HEART_RATE_MEASUREMENT_UUID.equals(characteristicUuid)) {
            decodeStandardHeartRatePacket(value);
            return;
        }

        if (BLE_CUSTOM_NOTIFY_CHARACTERISTIC_UUID.equals(characteristicUuid) || looksLikeCustomLivePacket(value)) {
            decodeCustomLiveWatchPacket(value);
        }
    }

    private BluetoothGattCharacteristic findFirstNotifyCharacteristic(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        if (services == null) {
            return null;
        }

        for (BluetoothGattService service : services) {
            List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
            if (characteristics == null) {
                continue;
            }

            for (BluetoothGattCharacteristic characteristic : characteristics) {
                int properties = characteristic.getProperties();
                boolean canNotify = (properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0
                        || (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
                if (canNotify && characteristic.getDescriptor(BLE_CCCD_UUID) != null) {
                    return characteristic;
                }
            }
        }

        return null;
    }

    private boolean looksLikeCustomLivePacket(byte[] value) {
        return value != null && value.length >= 2 && (value[0] & 0xFF) == 0xBC;
    }

    private String shortUuid(UUID uuid) {
        String uuidText = uuid.toString();
        if (uuidText.startsWith("0000") && uuidText.endsWith("-0000-1000-8000-00805f9b34fb")) {
            return uuidText.substring(4, 8).toUpperCase();
        }
        return uuidText.substring(0, 8);
    }

    private void decodeStandardHeartRatePacket(byte[] value) {
        if (value == null || value.length < 2) {
            return;
        }

        int flags = value[0] & 0xFF;
        int bpm = (flags & 0x01) == 0
                ? value[1] & 0xFF
                : unsignedLittleEndianShort(value, 1);
        updateLiveBpm(bpm);
    }

    private void decodeCustomLiveWatchPacket(byte[] value) {
        if (value == null || value.length < 5 || (value[0] & 0xFF) != 0xBC) {
            return;
        }

        int packetType = value[1] & 0xFF;
        if (packetType == 0x5C && value.length >= 5) {
            int bpm = value[4] & 0xFF;
            updateLiveBpm(bpm);
            return;
        }

        if (packetType == 0x51 || value.length >= 8) {
            Long steps = extractCustomStepCount(value);
            if (steps != null) {
                updateLiveSteps(steps, packetType);
            } else if (packetType == 0x51) {
                updateBleStatus("Step packet received, but step bytes could not be decoded.");
            }
        }
    }

    private void updateLiveBpm(int bpm) {
        if (bpm < 30 || bpm > 220) {
            return;
        }

        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        latestHealthSnapshot = latestHealthSnapshot.withBpm(String.valueOf(bpm));
        latestHealthSnapshot = latestHealthSnapshot.withWellnessScore(
                String.valueOf(calculateWellnessScore(bpm, steps)));
        updateBleStatus("Live BLE from " + selectedBleDeviceLabel + ": " + bpm + " BPM");
        handler.post(() -> applyHealthSnapshot(currentContent));
    }

    private void updateLiveSteps(long steps, int packetType) {
        if (steps < 0L || steps > 300000L) {
            return;
        }

        latestHealthSnapshot = latestHealthSnapshot.withSteps(String.valueOf(steps));
        latestHealthSnapshot = latestHealthSnapshot.withWellnessScore(
                String.valueOf(calculateWellnessScore(parseLongSafe(latestHealthSnapshot.bpmText), steps)));
        updateBleStatus("Live BLE from " + selectedBleDeviceLabel + ": "
                + formatNumber(steps) + " steps"
                + " (packet " + Integer.toHexString(packetType).toUpperCase(Locale.US) + ")");
        handler.post(() -> applyHealthSnapshot(currentContent));
    }

    private Long extractCustomStepCount(byte[] value) {
        long currentSteps = parseLongSafe(latestHealthSnapshot.stepsText);
        if (value.length >= 8) {
            long directSteps = unsignedLittleEndianInt(value, 4);
            if (isPlausibleStepCount(directSteps, currentSteps)) {
                return directSteps;
            }
        }

        if (value.length >= 6) {
            int shortSteps = unsignedLittleEndianShort(value, 4);
            if (isPlausibleStepCount(shortSteps, currentSteps)) {
                return (long) shortSteps;
            }
        }

        for (int offset = 2; offset <= value.length - 4; offset++) {
            long candidate = unsignedLittleEndianInt(value, offset);
            if (isPlausibleStepCount(candidate, currentSteps)) {
                return candidate;
            }
        }

        for (int offset = 2; offset <= value.length - 2; offset++) {
            int candidate = unsignedLittleEndianShort(value, offset);
            if (isPlausibleStepCount(candidate, currentSteps)) {
                return (long) candidate;
            }
        }

        return null;
    }

    private boolean isPlausibleStepCount(long candidate, long currentSteps) {
        if (candidate < 0L || candidate > 300000L) {
            return false;
        }

        return currentSteps == 0L || candidate >= currentSteps || candidate < 1000L;
    }

    private int unsignedLittleEndianShort(byte[] value, int offset) {
        if (value.length <= offset + 1) {
            return 0;
        }

        return (value[offset] & 0xFF) | ((value[offset + 1] & 0xFF) << 8);
    }

    private long unsignedLittleEndianInt(byte[] value, int offset) {
        return ((long) value[offset] & 0xFF)
                | (((long) value[offset + 1] & 0xFF) << 8)
                | (((long) value[offset + 2] & 0xFF) << 16)
                | (((long) value[offset + 3] & 0xFF) << 24);
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
        if (root == null) {
            return;
        }

        LinearLayout deviceList = root.findViewById(R.id.bleDeviceList);
        if (deviceList == null) {
            return;
        }

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

        deviceList.removeAllViews();
        if (candidates.isEmpty()) {
            deviceList.setVisibility(bleScanning ? View.VISIBLE : View.GONE);
            if (bleScanning) {
                deviceList.addView(createBleInfoRow("Searching for nearby BLE watches..."));
            }
            return;
        }

        deviceList.setVisibility(View.VISIBLE);
        int limit = Math.min(candidates.size(), 40);
        for (int i = 0; i < limit; i++) {
            BleDeviceCandidate candidate = candidates.get(i);
            TextView row = createBleInfoRow(candidate.displayText());
            row.setOnClickListener(view -> connectSelectedBleDevice(candidate));
            deviceList.addView(row);
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

    private String inferStressLabel(long averageBpm) {
        if (averageBpm >= 90L) {
            return "Elevated";
        }
        if (averageBpm >= 76L) {
            return "Balanced";
        }
        return "Low";
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
        setTextIfPresent(root, R.id.wellnessScoreValue, latestHealthSnapshot.wellnessScoreText + " / 100");

        long bpm = parseLongSafe(latestHealthSnapshot.bpmText);
        long steps = parseLongSafe(latestHealthSnapshot.stepsText);
        setTextIfPresent(root, R.id.liveMoodText, calculateLiveMood(bpm, steps));
    }

    private String calculateLiveMood(long bpm, long steps) {
        if (bpm <= 0) {
            if (steps > 0) {
                return "Activity logged: " + formatNumber(steps) + " steps. Awaiting live heart rate data for complete mood analysis.";
            }
            return "Calibrating neural biosensors. Waiting for stable biometric readings...";
        }
        if (bpm > 100 && steps > 2000) {
            return "High energy state detected. Your elevated heart rate aligns with recent physical activity.";
        }
        if (bpm > 100) {
            return "Elevated stress or resting heart rate detected. Consider taking a moment to breathe and reset.";
        }
        if (bpm >= 60 && bpm <= 80) {
            return "Your biometric signature indicates a state of optimal tranquility. Heart rate variability is stable.";
        }
        if (bpm < 60 && bpm > 0) {
            return "Deeply relaxed state. Your heart rate is exceptionally calm, indicating strong recovery.";
        }
        return "Your vitals are being monitored. Heart rate and activity levels are balanced.";
    }

    private void setTextIfPresent(View root, int viewId, String text) {
        TextView textView = root.findViewById(viewId);
        if (textView != null) {
            textView.setText(text);
        }
    }

    @SuppressWarnings("unchecked")
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
            Bitmap bitmap = downloadBitmap(url);
            if (bitmap == null) {
                return;
            }

            imageCache.put(url, bitmap);
            handler.post(() -> imageView.setImageBitmap(bitmap));
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
            return new HealthSnapshot("74", "0", "84", "Connect to read today's heart rate and steps.");
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
