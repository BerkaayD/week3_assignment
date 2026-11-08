# Akıllı Ev Kontrol Merkezi

Evdeki akıllı cihazları tek ekrandan görüntülemek, açıp kapatmak ve anlık enerji tüketimini takip etmek için geliştirilmiş bir Android uygulamasıdır. Cihaz verileri bellek içinde simüle edilir.

## Özellikler

- Cihaz adına göre arama ve odaya göre filtreleme.
- Cihazları açma ve kapatma.
- Online ve açık cihazların toplam tüketimini anlık gösterme.
- Arama ve filtrelerden bağımsız, tüm evi kapsayan enerji sayacı.

## Teknolojiler

Kotlin, Jetpack Compose, Material 3, MVVM, ViewModel, Coroutines ve StateFlow.

## Testler

27 birim ve mimari test ile 8 emülatör UI testi başarıyla tamamlandı. Arama, oda filtreleme, enerji hesabı, cihaz kontrolleri ve ekran döndürmede durumun korunması kontrol edildi. Hızlı arama ayrıca üç tekrarda toplam 25 döngüyle denendi.
