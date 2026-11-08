# R8 karşılaştırması ve sürekli kaydırma ölçümü

**Güncel tekrar** 8 Kasım'da oda verileri düzeltilmiş APK ile **109,86 FPS** ölçüldü ve hızlı arama kontrolü başarısız oldu. [Son teslim kontrolü](../submission-2026-11-08/README.md). Bu dosya önceki APK'nın ölçümlerini kaydeder.

7 Kasım 2026 ölçümleri, 8 Kasım 2026'da rapora aktarıldı. `Week3_Pixel9_120Hz` üzerinde R8 etkin release APK ile üç adet 30 saniyelik sürekli kaydırmada birleşik **119,70 FPS** ölçüldü. Bu sonuç her karenin 8,33 ms içinde sunulduğu anlamına gelmez. Aynı seride sunum aralığının p95 değeri **10,084 ms**, en uzun aralık **16,770 ms** oldu.

## Yapılan değişiklik ve karşılaştırma

`app/build.gradle.kts` içinde release için `isMinifyEnabled = true` ve Android'in varsayılan optimize ProGuard kuralları etkinleştirildi. Bu karşılaştırma sırasında UI, ViewModel ve Repository davranışı değiştirilmedi. İmzalı APK boyutu 8.071.659 bayttan 1.628.504 bayta indi. Her iki APK yerel Android test anahtarıyla imzalandı ve kurulumdan sonra özeti kontrol edildi.

| APK | SHA-256 |
|---|---|
| A, R8 kapalı | `E356D5D05043CAE36D7370B96CF7C5947400C74EE470CFFFE0ECC91CC1C4164E` |
| B, R8 açık | `97CCA7A5A12EE6246EF8E0FE651AD2A4872B1A9FDDCB7FD3BB7498647162481C` |

A1, B1, B2, A2, A3, B3 sırasıyla ölçüldü. Aynı emülatör, 1080 × 2424 ekran, host GPU ve 120 Hz ayarları kullanıldı. Her iki APK için her kurulumdan sonra `cmd package compile -m speed -f com.example.week3assignment` çalıştırıldı. Her ölçüm yeni uygulama başlangıcı ve yaklaşık dört saniyelik ısınma içerdi. Bilgisayarın diğer iş yükleri sabitlenmedi.

| Kısa kaydırma protokolü | 1. tekrar | 2. tekrar | 3. tekrar | Birleşik FPS |
|---|---:|---:|---:|---:|
| A, R8 kapalı | 96,93 | 115,88 | 116,71 | 110,02 |
| B, R8 açık | 116,04 | 116,79 | 117,27 | 116,70 |

Altı son durum kontrolü geçti. A1 diğer A tekrarlarından belirgin şekilde düşük olduğundan ortalamalar arasındaki farkın tamamı R8'e bağlanamaz. Bu deneyde iki sürüme de uygulanan ART derlemesi nedeniyle önceki 103,46 FPS sonucu ile doğrudan bir R8 kazancı hesaplanamaz. [A sonuçları](A/results.csv), [B sonuçları](B/results.csv).

## Kesintisiz hareket protokolü

Kısa kaydırmalarda her `input swipe` çağrısının süreç ve girdi hazırlık süresi işaretlenen hareket penceresine girer. B serisindeki 81 hareketin nominal süresi 89,1 saniye, ölçülen hareket pencereleri toplamı 91,93 saniyeydi. Bu nedenle ayrıca tek dokunuşla kesintisiz hareket ölçüldü. Önceki sonuçlar korunmuştur.

[Bağımsız ölçüm sürücüsü](drag-driver/ContinuousDrag.java) uygulamadan ayrı bir instrumentation sürecinde çalışır. Parmak 30 saniye boyunca kaldırılmaz. Her 1,1 saniyede 600 piksel gidilip yön değiştirilir. En fazla 240 hareket girdisi/saniye üretilir. Başlangıç ve bitiş, Android'in `elapsedRealtimeNanos()` saatiyle işaretlenir. R8 APK, ART derleme ve ekran ayarları B serisiyle aynıdır. İki protokolün sonuçları doğrudan aynı ölçüm sayılmaz.

| Sürekli kaydırma | Sunulan kare | FPS | Sunum aralığı p95 |
|---|---:|---:|---:|
| 1 | 3.592 | 119,73 | 10,093 ms |
| 2 | 3.592 | 119,73 | 9,894 ms |
| 3 | 3.589 | 119,63 | 10,304 ms |
| Birleşik, 90,003 saniye | 10.773 | **119,70** | **10,084 ms** |

Üç son durum kontrolü geçti. Kare aralığı 8.333.333 ns, izlerde kayıp ve eşleşmeyen uygulama karesi sayısı sıfırdı. FrameTimeline jank etiketlerinin büyük çoğunluğu `Prediction Error` olduğundan %86,71 etiket oranı düşürülen kare yüzdesi olarak yorumlanmamalıdır. [Sonuç CSV](continuous/results.csv), [kayıt bilgileri](continuous/captures.json), [ham izler](continuous/).

R8 sürümünde ayrıca arama/oda son durum kontrolü ve 19 aç/kapat girdisinin ardından 91,0 W kontrolü geçti. Bunlar kısa davranış kontrolleridir, ayrı bir sürdürülebilir FPS kanıtı değildir.

## Tanı sınırları

Son sürekli kaydırma izine VSYNC kimliğiyle eşlenen 104 `gfxinfo` karesinde, ana iş parçacığının girdiyi almaya başlamasından render kuyruğuna vermesine kadar geçen sürenin p95 değeri 1,811 ms oldu. Komut gönderimi ile GPU tamamlanması arasındaki p95 süre 11,230 ms idi. İkincisi kuyruk ve sürücü beklemelerini de içerir, salt GPU hesaplama süresi değildir. [Ham gfxinfo kaydı](continuous/gfxinfo.txt).

Ek çekirdek zamanlama kaydı Trace Processor tarafından `FTRACE_STATUS_PARTIAL_PAGE_READ` hatasıyla reddedildi. Bu bozuk kayıt FPS sonuçlarına katılmadı. Kalan gecikmenin kesin kök nedeni belirlenmiş değildir. [Ek tanı yapılandırması](scheduler.pbtxt) ana FrameTimeline ölçümünden ayrıdır.

Mevcut sürekli kaydırma izleri proje kökünde yeniden analiz edilebilir.

```powershell
python docs/performance/emulator-120hz/analyze.py --processor tmp/bench-120hz/trace_processor_shell.exe --traces docs/performance/emulator-120hz/r8-ab/continuous --out tmp/continuous-results.csv
```

Bu APK'larda iki oda adı ASCII biçimindeydi. 8 Kasım teslim kontrolünde kaynak ödevle eşleştirilerek `Bahçe` ve `Yatak Odası` olarak düzeltildi. Buradaki SHA-256 değerleri ve ölçümler önceki APK'lara aittir.
