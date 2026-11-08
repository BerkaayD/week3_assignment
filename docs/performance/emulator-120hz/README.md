# Pixel 9 emülatöründe 120 FPS testi

**Güncel durum** 8 Kasım'daki son APK tekrarında **109,86 FPS** ve başarısız hızlı arama kontrolü görüldü. [Teslim öncesi kontrol](submission-2026-11-08/README.md). Önceki R8 sürümünün kesintisiz dokunmayla **119,70 FPS** sonucu [ayrı raporda](r8-ab/README.md) yer alır. Aşağıdaki 103,46 FPS serisi ilk ölçümün kaydıdır.

**7 Kasım 2026.** Android ve uygulama dokuz kaydın tamamında 8,33 ms kare periyodu bildirdi. Üç tekrarın aktif kaydırmaları birlikte **103,46 FPS** verdi. Sürekli 120 FPS hedefine ulaşılamadı.

| Senaryo | 1. tekrar | 2. tekrar | 3. tekrar | Birleşik hız | FrameTimeline jank oranı |
|---|---:|---:|---:|---:|---:|
| Aktif kaydırma | 113,88 | 114,81 | 82,26 | **103,46 FPS** | %35,62 |
| Arama + oda | 100,93 | 91,12 | 58,74 | 83,46 sunum/sn | %38,35 |
| Aç/kapat | 119,03 | 76,01 | 83,50 | 92,85 sunum/sn | %68,38 |

Arama ve aç/kapat hızları, girdiler arasındaki beklemeleri içeren **tam senaryo penceresinde** hesaplandı. Aynı tam pencere tanımıyla kaydırma hızı 103,01 sunum/sn oldu. Jank oranları her üç satırda tam pencereye aittir. Üçüncü kaydırmada hız 82,26 FPS'ye düşerken izde SurfaceFlinger CPU zaman sınırı ve tampon kuyruğu gecikmesi etiketleri görüldü. Bilgisayarın diğer iş yükleri deney boyunca sabitlenmedi. Son ölçümden sonra Android'in termal durum kodu 0 olarak okundu.

78 kaydırma hareketi içinde 89,87 saniyede 9.298 kare sunuldu. Ardışık sunum aralığı medyanda **8,83 ms**, p95'te **15,89 ms**, p99'da **19,10 ms**, en uzun aralıkta **57,14 ms** oldu. Örneğin p95 değeri, aralıkların %95'inin 15,89 ms veya daha kısa olduğunu gösterir.

Üç senaryonun üçer tekrarı toplam 277,01 saniye sürdü. 66 arama/oda döngüsü ve 385 aç/kapat girişi uygulandı. Dokuz son durum kontrolü geçti. Ayrı çoklu silme denemesindeki sorgu hatası aşağıda kayıtlıdır. Dokuz izde Trace Processor veri kaybı sayacı 0 çıktı. Son aç/kapat izinde, `dumpsys SurfaceFlinger --latency` listesinin kapsadığı ortak 96 sunum için en yakın FrameTimeline zamanı ile farkın medyanı 0,011 ms, en büyüğü 0,132 ms'den küçüktü. Kurulu APK'nın SHA-256 özeti yerel APK ile eşleşti. [Deneme sonuçları](results.csv) · [Ham izler](traces/) · [Bağımsız zaman listesi](sf-last-window.txt)

## Test ortamı

Android 16 (API 36) üzerinde çalışan ayrı bir Pixel 9 AVD kullanıldı. Test cihazının adı `Week3_Pixel9_120Hz`. x86_64 Google APIs Play Store 16 KB sistem imajı revizyon 7, Android Emulator 36.6.11, 4 sanal CPU, 3072 MB RAM ve 1080 × 2424 ekranla çalıştırıldı. Penceresiz emülatörde `-gpu host` seçildi. Android'in bildirdiği OpenGL sürücüsü NVIDIA GeForce RTX 3060 Laptop GPU üzerinde çalıştı.

**120 Hz kontrolü.** Cihaz profilinde `hw.lcd.vsync=120` ayarlandı. İlk kısa denemede aktif ekran modu 120 Hz iken Android'in `renderFrameRate` değeri 60 Hz çıktı. Bu AVD'de `min_refresh_rate` ve `peak_refresh_rate` 120,0 yapıldığında çizim hızı da 120 Hz oldu. Kayıt betiği başlamadan önce aktif modu, çizim hızını ve SurfaceFlinger periyodunu doğrular. Her ölçümden sonra uygulamanın `gfxinfo framestats` çıktısındaki son karenin `FrameInterval` değerini kontrol eder. [Cihaz ve deneme kayıtları](traces/captures.json)

**APK.** Projenin `:app:assembleRelease` çıktısı kullanıldı. Uygulama sürümü 1.0. APK yerel Android test anahtarıyla imzalandı. Kurulu paketin bayrakları `HAS_CODE ALLOW_CLEAR_USER_DATA` olarak okundu. Test APK'sının SHA-256 özeti `E356D5D05043CAE36D7370B96CF7C5947400C74EE470CFFFE0ECC91CC1C4164E`.

## Senaryolar

Her tekrarda uygulama yeniden başlatılır, başlangıçta 106,0 W olduğu kontrol edilir ve aynı etkileşimle yaklaşık dört saniye ısınma yapılır. Ölçüm bölümü 30 saniye dolunca yürürlükteki hareketi tamamlar. Uygulama girişleri ve doğrulamalar ayrı adımlardır. Ekran ağacını okuyan kontroller kayıt penceresinin dışında çalışır.

| Senaryo | Uygulanan girdi | Son durum kontrolü |
|---|---|---|
| Kaydırma | Listenin ortasında 600 piksel uzunluğunda, 1,1 saniyelik yukarı/aşağı hareketler | Activity görünür ve ana pencere yeni kareler sunar |
| Arama + oda | Tümü seçilir, eski sorgu Ctrl+A ve Sil ile temizlenir. `kamera` harfleri arasında 60 ms beklenerek yazılır. Bahçe ve ardından Salon seçilir. | Sorgu `kamera`, Salon seçili, sonuç boş, toplam 106,0 W |
| Aç/kapat | Köşe Lambası düğmesine her dokunuştan sonra 180 ms beklenir. Dokunuş sayısı tek olacak şekilde tamamlanır. | Toplam 91,0 W |

İlk uzun arama denemesinde eski sorgu altı geri silme olayı tek komutta gönderilerek temizlendi. Doğrulayıcı son sorguyu `aeram` olarak okudu ve bu deneme başarısız oldu. Bu giriş biçimindeki sorunun kök nedeni ayrıştırılmadı. Kayıt ayrıca saklandı. Üç ana arama tekrarı tabloda belirtilen seçip silme işlemini kullanır. [Ayrı arama kaydı](exploratory/search-batched-delete.perfetto-trace.gz)

## Hesaplama

1. Perfetto FrameTimeline içinde uygulama sürecine ve `MainActivity` katmanına ait `Is Buffer? = Yes` kareleri seçilir. Uygulama karesi, `display_frame_token` ile SurfaceFlinger ekran karesine bağlanır. Erken, zamanında veya geç sunulan her benzersiz ekran karesi bir kez sayılır. Sunum zamanı için ekran karesinin bitişi kullanılır. [SQL sorgusu](frames.sql) · [Perfetto FrameTimeline belgeleri](https://perfetto.dev/docs/data-sources/frametimeline)
2. Toplam pencere hızı, pencere içindeki sunum sayısının saniye cinsinden süresine bölümüdür. Aktif kaydırma FPS hesabında yalnızca işaretlenmiş kaydırma hareketleri kullanılır. Üç tekrarın kare sayıları ve süreleri toplanır. Arama ve aç/kapat pencereleri, girdiler arasındaki beklemeleri de içerir.
3. Kaydırma p50, p95, p99 ve maksimum aralıkları, aynı hareket içinde ardışık iki sunum arasındaki sürelerden hesaplanır. Bir hareketin sonu ile diğerinin başlangıcı birleştirilmez. Senaryo işaretleri, emülatördeki açılıştan beri geçen süreyi veren saatten 10 ms çözünürlükle alınır. Tek tek kare zamanları doğrudan izden nanosaniye biriminde okunur.
4. Jank oranının paydası, ölçüm penceresinde başlayan tamamlanmış uygulama kareleridir. Payı, FrameTimeline'ın bir jank türü atadığı karelerdir. Düşürülen kare, uygulamanın işi geç bitirmesi ve jank ayrı sütunlarda tutulur. Örneğin `Prediction Error` ve `Buffer Stuffing` etiketleri de jank sayımına girer. [Sonuçlar](results.csv)

## Tekrar çalıştırma

Kurulu AVD'yi aynı seçeneklerle başlatın. Ölçüm sırasında test ekranına elle giriş yapmayın. Aşağıdaki örnek proje kökünde, ADB ve Python 3 erişilebilirken çalışır. `$tp` için resmi Perfetto Trace Processor v59.1 çalıştırılabilir dosyasının yolunu kullanın.

```powershell
Start-Process "$env:ANDROID_HOME/emulator/emulator.exe" -ArgumentList @('-avd','Week3_Pixel9_120Hz','-port','5562','-gpu','host','-no-window','-no-audio','-no-boot-anim','-no-snapshot','-no-metrics') -WindowStyle Hidden
adb -s emulator-5562 wait-for-device
# Android açılışı tamamlandıktan ve aynı release APK kurulduktan sonra:
adb -s emulator-5562 shell settings put system peak_refresh_rate 120.0
adb -s emulator-5562 shell settings put system min_refresh_rate 120.0
adb -s emulator-5562 shell input keyevent KEYCODE_WAKEUP
adb -s emulator-5562 shell wm dismiss-keyguard
$suite = 'docs/performance/emulator-120hz'
$tp = 'tmp/bench-120hz/trace_processor_shell.exe'
python "$suite/record.py" --serial emulator-5562 --out tmp/120hz-yeni --rounds 3 --seconds 30
python "$suite/analyze.py" --processor $tp --traces tmp/120hz-yeni --out tmp/120hz-yeni/sonuclar.csv
```

Mevcut dokuz kaydı yeniden analiz etmek için yalnızca `analyze.py --processor <yol>` çalıştırılabilir. Sıkıştırılmış izleri [Perfetto arayüzünde](https://ui.perfetto.dev/) açıp uygulamanın FrameTimeline satırı incelenebilir.
