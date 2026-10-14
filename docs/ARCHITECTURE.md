# Arxitektura

## Ümumi baxış

Bu tətbiq bir nəfərin istifadə etməsi üçün hazırlanıb və internetsiz işləyir. Bütün biznes məlumatları telefonda Room verilənlər bazasında qalır. Hesab, server, analitika və şəbəkə bağlantısı yoxdur.

## Tətbiq qatları

1. **Compose interfeysi** — icmal, müştərilər, müştəri əməliyyatları, ayrıca xərclər bölməsi, təqvim, adi hesablayıcı və parametrlər.
2. **Sahə qaydaları** — AZN/qəpik çevirməsi, hesablayıcı hesablamaları, gün/həftə/ay sərhədləri və ehtiyat nüsxəsinin yoxlanması.
3. **Məlumatların saxlanması** — Room müştəriləri, tarixli əməliyyatları, ayrıca xərcləri və parametrləri saxlayır. Sxem dəyişiklikləri məlumatı qoruyan miqrasiya ilə edilməlidir.
4. **Ehtiyat nüsxəsi** — Android Storage Access Framework vasitəsilə istifadəçinin seçdiyi fayla versiyalı JSON yazılır və fayldan bərpa olunur.

## Əsas məlumatlar

- **Müştəri:** sabit ID, adı, istəyə görə əlaqə məlumatı və qeyd, kq üçün dəyişdirilə bilən tarif, aktiv/arxiv vəziyyəti.
- **Əməliyyat:** sabit ID, müştəri ID-si, yerli təqvim tarixi (`yyyy-MM-dd`), xidmət haqqı/ödəniş istiqaməti, qəpiklə məbləğ, tətbiq olunmuş tarif, istəyə görə səbəb, kiloqram və ya hər pərdə işi üçün ayrıca metr və metr tarifi.
- **Xərc:** sabit ID, adı, yerli təqvim tarixi (`yyyy-MM-dd`) və qəpiklə məbləğ.
- **Tətbiq parametrləri:** Azərbaycan dili və ehtiyat nüsxəsinin format versiyası.

Hər müştərinin bir neçə əməliyyatı ola bilər. Məbləğ həmişə qəpiklə qeyri-mənfi tam ədəd kimi saxlanır. Xidmət haqqı əməliyyatında kiloqram və müştərinin tarifi varsa, məbləğ kiloqram × tarif kimi hesablanır və həmin əməliyyatda tarifin surəti saxlanır. Pərdə borcunda metr və hər qeyd üçün ayrıca daxil edilən metr tarifi vurulur; tarif müştərinin daimi tarifini dəyişmir və əməliyyatda saxlanır. Müştərinin ödəniləcək qalığı xidmət haqlarının cəmindən ödənişlərin cəminin çıxılması ilə tapılır. Tətbiqdə ümumi mədaxil/məxaric cəmləri saxlanılır.

## Cəmlər

Əməliyyatlar və xərclər cihazın yerli təqvim tarixinə görə qruplaşdırılır. Həftə yerli həftənin ilk günündən başlayır. İcmal seçilən gün və ayı, yaxud bütün tarixçəni; təqvim isə seçilən gün, həftə və ayı göstərir. Dövr cəmlərinə xidmət haqqı, ödəniş, qalıq, kq və pərdə metrləri daxildir. Qalıq xidmət haqlarının cəmindən ödənişlərin cəminin çıxılması ilə hesablanır. Xərclər bölməsi seçilən gün və ya ayın xərc qeydlərini və həmin dövrün cəmini göstərir. Müştəri siyahısı hər müştərinin cari ödəniləcək qalığını göstərir. Müştəri səhifəsindən əməliyyat tarixçəsini ayrıca təsdiqlə silmək olar; bu, müştəri məlumatlarını və tarifini saxlayır. Müştərini arxivləmək tarixçəni saxlayır. Daimi silmə ayrıca təsdiq olunur və həmin müştərinin əməliyyatlarını da silir.

## Naviqasiya və asılılıqlar

Alt menyuda İcmal, Müştərilər, Xərclər, Təqvim, Hesablayıcı və Parametrlər var. Müştərinin səhifəsində əməliyyat jurnalı yerləşir. Şəbəkə icazəsi və fon xidməti tələb olunmur. Layihə Android Studio-da asan yığılmalı və birbaşa quraşdırılan APK yaratmalıdır.
