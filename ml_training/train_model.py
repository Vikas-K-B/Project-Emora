import os
import glob
import pickle
import zipfile
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, classification_report
from sklearn.utils.class_weight import compute_class_weight

WESAD_DIR = r"C:\Users\vikas\OneDrive\Desktop\WESAD\WESAD"
PAMAP2_DIR = r"C:\Users\vikas\OneDrive\Desktop\pamap2 datastes\dataset\PAMAP2_Dataset\Protocol"

# Labels Mapping:
# 0: Calm (Baseline)
# 1: Stressed
# 2: Energetic (Amusement / High Activity)
# 3: Relaxed (Meditation)

def load_wesad_data():
    X = []
    y = []
    
    wesad_folders = glob.glob(os.path.join(WESAD_DIR, "S*"))
    for folder in wesad_folders:
        subject = os.path.basename(folder)
        pkl_path = os.path.join(folder, f"{subject}.pkl")
        hr_path = os.path.join(folder, subject.lower(), "HR.csv")
        zip_path = os.path.join(folder, f"{subject}_E4_Data.zip")
        
        if not os.path.exists(pkl_path):
            continue
            
        print(f"Processing WESAD: {subject}")
        
        # Unzip E4 data if HR.csv is missing
        if not os.path.exists(hr_path) and os.path.exists(zip_path):
            print(f"Extracting {zip_path}...")
            try:
                with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                    zip_ref.extractall(os.path.join(folder, subject.lower()))
            except Exception as e:
                print(f"Error extracting zip: {e}")
        
        if not os.path.exists(hr_path):
            print(f"HR.csv still not found for {subject}, skipping...")
            continue
            
        with open(pkl_path, 'rb') as f:
            data = pickle.load(f, encoding='latin1')
            
        labels = data['label'] # 700 Hz
        
        hr_df = pd.read_csv(hr_path, header=None)
        # The first row is start time, second row is frequency (1Hz)
        hr_data = hr_df.iloc[2:].values.flatten()
        
        # Since HR is 1Hz and labels are 700Hz, we can downsample labels
        num_seconds = len(labels) // 700
        min_len = min(len(hr_data), num_seconds)
        
        for i in range(min_len):
            hr_val = hr_data[i]
            if np.isnan(hr_val):
                continue
            
            # Get the majority label for this second
            sec_labels = labels[i*700 : (i+1)*700]
            majority_label = np.bincount(sec_labels).argmax()
            
            # Map label
            if majority_label == 1:
                mapped_label = 0 # Calm
            elif majority_label == 2:
                mapped_label = 1 # Stressed
            elif majority_label == 3:
                mapped_label = 2 # Energetic
            elif majority_label == 4:
                mapped_label = 3 # Relaxed
            else:
                continue # Skip transient or undefined labels
                
            activity_level = 10.0 # Low activity proxy for WESAD
            X.append([hr_val, activity_level])
            y.append(mapped_label)

    return X, y

def load_pamap2_data():
    X = []
    y = []
    
    pamap2_files = glob.glob(os.path.join(PAMAP2_DIR, "subject*.dat"))
    for file in pamap2_files:
        print(f"Processing PAMAP2: {os.path.basename(file)}")
        # Col 0: timestamp, Col 1: activityID, Col 2: HR
        df = pd.read_csv(file, sep=r'\s+', header=None, usecols=[1, 2])
        df.columns = ['activity', 'hr']
        df = df.dropna()
        
        for _, row in df.iterrows():
            act = int(row['activity'])
            hr = float(row['hr'])
            
            if act == 0:
                continue # undefined activity
                
            # Map activity to levels and labels
            # Low: 1(lying), 2(sitting), 3(standing) -> Calm
            if act in [1, 2, 3]:
                step_proxy = np.random.uniform(0, 20)
                label = 0 # Calm
            # Moderate/High: 4(walking), 5(running), 12(ascending stairs), etc -> Energetic
            elif act in [4, 5, 12, 13, 24]:
                step_proxy = np.random.uniform(80, 150)
                label = 2 # Energetic
            elif act in [16, 17]: # Vacuuming, ironing (Moderate)
                step_proxy = np.random.uniform(20, 80)
                label = 2 # Energetic
            else:
                continue
                
            X.append([hr, step_proxy])
            y.append(label)
            
    return X, y

def build_and_train():
    print("Loading WESAD dataset...")
    X_wesad, y_wesad = load_wesad_data()
    
    print("Loading PAMAP2 dataset...")
    X_pamap2, y_pamap2 = load_pamap2_data()
    
    X = np.array(X_wesad + X_pamap2, dtype=np.float32)
    y = np.array(y_wesad + y_pamap2, dtype=np.int32)
    
    print(f"Total Combined Samples: {len(X)}")
    
    unique_classes, class_counts = np.unique(y, return_counts=True)
    print("\n--- Class Distribution ---")
    for cls, count in zip(unique_classes, class_counts):
        pct = (count / len(y)) * 100
        print(f"Class {cls}: {count} samples ({pct:.2f}%)")
        
    class_weights_arr = compute_class_weight('balanced', classes=np.unique(y), y=y)
    class_weights_dict = {cls: weight for cls, weight in zip(np.unique(y), class_weights_arr)}
    print(f"\nComputed Class Weights: {class_weights_dict}")
    
    # Train / Val / Test Split (70, 15, 15)
    X_train_val, X_test, y_train_val, y_test = train_test_split(X, y, test_size=0.15, random_state=42, stratify=y)
    X_train, X_val, y_train, y_val = train_test_split(X_train_val, y_train_val, test_size=0.1765, random_state=42, stratify=y_train_val)
    
    print(f"\nTraining Samples: {len(X_train)}")
    print(f"Validation Samples: {len(X_val)}")
    print(f"Test Samples: {len(X_test)}\n")
    
    # Model Architecture
    inputs = keras.Input(shape=(2,))
    x_scaled = inputs / tf.constant([200.0, 200.0])
    
    x = keras.layers.Dense(32, activation='relu')(x_scaled)
    x = keras.layers.Dense(16, activation='relu')(x)
    outputs = keras.layers.Dense(4, activation='softmax')(x)
    
    model = keras.Model(inputs=inputs, outputs=outputs)
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    
    # Train
    print("Training Model with Class Weights...")
    model.fit(X_train, y_train, epochs=15, batch_size=128, validation_data=(X_val, y_val), class_weight=class_weights_dict)
    
    # Evaluate
    print("\n--- Evaluation on Test Set ---")
    y_pred_probs = model.predict(X_test)
    y_pred = np.argmax(y_pred_probs, axis=1)
    
    acc = accuracy_score(y_test, y_pred)
    cm = confusion_matrix(y_test, y_pred)
    report = classification_report(y_test, y_pred, target_names=["Calm", "Stressed", "Energetic", "Relaxed"])
    
    print(f"Overall Accuracy: {acc:.4f}\n")
    print("Confusion Matrix:")
    print(cm)
    print("\nClassification Report (Per-class Precision, Recall, F1):")
    print(report)
    print("------------------------------\n")
    
    # Export to TFLite
    print("Exporting to TensorFlow Lite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    
    assets_dir = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    output_path = os.path.join(assets_dir, "stress_classifier.tflite")
    
    with open(output_path, "wb") as f:
        f.write(tflite_model)
        
    print(f"Saved balanced real-data model to: {output_path}")

if __name__ == "__main__":
    build_and_train()
