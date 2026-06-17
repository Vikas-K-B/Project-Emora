package com.example.genzmusicapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class BluetoothForegroundService extends Service {

    private static final String CHANNEL_ID = "MusicZBleChannel";
    private static final int NOTIFICATION_ID = 1001;

    // Standard BLE UUIDs
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

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt watchGatt;

    private String targetAddress;
    private String targetName = "Watch";
    
    // State
    private int currentBpm = 0;
    private long currentSteps = 0;
    private BiometricClassifier biometricClassifier;

    private static final String ALERT_CHANNEL_ID = "WellnessAlertsChannel";
    private static final int ALERT_NOTIFICATION_ID = 1002;
    private long continuousStressStartTime = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final IBinder binder = new LocalBinder();
    private final CopyOnWriteArrayList<BleDataCallback> callbacks = new CopyOnWriteArrayList<>();

    public interface BleDataCallback {
        void onStatusUpdated(String status);
        void onBiometricsUpdated(int bpm, long steps);
        void onMoodPredicted(String mood, int confidence);
    }

    public class LocalBinder extends Binder {
        public BluetoothForegroundService getService() {
            return BluetoothForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        biometricClassifier = new BiometricClassifier(this);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("Service started, waiting for connection..."));
        
        if (intent != null && intent.hasExtra("MAC_ADDRESS")) {
            targetAddress = intent.getStringExtra("MAC_ADDRESS");
            targetName = intent.getStringExtra("DEVICE_NAME");
            if (targetName == null) targetName = "Watch";
            connectToDevice(targetAddress);
        }
        
        return START_STICKY; 
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        if (biometricClassifier != null) {
            biometricClassifier.close();
        }
        closeWatchGatt();
        super.onDestroy();
    }

    public void addCallback(BleDataCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
            callback.onStatusUpdated("Connected to Service. " + (watchGatt != null ? "GATT Active." : "Waiting for BLE."));
            if (currentBpm > 0) callback.onBiometricsUpdated(currentBpm, currentSteps);
        }
    }

    public void removeCallback(BleDataCallback callback) {
        callbacks.remove(callback);
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) {
            broadcastStatus("Bluetooth not available or address is empty.");
            return;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            broadcastStatus("Device not found.");
            return;
        }

        closeWatchGatt();
        broadcastStatus("Connecting to " + targetName + " in background...");
        updateNotification("Connecting to " + targetName + "...");
        
        watchGatt = device.connectGatt(this, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    @SuppressLint("MissingPermission")
    public void closeWatchGatt() {
        if (watchGatt != null) {
            watchGatt.disconnect();
            watchGatt.close();
            watchGatt = null;
        }
    }

    public void disconnectAndStop() {
        closeWatchGatt();
        stopForeground(true);
        stopSelf();
    }

    private void broadcastStatus(String status) {
        mainHandler.post(() -> {
            for (BleDataCallback callback : callbacks) {
                callback.onStatusUpdated(status);
            }
        });
    }

    private void broadcastBiometrics() {
        mainHandler.post(() -> {
            for (BleDataCallback callback : callbacks) {
                callback.onBiometricsUpdated(currentBpm, currentSteps);
            }
        });
        
        if (currentBpm > 0 && biometricClassifier != null) {
            BiometricClassifier.MoodPrediction prediction = biometricClassifier.predictMood(currentBpm, currentSteps);
            mainHandler.post(() -> {
                for (BleDataCallback callback : callbacks) {
                    callback.onMoodPredicted(prediction.mood, prediction.confidencePercent);
                }
            });
            updateNotification(currentBpm + " BPM | Mood: " + prediction.mood);
            evaluateStressAlert(prediction.mood, currentBpm);
        } else {
            updateNotification(currentBpm > 0 ? currentBpm + " BPM" : "Connected. Waiting for HR...");
        }
    }

    private void evaluateStressAlert(String mood, int bpm) {
        android.content.SharedPreferences prefs = getSharedPreferences("MusicZPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("prefStressAlerts", true)) return;

        long now = System.currentTimeMillis();
        if ("Stressed".equals(mood)) {
            if (continuousStressStartTime == 0) {
                continuousStressStartTime = now;
            } else if (now - continuousStressStartTime >= 5 * 60 * 1000) { // 5 minutes
                long lastAlertTime = prefs.getLong("lastStressAlertTime", 0);
                if (now - lastAlertTime >= 60 * 60 * 1000) { // 1 hour cooldown
                    prefs.edit().putLong("lastStressAlertTime", now).apply();
                    fireStressNotification(bpm);
                }
            }
        } else {
            continuousStressStartTime = 0;
        }
    }
    
    private void fireStressNotification(int bpm) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            // Calculate baseline HR
            java.util.List<com.example.genzmusicapp.db.WellnessHistory> history =
                    com.example.genzmusicapp.db.AppDatabase.getDatabase(this).wellnessDao().getRecentHistory(500);
            long avgBpm = 75; // fallback
            if (history != null && !history.isEmpty()) {
                long sum = 0;
                for (com.example.genzmusicapp.db.WellnessHistory h : history) {
                    sum += h.bpm;
                }
                avgBpm = sum / history.size();
            }
            
            String explainableText = "Heart rate (" + bpm + " BPM) is elevated above your personal baseline (" + avgBpm + " BPM) while activity remains low. Tap here to sync calming music.";
            
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("OPEN_PLAYER", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                    .setContentTitle("High Stress Detected")
                    .setContentText(explainableText)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(explainableText))
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
                    
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(ALERT_CHANNEL_ID, "Wellness Alerts", NotificationManager.IMPORTANCE_HIGH);
                    manager.createNotificationChannel(channel);
                }
                manager.notify(ALERT_NOTIFICATION_ID, builder.build());
            }
        });
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                broadcastStatus("BLE disconnected (status " + status + "). Attempting auto-reconnect...");
                updateNotification("Disconnected. Auto-reconnecting...");
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                broadcastStatus("Connected to " + targetName + ". Discovering services...");
                updateNotification("Connected to " + targetName + ". Securing channels...");
                boolean mtuStarted = gatt.requestMtu(247);
                if (!mtuStarted) {
                    gatt.discoverServices();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                broadcastStatus(targetName + " disconnected. Auto-reconnecting...");
                updateNotification("Disconnected. Auto-reconnecting...");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            gatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                broadcastStatus("Service discovery failed: " + status);
                return;
            }

            BluetoothGattService customService = gatt.getService(BLE_CUSTOM_LIVE_SERVICE_UUID);
            if (customService != null) {
                BluetoothGattCharacteristic notifyChar = customService.getCharacteristic(BLE_CUSTOM_NOTIFY_CHARACTERISTIC_UUID);
                if (notifyChar != null) {
                    enableNotifications(gatt, notifyChar);
                    return;
                }
            }

            BluetoothGattService hrService = gatt.getService(BLE_STANDARD_HEART_RATE_SERVICE_UUID);
            if (hrService != null) {
                BluetoothGattCharacteristic hrChar = hrService.getCharacteristic(BLE_STANDARD_HEART_RATE_MEASUREMENT_UUID);
                if (hrChar != null) {
                    enableNotifications(gatt, hrChar);
                    return;
                }
            }

            BluetoothGattCharacteristic fallback = findFirstNotifyCharacteristic(gatt);
            if (fallback != null) {
                enableNotifications(gatt, fallback);
                return;
            }

            broadcastStatus("No live biometric channels found on " + targetName + ".");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastStatus("Live data stream established! Measuring metrics...");
            } else {
                broadcastStatus("Failed to establish stream on " + targetName + " (status " + status + ")");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            handleLiveCharacteristic(characteristic.getUuid(), value);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleLiveCharacteristic(characteristic.getUuid(), characteristic.getValue());
        }
    };

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        int properties = characteristic.getProperties();
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 &&
            (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
            return;
        }

        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLE_CCCD_UUID);
        if (descriptor != null) {
            descriptor.setValue((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0
                    ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    : BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    private BluetoothGattCharacteristic findFirstNotifyCharacteristic(BluetoothGatt gatt) {
        List<BluetoothGattService> services = gatt.getServices();
        if (services == null) return null;
        for (BluetoothGattService service : services) {
            for (BluetoothGattCharacteristic c : service.getCharacteristics()) {
                int prop = c.getProperties();
                if (((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0 || 
                     (prop & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) &&
                    c.getDescriptor(BLE_CCCD_UUID) != null) {
                    return c;
                }
            }
        }
        return null;
    }

    private void handleLiveCharacteristic(UUID characteristicUuid, byte[] value) {
        if (value == null || value.length < 2) return;

        if (BLE_STANDARD_HEART_RATE_MEASUREMENT_UUID.equals(characteristicUuid)) {
            int flags = value[0] & 0xFF;
            int bpm = (flags & 0x01) == 0 ? value[1] & 0xFF : ((value[1] & 0xFF) | ((value[2] & 0xFF) << 8));
            if (bpm >= 30 && bpm <= 220) {
                currentBpm = bpm;
                broadcastBiometrics();
            }
        } else if (BLE_CUSTOM_NOTIFY_CHARACTERISTIC_UUID.equals(characteristicUuid) || (value[0] & 0xFF) == 0xBC) {
            if (value.length < 5) return;
            int packetType = value[1] & 0xFF;

            if (packetType == 0x5C && value.length >= 5) {
                int bpm = value[4] & 0xFF;
                if (bpm >= 30 && bpm <= 220) {
                    currentBpm = bpm;
                    broadcastBiometrics();
                }
            } else if (packetType == 0x51 && value.length >= 8) {
                long steps = ((long) value[4] & 0xFF) | (((long) value[5] & 0xFF) << 8) | (((long) value[6] & 0xFF) << 16) | (((long) value[7] & 0xFF) << 24);
                if (steps >= 0 && steps <= 300000 && (currentSteps == 0 || steps >= currentSteps || steps < 1000)) {
                    currentSteps = steps;
                    broadcastBiometrics();
                }
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "MusicZ Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(String text) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }

    private Notification buildNotification(String contentText) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("MusicZ Sync Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }
}
