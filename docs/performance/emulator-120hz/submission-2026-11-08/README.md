# 8 Kasım 2026 teslim öncesi release kontrolü

**Teslime tam hazır olarak işaretlenmedi.** Güncel release APK'da sürekli kaydırma ortalaması **109,86 FPS** oldu. ADB ile harf harf arama girdisi uygulanan kontrolde beklenen `kamera` sorgusu `kaeram` olarak görüldü. Bu iki bulgu önceki başarılı birim ve Compose UI testlerinden ayrı tutulmalıdır.

## Sürüm ve ortam

Kaynak veriyle uyuşmayan iki oda adı `Bahçe` ve `Yatak Odası` olarak düzeltildikten sonra `:app:assembleRelease` çalıştırıldı. R8 açık, APK yerel Android test anahtarıyla imzalıdır. Dosya boyutu 1.628.504 bayt, SHA-256 özeti `A3041B70DFB96746B117067EE9C6CEDCD20EB445FCC5FAC03C23A8E050FF15E3` oldu. Emülatörde kurulu APK'nın özeti de aynı çıktı.

`Week3_Pixel9_120Hz`, Android 16 / API 36, 1080 × 2424, host GPU ve NVIDIA GeForce RTX 3060 Laptop GPU kullanıldı. Ekran ve çizim hızı 120 Hz, SurfaceFlinger periyodu ve son uygulama karesinin aralığı 8.333.333 ns olarak doğrulandı. APK'ya `cmd package compile -m speed -f com.example.week3assignment` uygulandı. Bilgisayarın diğer iş yükleri kontrol altında tutulmadı.

## Kaydırma

[Sürekli dokunma protokolü](../r8-ab/README.md) kullanıldı. Her tekrarda uygulama yeniden açıldı, 106,0 W kontrol edildi ve yaklaşık dört saniye ısınma yapıldı. Ardından tek dokunuşla 30 saniye boyunca, her 1,1 saniyede 600 piksel gidilip yön değiştirildi. Başlangıç ve bitiş Android'in nanosaniyelik açılış saatiyle işaretlendi.

| Tekrar | Sunulan kare | FPS | Sunum aralığı p95 |
|---|---:|---:|---:|
| 1 | 3.027 | 100,90 | 18,348 ms |
| 2 | 3.277 | 109,23 | 16,185 ms |
| 3 | 3.584 | 119,46 | 10,044 ms |
| Birleşik, 90,003 saniye | 9.888 | **109,86** | **15,955 ms** |

En uzun sunum aralığı 56,016 ms oldu. Üç son durum kontrolü geçti, iz veri kaybı ve eşleşmeyen kare sayısı sıfırdı. Sıfır `Dropped Frame` etiketi, her ekran yenilemesinde yeni uygulama karesi sunulduğu anlamına gelmez. Hız doğrudan sunum sayısı ve ölçülen süreden hesaplandı. [Sonuç CSV](continuous/results.csv), [kayıt bilgileri](continuous/captures.json), [ham izler](continuous/).

7 Kasım'da önceki R8 APK ve aynı sürekli dokunma protokolüyle 119,70 FPS ölçülmüştü. Bu sonuç güncel tekrarda korunmadı. Oda metni değişikliği ile performans farkı arasında nedensellik kurulmadı. Her senaryoda sürdürülebilir 120 FPS doğrulanmış değildir.

## Release davranış kontrolü

[Mevcut kayıt betiği](../record.py) ve [girdi betiği](../workload.sh) ile arama/oda ve aç-kapat senaryoları birer kez çalıştırıldı. Aramada önce Tümü seçildi, sorgu Ctrl+A ve Sil ile temizlendi, harfler arasında 60 ms beklendi, Bahçe ve Salon seçildi.

| Senaryo | Sonuç |
|---|---|
| Arama/oda, dört döngü | **Başarısız**. Beklenen `kamera`, görülen `kaeram`. Salon seçili, liste boş ve toplam 106,0 W kontrolleri doğru. |
| Aç/kapat, 19 girdi | Geçti. Son toplam 91,0 W. |

Hatalı sorgunun giriş üretimi, metin alanı durumu ve asenkron durum yayını arasındaki kesin nedeni henüz ayrıştırılmadı. Sorun çözülmüş olarak işaretlenmedi. Standart Compose UI testi metni tek işlemle değiştirdiğinden bu harf harf giriş davranışını kanıtlamaz. [Son durumlar ve ham izler](smoke/captures.json).

Yeni sonuçları yeniden analiz etmek için proje kökünde çalıştırılabilir.

```powershell
python docs/performance/emulator-120hz/analyze.py --processor tmp/bench-120hz/trace_processor_shell.exe --traces docs/performance/emulator-120hz/submission-2026-11-08/continuous --out tmp/submission-fps-results.csv
```
