# Hızlı arama düzeltmesi ve FPS tekrarı

8 Kasım 2026. Önceki `kamera` → `kaeram` hatası için ViewModel durum yayını düzeltildi. Release arama kontrolünün üç tekrarı geçti. Sürekli kaydırmanın üç tekrarında birleşik **119,13 FPS** ölçüldü. Önceki 119,70 FPS tam olarak tekrarlanmadı.

## Düzeltme

Metin alanına geri verilen arama metni daha önce Default üzerindeki `combine` hesabını bekliyordu. ViewModel artık metin ve oda seçimini tek dış `StateFlow` içinde senkron yayımlar. Arka plan sonucu yalnızca sorgusu ve oda seçimi güncel girdiye eşitse yayımlanır. Bu, eski sonucun yeni metni geri almasını önler. Liste ve enerji hesabı hâlâ `flowOn(Default)` üzerinde çalışır. UI stateless ve ViewModel Compose bağımsızdır. Abonelik bittiğinde türetme aboneliği kapanır, `WhileSubscribed(5_000)` hesaplamayı durdurur.

Bu değişiklik, [Compose metin alanının yeni değeri bir sonraki kareye kadar geri alma sözleşmesine](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextField) uygundur. Hesap bitene kadar önceki liste gösterilir. Metin girdisi hesaplamayı beklemez.

## Doğrulama

- **27/27 birim ve mimari test** geçti. İki yeni regresyon testi, arka plan çalışmadan metnin yayımlanmasını ve yeniden abonelikte eski metnin arada bile yayımlanmamasını denetler.
- **8/8 Compose UI testi** geçti. Debug, test APK'sı ve R8 release derlemeleri başarılı. Lint 0 hata ve 8 uyarı bildirdi.
- Önceki başarısız kontrolün `record.py` ve `workload.sh` girdi yöntemi değiştirilmedi. Ctrl+A/Sil, 60 ms aralıklı harfler, Bahçe ve Salon seçimleri uygulandı. Üç kayıt için 10 saniye istendi, devam eden döngüler tamamlandı. 8 + 9 + 8 = **25 döngü** gerçekleşti. Üç kaydın son durumunda `kamera`, seçili Salon, boş liste ve **106,0 W** doğrulandı. Her ara döngüye ayrı UI ağacı kontrolü uygulanmadı. [Kayıt bilgileri ve izler](search/captures.json).

Release APK SHA-256 özeti `4A4A1B98AE36BBC56883C1CFCCDE79D7084D7329D15904CD13FAE369D903316B`. Yerel Android test anahtarıyla imzalandı, kuruldu ve ART için `cmd package compile -m speed -f com.example.week3assignment` uygulandı.

## FPS

Önceki 119,70 FPS ölçümündeki sürekli dokunma protokolü kullanıldı. `Week3_Pixel9_120Hz`, API 36, 1080 × 2424, host GPU / NVIDIA RTX 3060 Laptop GPU. Sanal ekran ve render hızı 120 Hz, uygulama kare periyodu 8.333.333 ns. Her kayıtta yeni uygulama başlangıcı, yaklaşık dört saniyelik ısınma ve 30 saniye kesintisiz hareket var. Parmak her 1,1 saniyede 600 piksel gidip yön değiştiriyor. İşaretler guest `elapsedRealtimeNanos()` saatinden, sunumlar Perfetto FrameTimeline'dan alındı. Bilgisayarın diğer yükleri sabitlenmedi.

| Tekrar | FPS | Sunum aralığı p95 |
|---|---:|---:|
| 1 | 118,76 | 10,207 ms |
| 2 | 119,43 | 10,104 ms |
| 3 | 119,20 | 10,277 ms |
| Birleşik | **119,13** | **10,200 ms** |

90,004 saniyede 10.722 kare sunuldu. En uzun sunum aralığı 22,437 ms. Üç son durum kontrolü geçti. İz veri kaybı ve eşleşmeyen kare sayısı sıfırdı. Jank etiketleri düşürülen kare yüzdesi olarak yorumlanmadı. [CSV](continuous/results.csv), [kayıt bilgileri](continuous/captures.json), [ham izler](continuous/).

119,70 FPS'ye göre fark −0,57 FPS. Bu sonuç yaklaşık 120 FPS düzeyine yakındır, her karede 8,33 ms bütçesinin karşılandığı veya her senaryoda sabit 120 FPS sağlandığı iddia edilmez. Önceki 109,86 FPS ile farkın tamamı arama düzeltmesine bağlanamaz. Aynı host yüküyle kontrollü A/B karşılaştırması yapılmadı.

GitHub yayını ve teslim PDF'si kullanıcının isteğiyle sonraya bırakıldı.
