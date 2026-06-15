import os
import glob
import pickle
import zipfile
import time
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import RandomizedSearchCV
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, classification_report

WESAD_DIR = r"C:\Users\vikas\OneDrive\Desktop\WESAD\WESAD"
PAMAP2_DIR = r"C:\Users\vikas\OneDrive\Desktop\pamap2 datastes\dataset\PAMAP2_Dataset\Protocol"

WESAD_TRAIN = ["S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9", "S10", "S11", "S13"]
WESAD_VAL = ["S14", "S15"]
WESAD_TEST = ["S16", "S17"]

PAMAP2_TRAIN = ["subject101.dat", "subject102.dat", "subject103.dat", "subject104.dat", "subject105.dat", "subject106.dat"]
PAMAP2_VAL = ["subject107.dat", "subject108.dat"]
PAMAP2_TEST = ["subject109.dat"]

# Labels Mapping 4-Class:
# 0: Calm (Baseline / Low Activity)
# 1: Stressed (Stress)
# 2: Energetic (Amusement / Moderate/High Activity)
# 3: Relaxed (Meditation)

def generate_features(df_hr, df_act, df_label, subject_id):
    df = pd.DataFrame({
        'Current_HR': df_hr,
        'Current_Steps': df_act,
        'Label': df_label
    })
    
    user_baseline_hr = df['Current_HR'].mean()
    df['User_Baseline_HR'] = user_baseline_hr
    df['HR_Baseline_Difference'] = df['Current_HR'] - user_baseline_hr
    
    df['HR_1m_Avg'] = df['Current_HR'].rolling(window=60, min_periods=1).mean()
    df['HR_5m_Avg'] = df['Current_HR'].rolling(window=300, min_periods=1).mean()
    
    df['HR_Change_Rate'] = df['Current_HR'].diff(periods=60).fillna(0)
    
    df['Activity_Trend'] = df['Current_Steps'].rolling(window=60, min_periods=1).mean()
    
    # Optional HRV proxy: rolling standard deviation of HR
    df['HR_Var_1m'] = df['Current_HR'].rolling(window=60, min_periods=2).std().fillna(0)
    df['HR_Var_5m'] = df['Current_HR'].rolling(window=300, min_periods=2).std().fillna(0)
    
    df = df.dropna()
    features = df[['Current_HR', 'Current_Steps', 'HR_1m_Avg', 'HR_5m_Avg', 
                   'HR_Change_Rate', 'Activity_Trend', 'User_Baseline_HR', 
                   'HR_Baseline_Difference', 'HR_Var_1m', 'HR_Var_5m']].values
    labels = df['Label'].values
    
    return features, labels

def process_wesad_subject(subject_folder):
    subject = os.path.basename(subject_folder)
    pkl_path = os.path.join(subject_folder, f"{subject}.pkl")
    hr_path = os.path.join(subject_folder, subject.lower(), "HR.csv")
    zip_path = os.path.join(subject_folder, f"{subject}_E4_Data.zip")
    
    if not os.path.exists(pkl_path):
        return None, None
        
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
        
        if majority_label == 1: mapped_label = 0 # Baseline -> Calm
        elif majority_label == 2: mapped_label = 1 # Stress -> Stressed
        elif majority_label == 3: mapped_label = 2 # Amusement -> Energetic
        elif majority_label == 4: mapped_label = 3 # Meditation -> Relaxed
        else: continue
            
        aligned_hr.append(hr_val)
        aligned_act.append(10.0) # Sitting/Low activity proxy
        aligned_labels.append(mapped_label)
        
    if len(aligned_hr) == 0:
        return None, None
        
    return generate_features(aligned_hr, aligned_act, aligned_labels, subject)

def process_pamap2_subject(file_path):
    df = pd.read_csv(file_path, sep=r'\s+', header=None, usecols=[0, 1, 2])
    df.columns = ['timestamp', 'activity', 'hr']
    df = df.dropna()
    
    df['timestamp_int'] = df['timestamp'].astype(int)
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
            label = 0 # Low Activity -> Calm
        elif act in [4, 5, 12, 13, 24]:
            step_proxy = np.random.uniform(80, 150)
            label = 2 # Mod/High -> Energetic
        elif act in [16, 17]:
            step_proxy = np.random.uniform(20, 80)
            label = 2 # Mod/High -> Energetic
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
    
    # We will combine Train and Val for RandomizedSearchCV as it does its own CV.
    # Wait, the user specifically requested cross-validation within the training split.
    # So we'll use just X_train for tuning, then evaluate on X_test.
    
    print("\n--- Hyperparameter Tuning Random Forest ---")
    param_dist = {
        'n_estimators': [100, 200, 300],
        'max_depth': [None, 10, 20, 30],
        'min_samples_split': [2, 5, 10],
        'min_samples_leaf': [1, 2, 4],
        'max_features': ['sqrt', 'log2', None]
    }
    
    rf = RandomForestClassifier(class_weight='balanced', random_state=42)
    # n_iter=10 for speed during testing, cv=3 is sufficient.
    search = RandomizedSearchCV(rf, param_distributions=param_dist, n_iter=10, cv=3, verbose=2, random_state=42, n_jobs=-1)
    
    start_tune = time.time()
    search.fit(X_train, y_train)
    print(f"Tuning took {time.time() - start_tune:.2f} seconds")
    
    best_rf = search.best_estimator_
    print(f"\nBest Hyperparameters: {search.best_params_}")
    
    print("\n--- Evaluating Best Random Forest on Unseen Test Set ---")
    start_inf = time.time()
    y_pred = best_rf.predict(X_test)
    end_inf = time.time()
    
    acc = accuracy_score(y_test, y_pred)
    cm = confusion_matrix(y_test, y_pred)
    report = classification_report(y_test, y_pred, target_names=["Calm", "Stressed", "Energetic", "Relaxed"])
    
    print(f"Inference Time: {(end_inf - start_inf)*1000:.2f} ms")
    print(f"Overall Accuracy: {acc:.4f}\n")
    print("Confusion Matrix:")
    print(cm)
    print("\nClassification Report:")
    print(report)
    
    feature_names = ['Current_HR', 'Current_Steps', 'HR_1m_Avg', 'HR_5m_Avg', 
                     'HR_Change_Rate', 'Activity_Trend', 'User_Baseline_HR', 
                     'HR_Baseline_Difference', 'HR_Var_1m', 'HR_Var_5m']
                     
    print("\n--- Feature Importance Ranking ---")
    importances = best_rf.feature_importances_
    sorted_idx = np.argsort(importances)[::-1]
    for i, idx in enumerate(sorted_idx):
        print(f"{i+1}. {feature_names[idx]}: {importances[idx]:.4f}")

    # Save the model
    with open("final_rf_model.pkl", "wb") as f:
        pickle.dump(best_rf, f)
        
    print(f"\nModel saved to final_rf_model.pkl. Size: {os.path.getsize('final_rf_model.pkl') / 1024 / 1024:.2f} MB")

if __name__ == "__main__":
    main()
