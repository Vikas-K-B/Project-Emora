import re

file_path = "app/src/main/java/com/example/genzmusicapp/MainActivity.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add imports
imports = """import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.core.content.ContextCompat;"""
content = re.sub(r'import android.content.Context;', f'import android.content.Context;\n{imports}', content)

# Remove old variables
content = re.sub(r'private BiometricClassifier biometricClassifier;\s*', '', content)
content = re.sub(r'private BluetoothGatt watchGatt;\s*', '', content)

# Add new variables
new_vars = """private BluetoothForegroundService bluetoothService;
    private boolean isServiceBound = false;
    private String currentMoodLabel = "Unknown";
    
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
            handler.post(() -> applyHealthSnapshot(currentContent));
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
    };"""
content = re.sub(r'private BluetoothLeScanner bluetoothLeScanner;', f'private BluetoothLeScanner bluetoothLeScanner;\n    {new_vars}', content)


# Remove biometricClassifier instantiation in onCreate
content = re.sub(r'biometricClassifier = new BiometricClassifier\(this\);\s*', '', content)

# Replace onDestroy content
old_destroy = r'if \(biometricClassifier != null\) \{\s*biometricClassifier\.close\(\);\s*\}\s*if \(watchGatt != null\) \{[^\}]+\}\s*watchGatt = null;\s*\}\s*'
new_destroy = """if (isServiceBound) {
            if (bluetoothService != null) bluetoothService.removeCallback(bleDataCallback);
            unbindService(serviceConnection);
            isServiceBound = false;
        }\n        """
content = re.sub(old_destroy, new_destroy, content, flags=re.DOTALL)


# Replace connectSelectedBleDevice
old_connect = r'private void connectSelectedBleDevice\(BleDeviceCandidate candidate\) \{[\s\S]*?connectGattDevice\(candidate\.device\);\s*\}'
new_connect = """private void connectSelectedBleDevice(BleDeviceCandidate candidate) {
        selectedBleDeviceLabel = candidate.name;
        stopBleScan();
        updateBleStatus("Connecting to " + selectedBleDeviceLabel + " via Service...");
        
        Intent serviceIntent = new Intent(this, BluetoothForegroundService.class);
        serviceIntent.putExtra("MAC_ADDRESS", candidate.device.getAddress());
        serviceIntent.putExtra("DEVICE_NAME", candidate.name);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }"""
content = re.sub(old_connect, new_connect, content)


# Remove calculateLiveMood method block and replace with simpler one
old_calc_mood = r'private String calculateLiveMood\(long bpm, long steps\) \{[\s\S]*?return "Calculating live mood\.\.\.";\s*\}'
new_calc_mood = """private String calculateLiveMood(long bpm, long steps) {
        if (bpm <= 0) {
            if (steps > 0) return "Activity logged: " + formatNumber(steps) + " steps. Awaiting live heart rate data...";
            return "Calibrating neural biosensors. Waiting for stable biometric readings...";
        }
        return "Mood: " + currentMoodLabel;
    }"""
content = re.sub(old_calc_mood, new_calc_mood, content)

# Remove inferStressLabel block completely or simplify
old_infer = r'private String inferStressLabel\(long averageBpm, long steps\) \{[\s\S]*?return "Unknown";\s*\}'
new_infer = """private String inferStressLabel(long averageBpm, long steps) {
        return currentMoodLabel;
    }"""
content = re.sub(old_infer, new_infer, content)

# We need to wipe out the giant blocks of watchGattCallback and methods
# Since regex might be tricky, let's just find their declarations and delete until the end of the method.
# It's safer to just let the script run the sub and if anything is left over, we'll fix it manually.
content = re.sub(r'private final BluetoothGattCallback watchGattCallback = new BluetoothGattCallback\(\) \{[\s\S]*?\};\n\n', '', content)
content = re.sub(r'@SuppressLint\("MissingPermission"\)\s*private void connectGattDevice[\s\S]*?watchGatt = device.connectGatt[^\}]+\}\s*', '', content)
content = re.sub(r'@SuppressLint\("MissingPermission"\)\s*private void enableLiveNotifications[\s\S]*?writeDescriptor[^\}]+\}\s*', '', content)
content = re.sub(r'@SuppressLint\("MissingPermission"\)\s*private void closeWatchGatt\(\) \{[\s\S]*?watchGatt = null;\s*\}\s*', '', content)
content = re.sub(r'private void handleLiveCharacteristic[\s\S]*?decodeCustomLiveWatchPacket[^\}]+\}\s*', '', content)
content = re.sub(r'private BluetoothGattCharacteristic findFirstNotifyCharacteristic[\s\S]*?return null;\s*\}\s*', '', content)
content = re.sub(r'private boolean looksLikeCustomLivePacket[\s\S]*?0xBC;\s*\}\s*', '', content)
content = re.sub(r'private String shortUuid[\s\S]*?return uuidText.substring\(0, 8\);\s*\}\s*', '', content)
content = re.sub(r'private void decodeStandardHeartRatePacket[\s\S]*?updateLiveBpm\(bpm\);\s*\}', '', content)
content = re.sub(r'private void decodeCustomLiveWatchPacket[\s\S]*?Ignored unrecognized packet type[^\}]+\}\s*\}\s*', '', content)
content = re.sub(r'private void updateLiveBpm\(int bpm\) \{[\s\S]*?applyHealthSnapshot\(currentContent\)\);\s*\}\s*', '', content)
content = re.sub(r'private void updateLiveSteps\(long steps, int packetType\) \{[\s\S]*?applyHealthSnapshot\(currentContent\)\);\s*\}\s*', '', content)
content = re.sub(r'private Long extractCustomStepCount\(byte\[\] value\) \{[\s\S]*?return null;\s*\}', '', content)
content = re.sub(r'private boolean isPlausibleStepCount\(long candidate, long currentSteps\) \{[\s\S]*?candidate < 1000L;\s*\}', '', content)
content = re.sub(r'private int unsignedLittleEndianShort\(byte\[\] value, int offset\) \{[\s\S]*?0xFF\) << 8\);\s*\}', '', content)
content = re.sub(r'private long unsignedLittleEndianInt\(byte\[\] value, int offset\) \{[\s\S]*?0xFF\) << 24\);\s*\}', '', content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done refactoring MainActivity.java")
