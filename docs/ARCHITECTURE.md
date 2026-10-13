# Arxitektura

## Ümumi baxış

Bu tətbiq bir nəfərin istifadə etməsi üçün hazırlanıb və internetsiz işləyir. Bütün biznes məlumatları telefonda Room verilənlər bazasında qalır. Hesab, server, analitika və şəbəkə bağlantısı yoxdur.

## Tətbiq qatları

1. **Compose interfeysi** — icmal, müştərilər, müştəri əməliyyatları, təqvim, adi hesablayıcı və parametrlər.
2. **Sahə qaydaları** — AZN/qəpik çevirməsi, hesablayıcı hesablamaları, gün/həftə/ay sərhədləri və ehtiyat nüsxəsinin yoxlanması.
3. **Məlumatların saxlanması** — Room müştəriləri, tarixli əməliyyatları və parametrləri saxlayır. Sxem dəyişiklikləri məlumatı qoruyan miqrasiya ilə edilməlidir.
4. **Ehtiyat nüsxəsi** — Android Storage Access Framework vasitəsilə istifadəçinin seçdiyi fayla versiyalı JSON yazılır və fayldan bərpa olunur.

## Əsas məlumatlar

- **Müştəri:** sabit ID, adı, istəyə görə əlaqə məlumatı və qeyd, aktiv/arxiv vəziyyəti.
- **Əməliyyat:** sabit ID, müştəri ID-si, yerli təqvim tarixi (`yyyy-MM-dd`), mədaxil/məxaric istiqaməti, qəpiklə məbləğ, istəyə görə səbəb və kiloqram.
- **Tətbiq parametrləri:** Azərbaycan dili və ehtiyat nüsxəsinin format versiyası.

Hər müştərinin bir neçə əməliyyatı ola bilər. Məbləğ həmişə qəpiklə qeyri-mənfi tam ədəd kimi saxlanır; mədaxil/məxaric istiqaməti balans hesablamasını müəyyən edir. Kiloqram göstərilə bilər, amma öz-özünə məbləğ hesablamır. Balans mədaxil məbləğlərinin cəmindən məxaric məbləğlərinin cəmini çıxmaqla tapılır.

## Cəmlər

Əməliyyatlar cihazın yerli təqvim tarixinə görə qruplaşdırılır. Həftə yerli həftənin ilk günündən başlayır. İcmal və təqvim seçilən gün, həftə və ay üçün kiloqramı, mədaxili, məxarici və balansı göstərir. Müştərini arxivləmək tarixçəni saxlayır. Daimi silmə ayrıca təsdiq olunur və həmin müştərinin əməliyyatlarını da silir.

## Naviqasiya və asılılıqlar

Alt menyuda İcmal, Müştərilər, Təqvim, Hesablayıcı və Parametrlər var. Müştərinin səhifəsində əməliyyat jurnalı yerləşir. Şəbəkə icazəsi və fon xidməti tələb olunmur. Layihə Android Studio-da asan yığılmalı və birbaşa quraşdırılan APK yaratmalıdır.
