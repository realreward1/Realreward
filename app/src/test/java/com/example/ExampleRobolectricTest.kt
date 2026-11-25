package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createEmptyComposeRule()

  @Before
  fun setUpFirebase() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    if (FirebaseApp.getApps(ctx).isEmpty()) {
      FirebaseApp.initializeApp(
        ctx,
        FirebaseOptions.Builder()
          .setApplicationId("1:159999829657:android:76a8f86eb0e7152397ba08")
          .setApiKey("AIzaSyAlJw3bqPQbQewk7Rw0FNL12YVVymEX3X0")
          .setProjectId("gen-lang-client-0351943250")
          .build()
      )
    }
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Real Reward", appName)
  }

  @Test
  fun `verify fonts and drawable sizes`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val f1 = ResourcesCompat.getFont(context, R.font.hind_siliguri)
    val f2 = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
    assertNotNull("hind_siliguri font failed", f1)
    assertNotNull("plus_jakarta_sans font failed", f2)

    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(context.resources, R.drawable.ic_brand_emblem_1795531052528, opts)
    assertTrue("emblem width invalid: ${opts.outWidth}", opts.outWidth in 1..4096)
    BitmapFactory.decodeResource(context.resources, R.drawable.img_hero_fintech_1795531052758, opts)
    assertTrue("hero width invalid: ${opts.outWidth}", opts.outWidth in 1..4096)
  }

  @Test
  fun `main activity launches without crashing`() {
    ActivityScenario.launch(MainActivity::class.java).use {
      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("google_sign_in_button").assertExists()
    }
  }
}
