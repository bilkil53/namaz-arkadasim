package com.example.data.util

import java.util.Calendar

data class DailyQuote(
    val category: String,
    val title: String,
    val text: String,
    val arabic: String? = null,
    val source: String,
    val isDiyanet: Boolean = false
)

data class SpiritualGreeting(
    val id: String,
    val title: String,
    val category: String, // "Tebrik", "Dua", "İstikrar", "Kaza"
    val iconEmoji: String,
    val text: String
)

object DailyContentProvider {

    val STATIC_GREETINGS_AND_DUAS = listOf(
        SpiritualGreeting(
            id = "greet_1",
            title = "Vakit Namazı Tebriki",
            category = "Tebrik",
            iconEmoji = "🌟",
            text = "Allah kabul etsin, kıldığın namazlar kalbine nur, ömrüne huzur ve bereket olsun kardeşim."
        ),
        SpiritualGreeting(
            id = "greet_2",
            title = "Samimi Kabul Duası",
            category = "Dua",
            iconEmoji = "🤲",
            text = "Rabbim kıldığın namazları, ettiğin duaları dergâh-ı izzetinde en güzel şekilde makbul eylesin."
        ),
        SpiritualGreeting(
            id = "greet_3",
            title = "İstikrar ve Gayret Takdiri",
            category = "İstikrar",
            iconEmoji = "🌿",
            text = "Namaz dinin direğidir. Birlikte hayırda yarışarak istikrarımızı koruduğumuz için Elhamdülillah!"
        ),
        SpiritualGreeting(
            id = "greet_4",
            title = "Hayırlı ve Huzurlu Gün",
            category = "Dua",
            iconEmoji = "🕊️",
            text = "Rabbim sana ve ailene afiyet, kalbine inşirah, hanene bereket ihsan eylesin."
        ),
        SpiritualGreeting(
            id = "greet_5",
            title = "Kaza Namazı Teşviki",
            category = "Kaza",
            iconEmoji = "📿",
            text = "Kaza namazlarını eritmeye devam; Rabbim azmini, gayretini ve sevabını kat kat artırsın."
        ),
        SpiritualGreeting(
            id = "greet_6",
            title = "Günün Tamamlanma Şükrü",
            category = "Tebrik",
            iconEmoji = "🌙",
            text = "Günün farzlarını tamamlayarak huzurla geceye kavuşturan Yüce Allah'a hamdolsun."
        )
    )

    private val AYETLER = listOf(
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Namazın Önemi",
            arabic = "إِنَّ الصَّلَاةَ كَانَتْ عَلَى الْمُؤْمِنِينَ كِتَابًا مَوْقُوتًا",
            text = "Şüphesiz namaz, mü’minler üzerine belirli vakitlerde farz kılınmıştır.",
            source = "Nisâ Sûresi, 103. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Huşû ve Kurtuluş",
            arabic = "قَدْ أَفْلَحَ الْمُؤْمِنُونَ ﴿١﴾ الَّذِينَ هُمْ فِي صَلَاتِهِمْ خَاشِعُونَ",
            text = "Müminler gerçekten kurtuluşa ermişlerdir; onlar ki, namazlarında derin bir saygı ve huşû içindedirler.",
            source = "Mü'minûn Sûresi, 1-2. Âyetler (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Sabır ve Namazla Yardım",
            arabic = "وَاسْتَعِينُوا بِالصَّبْرِ وَالصَّلَاةِ",
            text = "Sabır ve namaz ile Allah'tan yardım isteyin. Şüphesiz o, huşû duyanlardan başkasına pek ağır gelir.",
            source = "Bakara Sûresi, 45. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        ),
        DailyQuote(
            category = "Günün Ayeti · Diyanet Meali",
            title = "Kötülükten Alıkoyan Namaz",
            arabic = "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنْكَرِ",
            text = "Şüphesiz namaz, insanı hayasızlıktan ve her türlü kötülükten alıkoyar.",
            source = "Ankebût Sûresi, 45. Âyet (Diyanet İşleri Başkanlığı Meali)",
            isDiyanet = true
        )
    )

    private val DUALAR = listOf(
        DailyQuote(
            category = "Günün Duası · Özgün",
            title = "İbadette Süreklilik Duası",
            arabic = "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ",
            text = "Ey Rabbim! Beni ve neslimi namazı dosdoğru ve devamlı kılanlardan eyle. Ey Rabbimiz, dualarımızı kabul buyur!",
            source = "İbrâhim Sûresi, 40. Âyet Meali / Manevi Dua",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Günün Duası · Özgün",
            title = "Gönül Huzuru ve Doğruluk",
            arabic = "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            text = "Ey kalpleri halden hale çeviren Allah'ım! Kalbimizi rızana, ibadetine ve dinine sabit kıl. Bizi namazla huzur bulanlardan eyle.",
            source = "Tirmizî / Manevi Dua ve İltica",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Günün Duası · Özgün",
            title = "Tövbe ve Kaza Azmi",
            text = "Allah'ım! Geçmişte kaçırdığımız, kusurlu kıldığımız namazlarımızı bağışla. Kaza borçlarımızı şevkle ve ihlasla tamamlamayı bizlere kolaylaştır.",
            source = "Özgün Manevi Niyaz",
            isDiyanet = false
        )
    )

    private val MOTIVASYONLAR = listOf(
        DailyQuote(
            category = "Bugünün Motivasyon Sözü · Özgün",
            title = "Namaz Vakti Bir Fırsattır",
            text = "Her ezan, dünyanın telaşından sıyrılıp Yaratıcının huzuruna çıkmak için sana sunulan eşsiz bir randevudur. Bugün vakitlerini erteleme.",
            source = "Namaz Arkadaşım Özgün Motivasyon",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Bugünün Motivasyon Sözü · Özgün",
            title = "Birlikte Daha Güçlüyüz",
            text = "Bir dostun 'Namazını kıldın mı?' sorusu, cennet yolunda birbirine uzatılan en samimi eldir. Arkadaşını tebrik etmeyi unutma!",
            source = "Namaz Arkadaşım Özgün Motivasyon",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Bugünün Motivasyon Sözü · Özgün",
            title = "Kaza Borcu Dağ Değildir",
            text = "Kılınan her bir kaza namazı, ruhun üzerindeki bir yükü hafifletir. Küçük adımların istikrarı, büyük engelleri aşmanın en kutlu yoludur.",
            source = "Namaz Arkadaşım Özgün Motivasyon",
            isDiyanet = false
        )
    )

    private val OZLU_SOZLER = listOf(
        DailyQuote(
            category = "Günün Özlü Sözü · Özgün",
            title = "Secdenin Kıymeti",
            text = "Secde, yere fısıldayıp göklerin ötesinden işitildiğin en yakın ve en samimi makamdır.",
            source = "Gönül Dünyasından Hikmet Damlaları",
            isDiyanet = false
        ),
        DailyQuote(
            category = "Günün Özlü Sözü · Özgün",
            title = "Vaktin Bereketi",
            text = "Zamanın efendisi olmak istiyorsan, vaktini namazla tanzim et; çünkü namaz ömrün nizamıdır.",
            source = "İslami Tefekkür ve Hikmet",
            isDiyanet = false
        )
    )

    fun getDailyQuotes(calendar: Calendar = Calendar.getInstance()): List<DailyQuote> {
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val ayet = AYETLER[day % AYETLER.size]
        val dua = DUALAR[day % DUALAR.size]
        val motivasyon = MOTIVASYONLAR[day % MOTIVASYONLAR.size]
        val ozluSoz = OZLU_SOZLER[day % OZLU_SOZLER.size]
        return listOf(ayet, dua, motivasyon, ozluSoz)
    }

    fun getRandomRotatingNotificationQuote(customDay: Int? = null): DailyQuote {
        val cal = Calendar.getInstance()
        val day = customDay ?: cal.get(Calendar.DAY_OF_YEAR)
        val categoryCycle = day % 4
        return when (categoryCycle) {
            0 -> {
                val q = AYETLER.random()
                q.copy(category = "${((day - 1) % 30) + 1}. Gün: Günün Ayeti · Diyanet Meali")
            }
            1 -> {
                val q = DUALAR.random()
                q.copy(category = "${((day - 1) % 30) + 1}. Gün: Günün Duası · Manevi Niyaz")
            }
            2 -> {
                val q = MOTIVASYONLAR.random()
                q.copy(category = "${((day - 1) % 30) + 1}. Gün: Bugünün Motivasyon Sözü")
            }
            else -> {
                val q = OZLU_SOZLER.random()
                q.copy(category = "${((day - 1) % 30) + 1}. Gün: Günün Özlü Sözü · Hikmet Damlası")
            }
        }
    }
}
