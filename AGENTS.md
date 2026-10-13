# Kodlaşdırma agentləri üçün təlimat

## Layihə
- Restoran və şirkət müştəriləri üzrə camaşırxana əməliyyatlarını saxlayan, şəxsi və internetsiz Android tətbiqi.
- Kotlin, Jetpack Compose və Room istifadə edin. Bulud, analitika və giriş sistemi əlavə etməyin.
- Tətbiqin davranışını dəyişməzdən əvvəl `docs/ARCHITECTURE.md` və `docs/DESIGN.md` fayllarını oxuyun.
- Tətbiqdə görünən bütün mətnlər Azərbaycan dilində olmalıdır.

## Məhsul qaydaları
- Müştərinin adı, istəyə görə əlaqə məlumatı və qeyd, aktiv/arxiv vəziyyəti olur.
- Müştərinin səhifəsində tarixçəsi və hər mədaxil/məxaric əməliyyatı ayrıca göstərilir. Əməliyyatda müştəri, tarix, məbləğ, istiqamət, istəyə görə səbəb və kiloqram saxlanır.
- Eyni müştəriyə istənilən sayda əməliyyat əlavə, düzəliş və silmək mümkündür.
- Pul qəpiklə tam ədəd kimi saxlanır; ekranda iki rəqəmli AZN və `₼` göstərilir.
- Hesablayıcı adi gündəlik kalkulyator kimi işləyir. Nəticə müştəri və tarix seçilərək yeni əməliyyata göndərilə bilər. Tarixçədəki məbləğ hesablayıcıya yüklənə və həmin əməliyyat yenilənə bilər.
- İcmal və təqvim cəmləri cihazın yerli gün, həftə və ay sərhədlərinə əsaslanır. Arxiv müştərinin əməliyyatlarını saxlayır. Müştərinin daimi silinməsi həm müştərinin, həm də əməliyyatlarının silinəcəyini təsdiqlətməlidir.
- Ehtiyat nüsxəsinə müştərilər, əməliyyatlar və tətbiq parametrləri daxil olmalıdır. Bərpa istifadəçinin seçdiyi fayldan, təsdiqdən sonra aparılır.

## Mühəndislik
- Mümkün olduqca sahə qaydalarını Compose interfeysindən və verilənlər bazasından ayırın.
- Sabit ID-lər və açıq tarix dəyərləri işlədin. Sxem dəyişəndə əvvəlki məlumatları qoruyan Room miqrasiyası yazın; bazanı səssizcə silməyin.
- Lazımsız asılılıqlar əlavə etməyin. İnternet icazəsi tələb olunmur.
- README-də yığma, quraşdırma və yeniləmə təlimatlarını aktual saxlayın. APK yenilənəndə tətbiq ID-si və imza açarı eyni qalmalıdır.
- Məxfi açarları, imza fayllarını, yerli SDK yollarını və yaradılmış build fayllarını Git-ə daxil etməyin.
