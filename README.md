# 🕌 Namaz Arkadaşım (v.1 - Resmi Başlangıç Sürümü)

<p align="center">
  <b>Türkiye'nin En Kapsamlı ve Maneviyat Dolu Namaz, Kaza Takip, Karne ve İbadet Kardeşliği Uygulaması</b>
</p>

<p align="center">
  <a href="https://github.com/bilkil53/namaz-arkadasim/releases/latest/download/Namaz_Arkadasim.apk">
    <img src="https://img.shields.io/badge/İndir-Namaz%20Arkadaşım%20APK%20(v.1)-0F5132?style=for-the-badge&logo=android&logoColor=white" alt="APK İndir" />
  </a>
  <img src="https://img.shields.io/badge/Sürüm-v.1%20(Resmi%20Sürüm)-15803D?style=for-the-badge" alt="Sürüm" />
  <img src="https://img.shields.io/badge/Platform-Android%2014+%20(API%2024+)-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform" />
  <img src="https://img.shields.io/badge/Mimari-Jetpack%20Compose%20%7C%20M3-005C53?style=for-the-badge" alt="Mimari" />
  <img src="https://img.shields.io/badge/Güvenlik-Reklamsız%20%7C%20Sıfır%20Takipçi-10B981?style=for-the-badge" alt="Güvenlik" />
</p>

---

## 📥 Resmi Kurulum ve İndirme

Uygulamanın resmi başlangıç kararlı paketini (`v.1`) doğrudan Android cihazınıza kurmak için aşağıdaki güvenli GitHub bağlantısını kullanabilirsiniz:

👉 **[📥 Namaz Arkadaşım v.1 APK İndir (Resmi GitHub Dağıtımı)](https://github.com/bilkil53/namaz-arkadasim/releases/latest/download/Namaz_Arkadasim.apk)**

> 🛡️ **Güvenlik Notu:** Bu bağlantı Microsoft GitHub sunucuları üzerinden doğrudan, reklamsız ve uçtan uca SSL şifrelemeli olarak indirilir. Üçüncü taraf aracı veya link kısaltıcı siteler kesinlikle kullanılmamaktadır.

---

## 📖 Uygulamanın Amacı ve Vizyonu

**Namaz Arkadaşım**, Müslümanların günlük namaz ibadetlerini aksatmadan, huzur ve istikrarla eda etmelerini sağlamak; kaza namazı borçlarını disiplinli bir şekilde eritmek ve eşler / namaz kardeşleri arasında manevi bağı ve karşılıklı motivasyonu artırmak amacıyla geliştirilmiş modern bir Android uygulamasıdır.

---

## ⚙️ Uygulamanın Temel Çalışma Mantığı ve Mimarisi

Namaz Arkadaşım, Google tarafından önerilen en güncel Android modern mimari standartlarına (**MVVM + Clean Architecture**) göre inşa edilmiştir:

```
┌─────────────────────────────────────────────────────────────┐
│                 Jetpack Compose UI (M3)                    │
│   (TrackerScreen, ReportScreen, KazaScreen, PartnerScreen)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow & UIEvents
┌──────────────────────────────▼──────────────────────────────┐
│                       MainViewModel                         │
│       İbadet durumu, Eş senkronizasyonu, GPS, Vakitler       │
└──────────────┬───────────────────────────────┬──────────────┘
               │                               │
┌──────────────▼─────────────┐   ┌─────────────▼──────────────┐
│     Room SQLite Database   │   │     Diyanet & Sync Hub     │
│  (DailyPrayer, Kaza, Badges│   │ (GPS Geocoder, Firebase,   │
│       & Local Settings)    │   │  GitHub In-App Update API) │
└────────────────────────────┘   └────────────────────────────┘
```

---

### 1. 🕌 Namaz Vakitleri & Otomatik GPS Hesaplama Mantığı
- **GPS ile Yer Tespiti:** Uygulama açıldığında cihazın GPS modülü üzerinden kullanıcının enlem, boylam ve il/ilçe bilgisi (Geocoder) otomatik olarak çözümlenir.
- **T.C. Diyanet İşleri Başkanlığı Algoritması:** Astronomik güneş açıları ve Diyanet'in belirlemiş olduğu temkin süreleri (İmsak için Güneş'in ufkun 18° altına inmesi, Yatsı için 17° altına inmesi vb.) baz alınarak milisaniyelik hassasiyette 5 vakit ve kerahat vakitleri hesaplanır.
- **İnternetsiz (Çevrimdışı) Çalışma:** İlk tespitten sonra veya internet bağlantısı olmadığında dahi uygulama tamamen çevrimdışı olarak vakitleri kusursuz hesaplamaya devam eder.
- **Canlı Geri Sayım ve "Sıradaki Vakit" Kartı:** Ana ekrandaki zümrüt çam yeşili kart; sıradaki vakti, kalan saat ve dakikayı, o anki vakit aralığını ve ezan vaktini dinamik olarak gösterir.

---

### 2. 📋 Günlük 5 Vakit Takip Sistemi Mantığı
- **Sade ve Odaklı Yapı:** *"Dün, bugün, yarın"* gibi kafa karıştıran geçmiş sekmeler yerine, ekran doğrudan **Bugünün 5 Vaktine** odaklanır.
- **Durum Döngüsü:** Her vakit kutucuğuna tek dokunuşla durumlar sırasıyla değişir:
  - 🟢 **Kıldı (PRAYED):** Vaktin eda edildiğini belirtir, yeşil çubuk dolumu sağlar ve haftalık karneye 1 puan ekler.
  - 🔴 **Kılmadı (MISSED):** Vaktin kaçırıldığını belirtir; otomatik olarak kaza borcu hanesine ekleme önerir.
  - 🟣 **Muaf (EXCUSED):** Özel durumlarda ibadetten muafiyeti işaretler, karne ortalamasını düşürmez.
  - ⚪ **Bekliyor (WAITING):** Henüz vakti gelmemiş veya işaretlenmemiş durumu simgeler.
- **5 Segmentli İlerleme Çubuğu:** Gün içinde kılınan her namaz ile üstteki 5 bölümlü gösterge (örneğin `3/5`) dolup parlar.

---

### 3. 🤝 Eş / Namaz Arkadaşı Canlı Senkronizasyon Mantığı
- **Google Hesabı Entegrasyonu:** Kullanıcı Google hesabıyla tek tıkla güvenli profil oluşturur. Profil kartı uygulamanın yeşil tasarım diliyle uyumludur.
- **6 Haneli Benzersiz Davet Kodu:** Her kullanıcıya sistem tarafından özel bir eşleşme kodu üretilir. Bu kod WhatsApp, SMS veya sosyal medyadan tek dokunuşla paylaşılır.
- **Anlık İbadet Paylaşımı (Cloud Sync):**
  - Eşlerden biri bir namazı kıldığında veya kaza namazı eda ettiğinde, bu veri bulut üzerinden eşine anında iletilir.
  - Diğer eşin telefonuna *"Eşiniz Sabah namazını kıldı, Allah kabul etsin!"* şeklinde tebrik ve motivasyon bildirimi düşer.
  - Eşler birbirine tek dokunuşla manevi dualar gönderebilir.
- **Eşimin Kaza Durumu:** Kaza ekranında yalnızca kendi borcunuzu değil, eşinizin de kıldığı kaza sayılarını ve gayretini canlı görebilirsiniz.

---

### 4. 📿 Gelişmiş Kaza Namazı Takip ve Hesaplama Mantığı
- **Borç Hesaplama Sihirbazı:** Kaç yıl veya kaç ay kaza borcu olduğunu bilmeyenler için ergenlik yaşı ve geçen süreyi baz alarak toplam kaza borcunu otomatik hesaplar.
- **Ayrı Ayrı 6 Vakit Sayacı:** Sabah, Öğle, İkindi, Akşam, Yatsı ve Vitir namazları için bağımsız sayıcılar.
- **Tek Dokunuşla Azaltma (-1):** Kılınan her kaza namazında `-1` butonuna basıldığında borç anında düşer, kılınan kaza istatistiği artar ve veritabanına işlenir.

---

### 5. 📊 Haftalık Manevi Karne ve 7 Günlük Dağılım Grafiği
- **Son 7 Günün İstatistiği:** Cihazın yerel Room veritabanından son 7 günün namaz kayıtları çekilir.
- **Vakit Dağılım Çubuk Grafiği:** Pazartesi'den Pazar'a her günün 5 vakit üzerinden başarı sütunu görselleştirilir.
- **Haftalık Başarı Yüzdesi:** Haftalık toplam 35 vakit üzerinden tamamlama yüzdesi hesaplanır (Örn: `%94 Başarı Oranı`).
- **Manevi Başarı Rozetleri:**
  - 🌅 *Sabah Namazı Muhafızı* (Sabah namazlarını aksatmayanlar için)
  - 🕋 *5'te 5 Sadakati* (Günü firesiz tamamlayanlar için)
  - ⚡ *Kaza Avcısı* (Kaza borçlarını düzenli eritenler için)
  - 🤝 *İbadet Kardeşliği* (Eşiyle ortak namaz kılanlar için)
  - 🌟 *İstikrar Yıldızı* (Haftalık %80 üzeri başarı sağlayanlar için)
- **Günün Ayet ve Hadis-i Şerifi:** Her güne özel sahih hadis ve Kur'an ayeti ile manevi motivasyon.

---

### 6. 🔄 Dahili Otomatik Güncelleme (In-App Update) Mantığı
- **GitHub Releases API Denetimi:** Uygulama internete her bağlandığında GitHub üzerindeki en son yayınlanan sürüm etiketini arka planda sorgular.
- **Sürüm Karşılaştırma:** Mevcut sürüm (`v.1`) ile sunucudaki sürüm (örneğin ileride çıkacak `v.2`) SemVer algoritmasıyla karşılaştırılır.
- **Tek Tıkla Güncelleme Penceresi:** Yeni sürüm algılandığında kullanıcıya sürüm notları, yenilikler ve dosya boyutu gösterilir. *"Hemen Güncelle"* dendiğinde APK doğrudan indirilerek Android paket yükleyicisine aktarılır. Kullanıcının web tarayıcısına gitmesine gerek kalmaz.

---

## 🔒 Güvenlik, Gizlilik ve İzin Politikası

| İzin Adı | Kullanım Amacı | Zorunlu mu? |
| :--- | :--- | :--- |
| `ACCESS_FINE_LOCATION` | Bulunduğunuz yerin enlem/boylamına göre kesin Diyanet vakitlerini hesaplamak için. | Opsiyonel (Manuel şehir de seçilebilir) |
| `POST_NOTIFICATIONS` | Ezan vakti hatırlatması ve eşiniz namaz kıldığında tebrik bildirimi almak için. | Opsiyonel |
| `INTERNET` | Yalnızca eşler arası ibadet senkronizasyonu ve güncelleme kontrolü için. | Opsiyonel (Çevrimdışı çalışır) |

- ❌ **Reklam Yoktur:** İbadet esnasında dikkati dağıtacak hiçbir reklam veya ticari banner bulunmaz.
- ❌ **Veri Satışı Yoktur:** Bilgileriniz üçüncü şahıslara veya reklam ağlarına kesinlikle satılmaz.
- 🛡️ **Yerel SQLite Koruması:** Namaz kayıtlarınız telefonun dahili korumalı Room veritabanında saklanır.

---

## 🛠️ Kullanılan Teknolojiler

- **Programlama Dili:** Kotlin 2.0+
- **Kullanıcı Arayüzü:** Jetpack Compose (Material Design 3)
- **Veritabanı:** Room SQLite (AndroidX Room)
- **Eşzamansız İşlemler:** Kotlin Coroutines & StateFlow / SharedFlow
- **Ağ İstemcisi:** OkHttp 4
- **Harita & Konum:** Android Geocoder & LocationManager API
- **Sürüm Kontrolü & Dağıtım:** Git & GitHub Actions / Releases API

---

## 📱 Sürüm Geçmişi

- **v.1 (Resmi Başlangıç Sürümü):**
  - Sıfırdan resmi kararlı başlangıç sürümü.
  - Zümrüt çam yeşili tema ve profil kartı uyumu.
  - Diyanet resmi ezan vakitleri ve GPS konumlandırma.
  - 5 vakit sade takip ekranı.
  - Son 7 günün Karne başarı grafiği ve rozetler.
  - Kaza namazı takip ve hesaplama sihirbazı.
  - 6 haneli kodla eş/namaz arkadaşı canlı eşleşmesi.
  - GitHub In-App otomatik güncelleme entegrasyonu.

---

<p align="center">
  <b>Geliştirici:</b> bilkil53 · <b>İletişim & Destek:</b> e.bilkil5391@gmail.com<br>
  <i>"Namaz dinin direğidir." — Hadis-i Şerif</i><br>
  <b>Dualarınızda yer almak dileğiyle. Allah kabul etsin. 🤲</b>
</p>
