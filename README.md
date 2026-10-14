# Camaşırxana dəftəri

Restoran və şirkət müştərilərini, camaşırxana çəkisini, pərdə işlərinin metr və hər işə aid tarifini, borc və ödəniş əməliyyatlarını, ayrıca xərcləri, təqvim cəmlərini və hesablayıcı nəticələrini qeyd edən şəxsi Android tətbiqi. Tətbiq internetsiz işləyir və məlumatları cihazda saxlayır.

## Yığma və quraşdırma

1. Android Studio və tövsiyə olunan Android SDK komponentlərini quraşdırın: Android SDK Platform 36 və Build Tools. Layihə Android Studio-nun standart qovluğunda quraşdırılmış JDK-dan istifadə edir; yoxdursa `JAVA_HOME` dəyişənini JDK 17-yə, `ANDROID_HOME` dəyişənini Android SDK qovluğuna yönəldin.
2. Layihə qovluğunda PowerShell açıb `.\gradlew.bat assembleDebug` əmrini işlədin. İlk dəfə Gradle 9.5.0, sonra Android build asılılıqları endirilir. APK `app/build/outputs/apk/debug/app-debug.apk` ünvanında yaranır.
3. **Noutbukda emulatorla açmaq:** Android Studio-da **Tools → SDK Manager → SDK Tools** bölməsinə keçib **Android Emulator** və **Android SDK Platform-Tools** komponentlərini quraşdırın. Sonra **Tools → Device Manager → Create Virtual Device** seçin, Pixel telefon profili yaradın, sistem təsvirini endirib emulatoru başladın. Yaranmış APK faylını emulator pəncərəsinə sürükləyib buraxın; quraşdırmadan sonra tətbiqlər siyahısından **Camaşırxana dəftəri**ni açın.
4. APK-ni Android telefona köçürüb faylı açmaqla da quraşdırmaq olar. Android icazə istəsə, həmin fayl mənbəyindən quraşdırmaya icazə verin.

Emulator və ya USB ilə qoşulmuş telefon işləyərkən layihə qovluğunda `.\gradlew.bat installDebug` əmri APK-ni yenidən yığıb cihaza quraşdırır. Telefon üçün **Developer options** və **USB debugging** aktiv edilməli, bağlantı icazəsi telefonda təsdiqlənməlidir. USB quraşdırması barədə [Android cihaz təlimatına](https://developer.android.com/studio/run/device) baxın.

## Yeniləmələr və məlumatların qorunması

Yenilənmiş APK eyni tətbiq ID-si və imza açarı ilə quraşdırılmalıdır. Android Studio-nun sazlama APK-si `%USERPROFILE%\.android\debug.keystore` faylından istifadə edir. Tətbiqi yeniləmək və ya başqa kompüterə köçürmək istəyirsinizsə bu faylın ehtiyat nüsxəsini etibarlı yerdə saxlayın. Açar itərsə, başqa imza ilə hazırlanmış APK-ni quraşdırmaq üçün köhnə tətbiqi silmək lazım gələ bilər və bu zaman yerli məlumatlar silinə bilər. Əvvəlcə tətbiqdən ehtiyat nüsxəsi çıxarın. Uzunmüddətli istifadə üçün Android Studio-da ayrıca release açarı yaradıb qoruyun; açar faylını və parolları bu Git layihəsinə əlavə etməyin.

Müştərilər və əməliyyatlar yalnız telefonda saxlanılır. Ehtiyat nüsxələrini müntəzəm ixrac edib telefondan ayrı yerdə saxlayın. Bərpa təsdiqdən sonra telefondakı cari məlumatları əvəz edir.

## Layihə qaydaları

Kodlaşdırma qaydaları, məlumat modeli və interfeys tələbləri üçün `AGENTS.md`, `docs/ARCHITECTURE.md` və `docs/DESIGN.md` fayllarına baxın.
