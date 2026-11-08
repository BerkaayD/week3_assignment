# Doğrulama kaydı

## Son durum — hızlı arama düzeltmesi, 8 Kasım 2026

`kamera` → `kaeram` sorunu için ViewModel metni senkron yayımlayacak ve eski türetme sonuçlarının yeni girdiyi ezmesini önleyecek şekilde düzeltildi. UI stateless, hesaplamalar Default üzerinde kaldı. **27 birim/mimari test ve 8 UI testi geçti.** Debug ve R8 release derlemeleri başarılı, lint 0 hata / 8 uyarı.

Aynı ADB giriş yöntemiyle yapılan üç release arama tekrarı, toplam 25 döngü ardından doğru `kamera` sorgusu ve 106,0 W toplamıyla geçti. Yeni sürekli kaydırma sonuçları **118,76 / 119,43 / 119,20 FPS**, birleşik **119,13 FPS**. Önceki 119,70 tam olarak tekrarlanmadı. P95 sunum aralığı 10,20 ms olduğundan koşulsuz sabit 120 FPS doğrulanmış sayılmaz. [Güncel ölçüm ve düzeltme raporu](performance/emulator-120hz/searchfix-2026-11-08/README.md).

GitHub deposu ve teslim PDF'si kullanıcının açık isteğiyle son aşamaya ertelendi. Aşağıdaki kayıtlar düzeltme öncesi geçmiş bulgulardır.

## Güncel teslim kontrolü — 8 Kasım 2026

**Bütün gereksinimler tamamlandı denemez.** Temel kod, mimari ve derleme kontrolleri geçiyor. Ek release arama kontrolü başarısız oldu ve sürekli 120 FPS hedefi doğrulanmadı. Public GitHub deposu ile teslim PDF'si de henüz hazırlanmadı.

| Kontrol | Sonuç |
|---|---|
| Ödev veri seti | `Bahce` / `Yatak Odasi` değerleri kaynak ödevdeki `Bahçe` / `Yatak Odası` biçimine düzeltildi. Başlangıç oda listesi eşleştirildi. |
| Birim ve mimari testler | İlk kontrolde oda değerleri nedeniyle 3 test başarısızdı. Düzeltmeden sonra **25/25 geçti**. |
| Compose UI testleri | Güncel kaynakla `Week3_Pixel9_120Hz`, API 36 üzerinde **8/8 geçti**. Atlanan test yok. |
| Derleme | Debug uygulama, debug test APK'sı ve R8 etkin release başarıyla derlendi. |
| Lint | **0 hata, 8 uyarı**. Sürüm güncelleme önerileri, backup meta verisi ve resource shrinking önerisi. |
| Ekran görüntüleri | Beş ekran güncel UI testlerinden yeniden alındı ve görsel olarak kontrol edildi. |
| Release hızlı arama | **Başarısız**. Harf harf ADB girdisinde `kamera` yerine `kaeram` oluştu. Kök neden ayrıştırılmadı. |
| Release aç/kapat | 19 girdi ardından 91,0 W kontrolü geçti. |
| Release sürekli kaydırma | 100,90 / 109,23 / 119,46 FPS. Birleşik **109,86 FPS**, p95 **15,955 ms**. Sürekli 120 FPS doğrulanmadı. |

Mimari dosyaları ayrıca elle incelendi. Değişmez model, salt okunur Repository akışı, atomik güncelleme, `combine` / `flowOn(Default)` / `stateIn`, lifecycle sahibi ViewModel, `collectAsStateWithLifecycle`, stateless ekran ve kararlı liste anahtarları mevcut. Standart UI testleri tek işlemde metin değiştirir. Bu testlerin geçmesi, harf harf hızlı girdi kontrolündeki hatayı ortadan kaldırmaz.

[Güncel APK özeti, performans sonuçları ve başarısız arama kaydı](performance/emulator-120hz/submission-2026-11-08/README.md). Önceki 119,70 FPS ölçümü [R8 raporunda](performance/emulator-120hz/r8-ab/README.md) ayrı tutulur. Güncel sonuca yerine geçirilmez.

Teslimden önce kalan işler

- Hızlı arama girdisindeki hatanın nedenini ayırmak, düzeltmek ve aynı giriş senaryosuyla doğrulamak.
- 120 FPS gereksinimi için gözlenen değişkenliği gidermek veya karşılanmayan sonucu açıkça raporlamak. Mevcut sonuç koşulsuz bir 120 FPS geçişi değildir.
- Kaynakları kullanıcının kendi hesabındaki Public GitHub deposuna yüklemek ve bağlantının erişimini kontrol etmek. Kullanıcı 8 Kasım'da henüz yüklemediğini bildirdi.
- Depo bağlantısı ve uygulama ekran görüntülerini tek PDF içinde hazırlamak, bu PDF'yi ödev alanına son tarihten önce yüklemek. Yönerge belirli bir son tarih içermiyor.

## Önceki doğrulama kaydı

Tarih: 3 Kasım 2026 (Europe/Istanbul).

## Aşamalı mimari inceleme

| Aşama | Kontrol | Durum |
|---|---|---|
| İskelet | Tek `app` modülü, proje adı, wrapper, araç uyumluluğu | Tamamlandı |
| Model | Değişmez alanlar, 10 cihazlık seed, UI bağımlılığı yok | Tamamlandı |
| Repository | Tek kaynak, private mutable/public read-only akış, immutable atomik güncelleme, sıra korunması | Tamamlandı |
| ViewModel | AND filtresi, filtrelerden bağımsız toplam, tek UiState, combine/flowOn/stateIn, lifecycle scope | Tamamlandı |
| Compose | State + callback, iş hesabı yok, kararlı item ID, lifecycle-aware toplama | Tamamlandı |
| Birim/mimari testler | 15 ViewModel + 5 Repository + 5 mimari kontrol | 25/25 geçti |
| Android UI testleri | 7 Activity testi + 1 stateless sözleşme testi; gerçek yön değişimi ve 10 cihaza kaydırma dahil | 8/8 geçti |

Başlangıç snapshot'ı sabit ödev verisine özgüdür ve reaktif türetmeyle eşitliği test edilir; ayrıntılı gerekçe README'dedir. Tüm yeniden hesaplamalar Default üzerindedir. Arayüzde sayı biçimlendirmesi iş hesabı sayılmaz. Veri alanlarının ve bağımlılık yönlerinin manuel incelemesi kaynak tarama testlerine ek olarak yapıldı.

## Derleme ve test ortamı

- Windows, JDK 21.0.1; Gradle 9.4.1, AGP 9.2.1, compile SDK 37, target SDK 36.
- AVD: Pixel_9; model `sdk_gphone64_x86_64`; Android 16 / API 36; x86_64, yazılımsal GPU.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:lintDebug`: başarıyla tamamlandı.
- Lint: 0 hata, 7 uyarı. Bunlar target/bağımlılık güncelleme önerileri ve backup meta verisi uyarılarıdır; bastırılmadı.
- İlk UI denemesinde ekranı kapalı emülatör Compose hiyerarşisi sunmadı; başarısız çalıştırma geçiş sayılmadı. Ekran uyandırılarak yeniden test edildi ve geçti.
- Android testlerinde filtre, toggle ve toplam durumları hem `ActivityScenario.recreate()` hem de portrait → landscape yön değişimi boyunca korundu. On cihazın tamamına kaydırılarak ulaşıldı.
- Beş uygulama ekran görüntüsü `docs/screenshots` içinde; testlerde doğrulanan durumlarda emülatörden alındı.

Yerel raporlar (derleme sonrası oluşur, Git'e dahil edilmez):

```text
app/build/reports/tests/testDebugUnitTest/index.html
app/build/test-results/testDebugUnitTest/TEST-*.xml
app/build/reports/androidTests/connected/debug/index.html
app/build/reports/lint-results-debug.html
app/build/outputs/apk/debug/app-debug.apk
```

## Performans

4 Kasım 2026 tarihinde Pixel_9 AVD üzerinde debug APK ile kaydırma ölçüldü. Android 16 emülatörünün bildirdiği tek ekran modu 60 Hz idi. Isınma kaydırmasının ardından 1,2 saniyelik üç dikey kaydırma uygulandı. Her denemede MainActivity katmanının SurfaceFlinger `--latency` çıktısındaki yeni, geçerli sunum zamanları kullanıldı. FPS, ilk ve son sunum arasındaki kare aralığı sayısının geçen saniyeye bölünmesiyle hesaplandı. P95, ardışık sunumlar arasındaki sürenin 95. yüzdeliğidir.

| Deneme | Sunulan kare | İlk–son sunum (ms) | FPS | P95 aralık (ms) |
|---|---:|---:|---:|---:|
| 1 | 53 | 1625,75 | 31,99 | 40,42 |
| 2 | 52 | 1407,57 | 36,23 | 36,33 |
| 3 | 52 | 1421,55 | 35,88 | 44,04 |

Üç pencere birlikte yaklaşık **34,6 FPS** verdi. [Ham sunum zamanları (ns)](performance/emulator-scroll/) her deneme için ayrı saklandı. Bu ölçüm yalnızca emülatördeki kaydırmayı kapsar.

**P-01:** Kaynak incelemesinde Main üzerinde iş filtresi/toplam hesabı, bloklayan IO, sleep, GlobalScope veya yapay gecikme bulunmadı. Eşzamanlı Repository testi ve UI davranış testleri donanım kare süresi kanıtı değildir.

**P-02 emülatör testi, 7 Kasım 2026.** Release 1.0, host GPU kullanan `Week3_Pixel9_120Hz` AVD üzerinde çalıştırıldı. Sanal ekran modu ve Android çizim hızı 120 Hz, SurfaceFlinger periyodu ve uygulamanın kare aralığı 8.333.333 ns olarak doğrulandı. Kaydırma, arama/oda ve aç/kapat için yaklaşık 30 saniyelik üçer Perfetto kaydı alındı.

Aktif kaydırma denemeleri 113,88 / 114,81 / 82,26 FPS verdi. Birleşik sonuç **103,46 FPS**, sunum aralığı p95 değeri **15,89 ms** oldu. Sürekli 120 FPS hedefi bu koşullarda karşılanmadı. Ana serideki dokuz son durum kontrolü geçti. Ayrı bir toplu geri silme denemesinde beklenen `kamera` sorgusu `aeram` olarak görüldü. [Ölçüm, deneme sonuçları ve tekrar çalıştırma adımları](performance/emulator-120hz/README.md)
