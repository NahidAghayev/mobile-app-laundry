# Laundry Ledger

> **Language notice:** The application itself (all user-facing text) is in Azerbaijani. This documentation is written in English.

A personal Android application for recording restaurant and company customers, laundry weight, curtain meters and rate for each job, debt and payment transactions, separate expenses, calendar totals, and calculator results. The application works offline and stores data on the device.

## Build and install

1. Install Android Studio and the recommended Android SDK components: Android SDK Platform 36 and Build Tools. The project uses the JDK installed by default in the Android Studio folder; if not present, set the `JAVA_HOME` variable to JDK 17 and the `ANDROID_HOME` variable to the Android SDK folder.
2. Open PowerShell in the project folder and run `.\gradlew.bat assembleDebug`. Gradle 9.5.0 is downloaded first, then Android build dependencies. The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.
3. **To open with an emulator on a laptop:** In Android Studio, go to **Tools → SDK Manager → SDK Tools** and install **Android Emulator** and **Android SDK Platform-Tools**. Then select **Tools → Device Manager → Create Virtual Device**, create a Pixel phone profile, download the system image, and start the emulator. Drag the generated APK file into the emulator window; after installation, open **Laundry Ledger** from the apps list.
4. You can also install by transferring the APK to an Android phone and opening the file. If Android asks for permission, allow installation from that file source.

When running on an emulator or a phone connected via USB, `.\gradlew.bat installDebug` in the project folder rebuilds the APK and installs it on the device. For phone, **Developer options** and **USB debugging** must be enabled, and the connection must be confirmed on the phone. For USB installation details, see [Android device documentation](https://developer.android.com/studio/run/device).

## Updates and data preservation

The updated APK must be installed with the same app ID and signing key. Android Studio's debug APK uses the `%USERPROFILE%\.android\debug.keystore` file. If you want to update the app or move it to another computer, back up this file in a trusted location. If the key is lost, you may need to delete the old app to install an APK signed with a different key, in which case local data may be lost. First, back up from the app. For long-term use, create and protect a separate release key in Android Studio; do not add the key file and passwords to this Git project.

Customers and operations are stored only on the phone. Regularly export backups and store them separately from the phone. Restore replaces the current data on the phone after confirmation.

## Project rules

For coding rules, data model, and interface requirements, see `AGENTS.md`, `docs/ARCHITECTURE.md`, and `docs/DESIGN.md`.
