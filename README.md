# 👨‍👩‍👧 Aile ile Secdeye (v1.0 - Resmi Kararlı Sürüm)

<p align="center">
  <b>Dijital Namaz Takip Sistemi: Çekirdek ve Geniş Aileler İçin Ortak İbadet Havuzu, Canlı Aile Durum Paneli, Mahremiyet Korumalı Kaza Takibi ve Manevi İstikrar</b>
</p>

<p align="center">
  <a href="https://github.com/bilkil53/aile-ile-secdeye/releases/latest/download/Ailece_Secde.apk">
    <img src="https://img.shields.io/badge/İndir-Aile%20ile%20Secdeye%20v1.0%20(APK)-00897B?style=for-the-badge&logo=android&logoColor=white" alt="APK İndir" />
  </a>
  <img src="https://img.shields.io/badge/Sürüm-v1.0%20(Derleme%201)-0D47A1?style=for-the-badge" alt="Sürüm" />
  <img src="https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Mimari-MVVM%20%7C%20Jetpack%20Compose-purple?style=for-the-badge" alt="Mimari" />
  <img src="https://img.shields.io/badge/Gizlilik-%25100%20Mahrem%20%26%20Yerel%20Veritabanı-brightgreen?style=for-the-badge" alt="Gizlilik" />
</p>

> *"Ailene namazı emret ve onda sabırlı ol."* (Tâhâ Suresi, 132)

---

## 📱 İndirme ve Kurulum Bağlantıları

Uygulamayı Android cihazınıza doğrudan indirmek ve kurmak için:

👉 **[📥 Aile ile Secdeye v1.0 APK İndir (Resmi ve Doğrudan)](https://github.com/bilkil53/aile-ile-secdeye/releases/latest/download/Ailece_Secde.apk)**

* **Alternatif / GitHub Release:** [https://github.com/bilkil53/aile-ile-secdeye/releases](https://github.com/bilkil53/aile-ile-secdeye/releases)
* **Paket Kimliği (ApplicationId):** `com.app.namazarkadasim` (veya `com.aistudio.aileilesecdeye`)
* **Sürüm Kodu:** `1` (v1.0)
* **Minimum Android:** Android 7.0 (API 24+)

---

## 🌟 Öne Çıkan Özellikler ve Kullanım Detayları

### 1. 📊 Canlı Aile Durum Paneli (Çift Yönlü Doğrulama)
* **Gerçek Zamanlı Bağlantı Teyidi:** Kullanıcıların aktif havuzda olup olmadığını (`🟢 Çift Taraflı Bağlantı Aktif` / `⏳ Doğrulama Bekleniyor`) gösterir.
* **"Bağlantıyı Teyit Et 🔄" Butonu:** Karşı taraf uygulamayı silip yeniden kursa bile tek tıkla canlı bağlantıyı yeniden teyit eder ve günceller.
* **Davet Kodu Yönetimi:** Tek tıkla `Kodu Kopyala`, `WhatsApp/Paylaş` ve `Mail ile Davet Et`.

### 2. 👨‍👩‍👧‍👦 Geniş Aile Havuzu (1. Kişi Merkezli)
* **Kişi Rolleri ve İkonlar:** "Eş/Çocuk" gibi kalıp ifadeler yerine isteğe göre seçilebilen cinsiyet ve profil ikonları (👨, 👩, 👦, 👧).
* **2., 3., 4. Kişi Desteği:** Anne, baba, çocuklar veya evin diğer fertleri aynı aile kodunu girerek ortak ibadet havuzuna dahil olabilir.

### 3. 🔒 Mahremiyet Korumalı (Detaysız) Aile Kaza Özeti
* **Kul Hakkı & İbadet Mahremiyeti:** Kimsenin hangi vakti kaza ettiği detaylı olarak başkalarına gösterilmez.
* **Yalnızca Toplamlar:** Yalnızca o kişinin toplam kaza borcu ve erittiği kaza adedi özet chip olarak panoda yer alır.
* **Hızlı Teşvik:** Aile fertleri için hızlı `"+1 Kaza Kıldı"` butonu.

### 4. 📅 Dünün Aile Tablosu
* Dünün namaz durumunu görmek için tarih seçici üzerinden "Dün" seçilebilir, ailenin dünkü başarı karnesi (5/5 rozetleri) incelenebilir.

### 5. 🔄 2 Kişilik Sürümden (Namaz Arkadaşım) Kolay Geçiş
* Evlilikten çocuklu geniş aileye geçen kullanıcılar için **Ayarlar ➔ 'Namaz Arkadaşım Verilerini İçe Aktar'** sihirbazı.
* Geçmiş tüm kaza ve streak verileri tek tıkla kayıpsız olarak Aile ile Secdeye havuzuna aktarılır.

---

## 🛠️ Kurulum (Geliştiriciler İçin)

```bash
# Repoyu klonlayın
git clone https://github.com/bilkil53/aile-ile-secdeye.git
cd aile-ile-secdeye

# Projeyi derleyin
./gradlew assembleRelease
```
Derlenen APK dosyası `app/build/outputs/apk/release/app-release.apk` dizininde oluşturulur.
