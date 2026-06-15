import os
import glob
import pickle
import zipfile
import time
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.ensemble import RandomForestClassifier
import xgboost as xgb
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, classification_report
from sklearn.utils.class_weight import compute_class_weight

WESAD_DIR = r"C:\Users\vikas\OneDrive\Desktop\WESAD\WESAD"
PAMAP2_DIR = r"C:\Users\vikas\OneDrive\Desktop\pamap2 datastes\dataset\PAMAP2_Dataset\Protocol"

WESAD_TRAIN = ["S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "S11", "S13"]
WESAD_VAL = ["S14", "S15"]
WESAD_TEST = ["S16", "S17"]

PAMAP2_TRAIN = ["subject101.dat", "subject102.dat", "subject103.dat", "subject104.dat", "subject105.dat", "subject106.dat"]
PAMAP2_VAL = ["subject107.dat", "subject108.dat"]
PAMAP2_TEST = ["subject109.dat"]

# Labels Mapping:
# 0: Calm (Baseline)
# 1: Stressed
# 2: Energetic (Amusement / High Activity)
# 3: Relaxed (Meditation)

def generate_features(df_hr, df_act, df_label, subject_id):
    # df_hr, df_act, df_label should be 1D arrays of 1Hz aligned data
    df = pd.DataFrame({
        'Current_HR': df_hr,
        'Current_Steps': df_act,
        'Label': df_label
    })
    
    # Baseline
    user_baseline_hr = df['Current_HR'].mean()
    df['User_Baseline_HR'] = user_baseline_hr
    df['HR_Baseline_Difference'] = df['Current_HR'] - user_baseline_hr
    
    # Rolling features
    df['HR_1m_Avg'] = df['Current_HR'].rolling(window=60, min_periods=1).mean()
    df['HR_5m_Avg'] = df['Current_HR'].rolling(window=300, min_periods=1).mean()
    
    # HR Change Rate (Current - 60s ago)
    df['HR_Change_Rate'] = df['Current_HR'].diff(periods=60).fillna(0)
    
    # Activity Trend
    df['Activity_Trend'] = df['Current_Steps'].rolling(window=60, min_periods=1).mean()
    
    df = df.dropna()
    features = df[['Current_HR', 'Current_Steps', 'HR_1m_Avg', 'HR_5m_Avg', 
                   'HR_Change_Rate', 'Activity_Trend', 'User_Baseline_HR', 'HR_Baseline_Difference']].values
    labels = df['Label'].values
    
    return features, labels

def process_wesad_subject(subject_folder):
    subject = os.path.basename(subject_folder)
    pkl_path = os.path.join(subject_folder, f"{subject}.pkl")
    hr_path = os.path.join(subject_folder, subject.lower(), "HR.csv")
    zip_path = os.path.join(subject_folder, f"{subject}_E4_Data.zip")
    
    if not os.path.exists(pkl_path):
        return None, None
        
    # Unzip E4 data if HR.csv is missing
    if not os.path.exists(hr_path) and os.path.exists(zip_path):
        try:
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                zip_ref.extractall(os.path.join(subject_folder, subject.lower()))
        except Exception:
            pass
    
    if not os.path.exists(hr_path):
        return None, None
        
    with open(pkl_path, 'rb') as f:
        data = pickle.load(f, encoding='latin1')
        
    labels_700hz = data['label']
    hr_df = pd.read_csv(hr_path, header=None)
    hr_data = hr_df.iloc[2:].values.flatten()
    
    num_seconds = len(labels_700hz) // 700
    min_len = min(len(hr_data), num_seconds)
    
    aligned_hr = []
    aligned_act = []
    aligned_labels = []
    
    for i in range(min_len):
        hr_val = hr_data[i]
        if np.isnan(hr_val):
            continue
            
        sec_labels = labels_700hz[i*700 : (i+1)*700]
        majority_label = np.bincount(sec_labels).argmax()
        
        if majority_label == 1: mapped_label = 0
        elif majority_label == 2: mapped_label = 1
        elif majority_label == 3: mapped_label = 2
        elif majority_label == 4: mapped_label = 0 # Merged Relaxed into Resting
        else: continue
            
        aligned_hr.append(hr_val)
        aligned_act.append(10.0) # Low activity proxy
        aligned_labels.append(mapped_label)
        
    if len(aligned_hr) == 0:
        return None, None
        
    return generate_features(aligned_hr, aligned_act, aligned_labels, subject)

def process_pamap2_subject(file_path):
    df = pd.read_csv(file_path, sep=r'\s+', header=None, usecols=[0, 1, 2])
    df.columns = ['timestamp', 'activity', 'hr']
    df = df.dropna()
    
    df['timestamp_int'] = df['timestamp'].astype(int)
    # Aggregate to 1Hz
    df_1hz = df.groupby('timestamp_int').mean().reset_index()
    
    aligned_hr = []
    aligned_act = []
    aligned_labels = []
    
    for _, row in df_1hz.iterrows():
        act = int(round(row['activity']))
        hr = float(row['hr'])
        
        if act == 0: continue
            
        if act in [1, 2, 3]:
            step_proxy = np.random.uniform(0, 20)
            label = 0
        elif act in [4, 5, 12, 13, 24]:
            step_proxy = np.random.uniform(80, 150)
            label = 2
        elif act in [16, 17]:
            step_proxy = np.random.uniform(20, 80)
            label = 2
        else:
            continue
            
        aligned_hr.append(hr)
        aligned_act.append(step_proxy)
        aligned_labels.append(label)
        
    if len(aligned_hr) == 0:
        return None, None
        
    subject = os.path.basename(file_path)
    return generate_features(aligned_hr, aligned_act, aligned_labels, subject)

def load_data_split(wesad_subjects, pamap2_subjects):
    X, y = [], []
    
    for subj in wesad_subjects:
        folder = os.path.join(WESAD_DIR, subj)
        fx, fy = process_wesad_subject(folder)
        if fx is not None:
            X.extend(fx)
            y.extend(fy)
            
    for subj_file in pamap2_subjects:
        file_path = os.path.join(PAMAP2_DIR, subj_file)
        fx, fy = process_pamap2_subject(file_path)
        if fx is not None:
            X.extend(fx)
            y.extend(fy)
            
    return np.array(X, dtype=np.float32), np.array(y, dtype=np.int32)

def build_nn(input_shape):
    inputs = keras.Input(shape=(input_shape,))
    # Scale inputs heuristically based on max expected values
    scales = tf.constant([200.0, 200.0, 200.0, 200.0, 100.0, 200.0, 200.0, 100.0])
    x_scaled = inputs / scales
    
    x = keras.layers.Dense(32, activation='relu')(x_scaled)
    x = keras.layers.Dense(16, activation='relu')(x)
    outputs = keras.layers.Dense(3, activation='softmax')(x)
    
    model = keras.Model(inputs=inputs, outputs=outputs)
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    return model

def print_evaluation(name, y_true, y_pred, start_time, end_time, size_bytes):
    acc = accuracy_score(y_true, y_pred)
    cm = confusion_matrix(y_true, y_pred)
    report = classification_report(y_true, y_pred, target_names=["Resting", "Stressed", "Energetic"])
    
    print(f"\n{'='*40}")
    print(f"Model: {name}")
    print(f"{'='*40}")
    print(f"Inference Time (Test Set): {(end_time - start_time)*1000:.2f} ms")
    print(f"Estimated Model Size: {size_bytes / 1024:.2f} KB")
    print(f"Overall Accuracy: {acc:.4f}\n")
    print("Confusion Matrix:")
    print(cm)
    print("\nClassification Report:")
    print(report)

def main():
    print("Loading Train Split...")
    X_train, y_train = load_data_split(WESAD_TRAIN, PAMAP2_TRAIN)
    
    print("Loading Validation Split...")
    X_val, y_val = load_data_split(WESAD_VAL, PAMAP2_VAL)
    
    print("Loading Test Split...")
    X_test, y_test = load_data_split(WESAD_TEST, PAMAP2_TEST)
    
    print(f"Train samples: {len(X_train)}")
    print(f"Val samples: {len(X_val)}")
    print(f"Test samples: {len(X_test)}")
    
    class_weights_arr = compute_class_weight('balanced', classes=np.unique(y_train), y=y_train)
    class_weights_dict = {cls: weight for cls, weight in zip(np.unique(y_train), class_weights_arr)}
    sample_weights = np.array([class_weights_dict[cls] for cls in y_train])
    
    # 1. Neural Network
    print("\nTraining Neural Network...")
    nn_model = build_nn(X_train.shape[1])
    nn_model.fit(X_train, y_train, epochs=10, batch_size=128, validation_data=(X_val, y_val), class_weight=class_weights_dict, verbose=0)
    
    start = time.time()
    nn_preds = np.argmax(nn_model.predict(X_test, verbose=0), axis=1)
    end = time.time()
    
    nn_model.save("temp_nn.h5")
    nn_size = os.path.getsize("temp_nn.h5")
    print_evaluation("Neural Network", y_test, nn_preds, start, end, nn_size)
    
    # 2. Random Forest
    print("\nTraining Random Forest...")
    rf_model = RandomForestClassifier(n_estimators=100, class_weight='balanced', random_state=42, n_jobs=-1)
    rf_model.fit(X_train, y_train)
    
    start = time.time()
    rf_preds = rf_model.predict(X_test)
    end = time.time()
    
    with open("temp_rf.pkl", "wb") as f: pickle.dump(rf_model, f)
    rf_size = os.path.getsize("temp_rf.pkl")
    print_evaluation("Random Forest", y_test, rf_preds, start, end, rf_size)
    
    # 3. XGBoost
    print("\nTraining XGBoost...")
    xgb_model = xgb.XGBClassifier(n_estimators=100, max_depth=6, learning_rate=0.1, random_state=42)
    xgb_model.fit(X_train, y_train, sample_weight=sample_weights)
    
    start = time.time()
    xgb_preds = xgb_model.predict(X_test)
    end = time.time()
    
    xgb_model.save_model("temp_xgb.json")
    xgb_size = os.path.getsize("temp_xgb.json")
    print_evaluation("XGBoost", y_test, xgb_preds, start, end, xgb_size)
    
    feature_names = ['Current_HR', 'Current_Steps', 'HR_1m_Avg', 'HR_5m_Avg', 
                     'HR_Change_Rate', 'Activity_Trend', 'User_Baseline_HR', 'HR_Baseline_Difference']
    
    print("\n--- Feature Importance (Random Forest) ---")
    for name, imp in sorted(zip(feature_names, rf_model.feature_importances_), key=lambda x: x[1], reverse=True):
        print(f"{name}: {imp:.4f}")

    print("\n--- Feature Importance (XGBoost) ---")
    for name, imp in sorted(zip(feature_names, xgb_model.feature_importances_), key=lambda x: x[1], reverse=True):
        print(f"{name}: {imp:.4f}")

if __name__ == "__main__":
    main()
