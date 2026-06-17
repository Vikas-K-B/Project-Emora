import sys

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
skip_mode = False
skip_level = 0
current_level = 0

imports_added = False

for i, line in enumerate(lines):
    # track bracket levels
    open_c = line.count('{')
    close_c = line.count('}')
    
    # Calculate level before this line
    level_before = current_level
    current_level += (open_c - close_c)
    
    if not imports_added and "import android.content.Context;" in line:
        new_lines.append(line)
        new_lines.append("import android.content.ComponentName;\n")
        new_lines.append("import android.content.ServiceConnection;\n")
        new_lines.append("import android.os.IBinder;\n")
        new_lines.append("import androidx.core.content.ContextCompat;\n")
        imports_added = True
        continue
    
    # Check for variables we want to replace
    if "private BiometricClassifier biometricClassifier;" in line:
        new_lines.append("    private BluetoothForegroundService bluetoothService;\n")
        new_lines.append("    private boolean isServiceBound = false;\n")
        new_lines.append("    private String currentMoodLabel = \"Unknown\";\n")
        continue
        
    if "private BluetoothGatt watchGatt;" in line:
        new_lines.append("""
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
    };
""")
        continue
        
    # Check for blocks to remove
    removal_triggers = [
        "private final BluetoothGattCallback watchGattCallback = new BluetoothGattCallback() {",
        "private void connectGattDevice(BluetoothDevice device) {",
        "private void enableLiveNotifications(",
        "private void closeWatchGatt() {",
        "private void handleLiveCharacteristic(",
        "private BluetoothGattCharacteristic findFirstNotifyCharacteristic(",
        "private boolean looksLikeCustomLivePacket(",
        "private String shortUuid(",
        "private void decodeStandardHeartRatePacket(",
        "private void decodeCustomLiveWatchPacket(",
        "private void updateLiveBpm(",
        "private void updateLiveSteps(",
        "private Long extractCustomStepCount(",
        "private boolean isPlausibleStepCount(",
        "private int unsignedLittleEndianShort(",
        "private long unsignedLittleEndianInt("
    ]
    
    if not skip_mode:
        for trigger in removal_triggers:
            if trigger in line:
                skip_mode = True
                skip_level = level_before
                break
    
    if skip_mode:
        if current_level == skip_level:
            skip_mode = False
        continue

    # Other surgical replaces
    if "biometricClassifier = new BiometricClassifier(this);" in line:
        continue
        
    if "if (biometricClassifier != null) {" in line and "onDestroy" in "".join(lines[max(0, i-15):i]):
        new_lines.append("""
        if (isServiceBound) {
            if (bluetoothService != null) bluetoothService.removeCallback(bleDataCallback);
            unbindService(serviceConnection);
            isServiceBound = false;
        }
""")
        skip_mode = True
        skip_level = level_before
        continue

    # Same for watchGatt block in onDestroy
    if "if (watchGatt != null) {" in line and "onDestroy" in "".join(lines[max(0, i-15):i]):
        skip_mode = True
        skip_level = level_before
        continue
        
    if "connectGattDevice(candidate.device);" in line:
        new_lines.append("""        Intent serviceIntent = new Intent(this, BluetoothForegroundService.class);
        serviceIntent.putExtra("MAC_ADDRESS", candidate.device.getAddress());
        serviceIntent.putExtra("DEVICE_NAME", candidate.name);
        ContextCompat.startForegroundService(this, serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
""")
        continue
        
    if "private String calculateLiveMood(long bpm, long steps) {" in line:
        new_lines.append("""    private String calculateLiveMood(long bpm, long steps) {
        if (bpm <= 0) {
            if (steps > 0) return "Activity logged: " + formatNumber(steps) + " steps. Awaiting live heart rate data...";
            return "Calibrating neural biosensors. Waiting for stable biometric readings...";
        }
        return "Mood: " + currentMoodLabel;
    }
""")
        skip_mode = True
        skip_level = level_before
        continue
        
    if "private String inferStressLabel(long averageBpm, long steps) {" in line:
        new_lines.append("""    private String inferStressLabel(long averageBpm, long steps) {
        return currentMoodLabel;
    }
""")
        skip_mode = True
        skip_level = level_before
        continue
        
    new_lines.append(line)

with open('app/src/main/java/com/example/genzmusicapp/MainActivity.java', 'w', encoding='utf-8') as f:
    f.writelines(new_lines)
print("Done")
