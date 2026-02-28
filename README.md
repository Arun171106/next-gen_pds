# NextGen PDS Kiosk

This is the Android repository for the NextGen PDS Kiosk application. It features an offline multilingual voice assistant, face verification, and kiosk mode capabilities.

## Requirements

To run this application on a new device or development machine, you will need:
- **Android Studio** (Koala or newer recommended)
- **Java Development Kit (JDK)** 17 or higher
- **Android SDK** (Configured automatically via Android Studio)
- **Git**

## Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Arun171106/next-gen_pds.git
   ```

2. **Open the project:**
   - Launch Android Studio.
   - Select **Open** and navigate to the cloned directory (e.g., `NextGen-PDS_Kiosk`).
   - Wait for the Gradle sync to complete.

3. **Build and Run:**
   - Connect an Android device via USB (ensure Developer Options and USB Debugging are enabled) or start an Android Emulator.
   - Click the **Run** button (green play icon) in Android Studio, or run the following command in the terminal:
     ```bash
     ./gradlew assembleDebug
     ```

## Project Features

- **Voice Assistant:** Uses Vosk for offline, multilingual speech recognition (English, Tamil) and Android's built-in TTS.
- **Face Verification:** Integrates camera capabilities and ML to verify user identity.
- **Kiosk Mode:** Configured for Lock Task mode and Device Owner mode to ensure the app runs persistently on public kiosk hardware without interruption.

## Additional Setup Notes for Kiosk Mode
- To deploy as a true Kiosk, the device needs to be provisioned using ADB to set the app as the Device Owner:
  ```bash
  adb shell dpm set-device-owner com.example.nextgen_pds_kiosk/.receiver.DeviceAdminReceiver
  ```
- Ensure that the necessary offline ML models (Vosk) are downloaded or properly packaged depending on the implementation.
