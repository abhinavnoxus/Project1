package com.nexora.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test: the app launches and the first screen composes without crashing.
 *
 * Runs on a device/emulator only. The CI environment has none, so this is not reported as
 * passing anywhere.
 */
@RunWith(AndroidJUnit4::class)
class AppLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstScreenRenders() {
        composeRule.waitForIdle()
        val nexoraNodes = composeRule.onAllNodesWithText("NEXORA").fetchSemanticsNodes()
        assertTrue("expected the NEXORA brand to be on screen", nexoraNodes.isNotEmpty())
    }
}
