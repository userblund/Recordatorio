package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.IntervalUnit
import com.example.data.RecurrenceType
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context matches app name`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Recordatorio", appName)
  }

  @Test
  fun `date utils computes trigger millis for year 2500 and 10000`() {
    val millis2500 = DateUtils.computeTriggerMillis(2500, 1, 1, 12, 0)
    assertTrue("Year 2500 should be far in the future", millis2500 > System.currentTimeMillis())

    val millis10000 = DateUtils.computeTriggerMillis(10000, 1, 1, 0, 0)
    assertTrue("Year 10000 should be far in the future", millis10000 > millis2500)

    val isFar = DateUtils.isFarFuture(2500)
    assertTrue("Year 2500 is far future", isFar)
  }

  @Test
  fun `generic recurrence supports arbitrary values and units`() {
    val next = DateUtils.computeNextOccurrence(
      currentYear = 2026,
      currentMonth = 10,
      currentDay = 1,
      hour = 8,
      minute = 30,
      recurrenceType = RecurrenceType.GENERIC_INTERVAL,
      intervalValue = 2,
      intervalUnit = IntervalUnit.WEEK
    )
    assertEquals(15, next.day)
    assertEquals(10, next.month)
    assertEquals(8, next.hour)
    assertEquals(30, next.minute)

    val century = DateUtils.computeNextOccurrence(
      currentYear = 2026,
      currentMonth = 10,
      currentDay = 1,
      hour = 8,
      minute = 30,
      recurrenceType = RecurrenceType.GENERIC_INTERVAL,
      intervalValue = 2,
      intervalUnit = IntervalUnit.CENTURY
    )
    assertEquals(2226, century.year)
  }

  @Test
  fun `daily recurrence calculates next occurrence`() {
    val next = DateUtils.computeNextOccurrence(
      currentYear = 2026,
      currentMonth = 10,
      currentDay = 1,
      hour = 8,
      minute = 30,
      recurrenceType = RecurrenceType.DAILY
    )
    assertEquals(2, next.day)
    assertEquals(8, next.hour)
    assertEquals(30, next.minute)
  }
}
