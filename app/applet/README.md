# 🕌 Namaz Arkadaşım (v1.0 - Resmi Kararlı Sürüm)

<p align="center">
  <b>T.C. Diyanet İşleri Başkanlığı Uyumlu Vakitler, Sade 5 Vakit Takibi, Akıllı Namaz Arkadaşı Senkronizasyonu ve Maneviyat Karnesi</b>
</p>

<p align="center">
  <a href="https://github.com/bilkil53/namaz-arkadasim/releases/latest/download/Namaz_Arkadasim.apk">
    <img src="https://img.shields.io/badge/İndir-Namaz%20Arkadaşım%20v1.0%20(APK)-00897B?style=for-the-badge&logo=android&logoColor=white" alt="APK İndir" />
  </a>
  <img src="https://img.shields.io/badge/Sürüm-v1.0%20(Derleme%205)-0D47A1?style=for-the-badge" alt="Sürüm" />
  <img src="https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Mimari-MVVM%20%7C%20Jetpack%20Compose-purple?style=for-the-badge" alt="Mimari" />
  <img src="https://img.shields.io/badge/Gizlilik-%25100%20Reklamsız%20%26%20Yerel%20Veritabanı-brightgreen?style=for-the-badge" alt="Gizlilik" />
</p>

---

## 🎯 Uygulamanın Amacı ve Felsefesi

**Namaz Arkadaşım**, Müslümanların günlük namaz ibadetlerini aksatmadan, huzur ve manevi bir şuurla yerine getirmelerini desteklemek amacıyla geliştirilmiştir.

Uygulamanın temel gayeleri:
1. **İbadet Kardeşliğini Canlandırmak:** Namaz arkadaşlarının (eş, dost, kardeş veya akrabaların) birbirlerinin ibadet durumundan haberdar olup hayırda yarışmasını ve birbirlerine dua ile destek olmasını sağlamak (*«Mü'min erkekler ve mü'min kadınlar birbirlerinin velileridir; namazı dosdoğru kılarlar...»* - Tevbe, 71).
2. **Sadelik ve Odaklanma:** Karmaşık, dikkat dağıtıcı ve reklam dolu arayüzlerden tamamen arındırılmış; yalnızca ibadete odaklayan sade ve modern bir kullanıcı deneyimi sunmak.
3. **Kaza Namazlarını Bilinçle Eritmek:** Geçmiş kaza borçlarını kolayca hesaplayıp düzenli bir plana bağlayarak sıfırlama gayretini canlı tutmak.
4. **Manevi İstikrar (Karne):** Haftalık eda oranlarını grafik ve rozetlerle takip ederek kulun kendi nefsiyle tatlı bir muhasebe yapmasına vesile olmak.

---

## 📱 İndirme ve Resmi Kurulum

Uygulamayı Android telefonunuza doğrudan kurmak için resmi GitHub bağlantısını kullanabilirsiniz:

👉 **[📥 Namaz Arkadaşım v1.0 APK İndir (Resmi ve Doğrudan İndirme)](https://github.com/bilkil53/namaz-arkadasim/releases/latest/download/Namaz_Arkadasim.apk)**

* **Paket Kimliği (ApplicationId):** `com.app.namazarkadasim`
* **Sürüm Adı (VersionName):** `v1.0` (Resmi Kararlı Sürüm)
* **İç Derleme Kodu (VersionCode):** `5`
* **SHA-256 Doğrulama Özeti:** `dc7aebbfe57ff10899fe5e72136c084f4891079da5117ede1b11aad29186ecc2`

---

## ⚙️ Kapsamlı Çalışma Mantığı ve Modüller

### 1. 🕌 Diyanet Uyumlu Ezan Vakitleri & Akıllı Konum
* **Astronomik Hesaplama Algoritması:** Diyanet İşleri Başkanlığı'nın kullandığı açı ve parametreler (Fecr: 18°, İşa: 17°, Güneş doğuşu ve temkin payları) esas alınarak milisaniyelik hassasiyetle vakit hesaplaması yapılır.
* **Hibrit Konum Yönetimi:** Düşük pil tüketimli GPS (`FusedLocationProviderClient`) veya Türkiye'nin 81 ili arasından seçim imkânı sunar. İnternet kesilse dahi yerel kütüphane sayesinde vakitler kesintisiz çalışmaya devam eder.
* **Dinamik Geri Sayım:** Bir sonraki vakte kalan süreyi canlı sayaçla gösterir.

### 2. 📋 5 Vakit Namaz Takip Mekanizması
* **Basit İki Durumlu Model:** Sabah, Öğle, İkindi, Akşam ve Yatsı vakitleri için sade `Kıldım (Yeşil)` ve `Kılmadım (Gri/Kırmızı)` durumları.
* **Günün Özeti:** Gün içinde 5'te kaç vaktin eda edildiğini gösteren canlı ilerleme çubuğu.

### 3. 🤝 Akıllı Namaz Arkadaşı Senkronizasyonu
* **Eşleşme:** Her kullanıcı için benzersiz 4 haneli davet kodu oluşturulur (`ARK-XXXX`). Karşı taraf bu kodu girdiğinde namaz arkadaşları güvenli bir eşleşme kanalına bağlanır.
* **5 Saniye Akıllı Bekleme Süresi (Debounce):** Kullanıcı bir vakti kıldım veya kılmadım olarak işaretlediğinde, yanlışlıkla dokunma ihtimaline karşı sistem **5 saniye bekler**. Eğer kullanıcı fikrini değiştirirse önceki bildirim anında iptal edilir. Karar netleştiğinde arkadaşınıza tek ve kesin bildirim gider.
* **Çift Bildirim ve Metin Filtresi:** Karşı tarafa *"kıldı"* ve *"kılmadı"* bildirimlerinin aynı anda gitmesi engellenmiştir. Bildirimlerde kafa karışıklığı yaratacak *"kazaya bıraktı"* yerine net ve samimi bir ifade olan **"kılmadı"** metni gösterilir.
* **Karşılıklı Dua Gönderimi:** Arkadaşınıza tek dokunuşla hazır manevi tebrik ve dua iletebilme özelliği.

### 4. 📿 Gelişmiş Kaza Namazı Yönetimi
* **6 Vakit Kaza Sayacı:** Sabah, Öğle, İkindi, Akşam, Yatsı ve Vitir borçları ayrı ayrı tutulur.
* **Tek Tıkla Eda:** `Kıldım (-1)` butonuna basıldığında borç anında düşer, eda edilen sayı artar.
* **Otomatik Borç Hesaplama Sihirbazı:** Ergenlik başlangıç yaşı, namaza başlama yaşı veya kaç yıl/ay kaza borcu olduğunu otomatik gün/vakit hesabına döker.
* **Arkadaşımın Kaza Gayreti:** Kaza ekranında namaz arkadaşınızın da kıldığı kaza namazı sayıları canlı olarak gösterilir ve teşvik sağlar.

### 5. 🏆 Haftalık Karne & Manevi Rozetler
* **Haftalık Başarı Yüzdesi:** Son 7 günün 35 vaktinin oranını hesaplayan karne.
* **7 Günlük Vakit Dağılım Grafiği:** Gün gün hangi vakitlerin kılındığını görselleştiren bar grafiği.
* **Manevi Rozet Sistemi:**
  - 🌅 *Sabah Namazı Muhafızı* (Sabah namazlarını aksatmayanlara)
  - 🕋 *5'te 5 Sadakati* (Günü tam edayla bitirenlere)
  - ⚡ *Kaza Avcısı* (Kaza borçlarını düzenli eritenlere)
  - 🤝 *İbadet Kardeşliği* (Namaz arkadaşıyla düzenli takip yapanlara)
  - 🌟 *İstikrar Yıldızı* (Haftalık %80 üzeri başarı sağlayanlara)
* **Günün Sözü:** Her gün yenilenen sahih hadis ve ayet kartı.

### 6. 🔄 Dahili Güncelleme Sistemi (In-App Update)
* Uygulama her açıldığında ve Ayarlar menüsünden manuel olarak GitHub Releases API'sini sorgular.
* Yeni bir sürüm tespit edildiğinde sürüm notları ve indirme boyutuyla birlikte güncelleme diyaloğu açılır.
* Kullanıcı onayladığında APK **uygulama içerisinden doğrudan indirilerek** yerel paket yükleyicisine devredilir.
* Güncelleme sırasında hiçbir namaz verisi veya kaza kaydı kaybolmaz (Room SQLite güvenliği).

---

## 🏗️ Teknik Mimari ve Kullanılan Teknolojiler

```
com.app.namazarkadasim
├── data
│   ├── local        # Room Database (AppDatabase, DAOs, Entity sınıfları)
│   └── repository   # Single-Source-of-Truth veri katmanı (PrayerRepository)
├── ui
│   ├── components   # Yeniden kullanılabilir Compose bileşenleri (Kartlar, Diyaloglar)
│   ├── navigation   # Ekran yönlendirmeleri ve alt navigasyon çubuğu
│   ├── screens      # Ana ekranlar (Today, Partner, Kaza, Times, Report, Settings)
│   └── theme        # Material 3 renk paleti, tipografi ve tema tanımları
└── util             # Vakit motoru, Güncelleme yöneticisi, Konum & Bildirim yardımcıları
```

| Katman | Teknoloji | Açıklama |
| :--- | :--- | :--- |
| **Dil** | Kotlin (100%) | Modern, tip güvenli ve performanslı kod tabanı |
| **UI Framework** | Jetpack Compose + Material 3 | XML içermeyen modern, reaktif ve deklaratif arayüz |
| **Mimari Model** | MVVM (Model-View-ViewModel) | StateFlow ve Coroutines ile tek yönlü veri akışı (UDF) |
| **Veri Tabanı** | Room (SQLite) Persistence | Offline-first, cihaz içi şifrelenebilir yerel depolama |
| **Ağ İstemcisi** | OkHttp 3 | Hafif, düşük bellek tüketimli HTTP & JSON API istemcisi |
| **Vakit Motoru** | Yerleşik Astronomik Formüller | Güneş eğikliği ve koordinat bazlı Diyanet uyumlu hesaplama |
| **Konum** | Google Play Fused Location | Minimum batarya sarfiyatı sağlayan akıllı GPS |
| **Paket Yükleyici** | Android FileProvider | Güvenli dahili APK güncelleme dağıtımı |

---

## 🔒 Güvenlik, Gizlilik ve İzin Politikası

1. **Reklamsız ve Takipçisiz:** Uygulama içerisinde Google AdMob, Facebook SDK veya herhangi bir analitik takipçi **asla yer almaz**.
2. **Kişisel Veri Mahremiyeti:** Namaz kayıtlarınız, kaza borçlarınız ve kişisel notlarınız sadece cihazınızın dahili veritabanında saklanır. Hiçbir şirkete satılmaz veya aktarılmaz.
3. **Kullanılan İzinlerin Gerekçeleri:**
   - `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`: Yalnızca doğru ezan vakitlerini ve kıble yönünü tespit etmek için anlık kullanılır; arka planda konum takibi yapılmaz.
   - `POST_NOTIFICATIONS`: Vakit ezan hatırlatmaları ve namaz arkadaşınızın bildirimlerini size iletebilmek için gereklidir.
   - `REQUEST_INSTALL_PACKAGES`: Yeni bir sürüm çıktığında uygulama içerisinden güncelleme yapılabilmesini sağlar.

---

## 🛠️ Yerel Geliştirme ve Derleme

Projeyi yerel bilgisayarınızda derlemek için:

```bash
# 1. Depoyu klonlayın
git clone https://github.com/bilkil53/namaz-arkadasim.git
cd namaz-arkadasim

# 2. Debug APK derleyin
gradle :app:assembleDebug

# 3. Üretilen APK konumu:
# app/build/outputs/apk/debug/app-debug.apk
```

---

<p align="center">
  <b>Rabbimiz kıldığınız namazları, tuttuğunuz niyetleri ve ibadet ortaklığınızı dergâh-ı izzetinde kabul eylesin. 🤲</b>
</p>
