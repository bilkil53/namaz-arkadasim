package com.example

import com.example.data.util.DailyContentProvider
import com.example.data.util.PrayerCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testPrayerCalculator_returnsValidTimes() {
    val cal = Calendar.getInstance()
    val times = PrayerCalculator.calculatePrayerTimes(41.0082, 28.9784, cal)
    assertNotNull(times.imsak)
    assertNotNull(times.ogle)
    assertNotNull(times.ikindi)
    assertNotNull(times.aksam)
    assertNotNull(times.yatsi)
    assertTrue(times.imsak.contains(":"))
    assertTrue(times.ogle.contains(":"))
  }

  @Test
  fun testQiblaCalculator_returnsValidDegrees() {
    val qibla = PrayerCalculator.calculateQiblaBearing(41.0082, 28.9784)
    // Istanbul Qibla is approximately 150-155 degrees
    assertTrue("Qibla should be between 140 and 165 for Istanbul", qibla in 140.0..165.0)
  }

  @Test
  fun testVersionComparison_sameVersionDoesNotUpdate() {
    val method = com.example.util.AppUpdateManager::class.java.getDeclaredMethod("isTagHigher", String::class.java, String::class.java)
    method.isAccessible = true
    val result1 = method.invoke(com.example.util.AppUpdateManager, "v1.0", "v1.0") as Boolean
    assertFalse("v1.0 should NOT be higher than v1.0", result1)

    val result2 = method.invoke(com.example.util.AppUpdateManager, "1.0", "1.0") as Boolean
    assertFalse("1.0 should NOT be higher than 1.0", result2)

    val result3 = method.invoke(com.example.util.AppUpdateManager, "v1.1", "v1.0") as Boolean
    assertTrue("v1.1 SHOULD be higher than v1.0", result3)
  }

  @Test
  fun testDailyContentProvider_providesQuotes() {
    val quotes = DailyContentProvider.getDailyQuotes()
    assertEquals(4, quotes.size)
    assertTrue(quotes.any { it.isDiyanet })
    assertTrue(quotes.any { it.category.contains("Ayet") })
    assertTrue(quotes.any { it.category.contains("Dua") })

    val day1 = DailyContentProvider.getRandomRotatingNotificationQuote(1)
    val day2 = DailyContentProvider.getRandomRotatingNotificationQuote(2)
    assertNotNull(day1.text)
    assertNotNull(day2.text)
    assertTrue(day1.category.contains("1. Gün"))
    assertTrue(day2.category.contains("2. Gün"))

    val staticGreetings = DailyContentProvider.STATIC_GREETINGS_AND_DUAS
    assertTrue(staticGreetings.isNotEmpty())
    assertTrue(staticGreetings.any { it.category == "Tebrik" })
    assertTrue(staticGreetings.any { it.category == "Dua" })
  }
}

