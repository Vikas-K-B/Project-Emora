<div align="center">
  <h1>Emora</h1>
  <p>An AI-powered Music Recommendation and Wellness Application</p>
</div>

## 🌐 Demo
Check out the live website and download the latest APK: **[Emora Live Demo](https://emora-seven.vercel.app/)**

## 👤 Author
Developed by **[Vikas K B](https://github.com/Vikas-K-B)**

---

## 📱 About The Project

Emora is an innovative Android application that harmonizes your emotional state with your music library. By monitoring real-time biometric data via Bluetooth Low Energy (BLE) wearables (like heart rate and steps), Emora employs on-device Machine Learning (TensorFlow Lite) to calculate your current mood and stress levels. It then interfaces with Gemini AI to generate highly personalized music recommendations and wellness insights that help you stay balanced throughout your day.

### ✨ Key Features

- **Real-time Mood Sync**: Connects to smartwatches via BLE to read live heart rate (BPM) and step count data.
- **On-Device ML**: Utilizes TensorFlow Lite models to classify emotional states and stress levels instantly.
- **Explainable AI Insights**: Leverages Gemini AI to provide context-aware feedback on your daily wellness trends.
- **Smart Music Recommendations**: Recommends songs specifically tailored to match or elevate your current mood.
- **Beautiful OLED Design**: A premium, minimalist dark mode UI utilizing pure black (#000000) for seamless aesthetic appeal and battery saving.
- **Secure Authentication**: Integrated with Firebase Authentication (Google Sign-in and Email).
- **Offline Wellness History**: Securely logs biometric data locally using Room Database for long-term emotional tracking.

## 🛠️ Tech Stack

- **Frontend**: Java, XML, Android SDK.
- **Backend/Cloud**: Firebase Authentication.
- **AI & ML**: TensorFlow Lite (Random Forest models), Google Gemini API.
- **Database**: Room Database (SQLite).
- **Hardware Integration**: Android BLE (Bluetooth Low Energy) APIs.

## 🚀 Getting Started

1. Clone the repository.
2. Open the project in **Android Studio**.
3. Let Gradle sync and download the necessary dependencies.
4. Go to `Build > Make Project`.
5. Run the app on an emulator or physical device.

_Note: For the BLE heartbeat monitoring to function, run the app on a physical Android device and pair it with a compatible BLE heart-rate monitor/smartwatch._
