package io.github.sourcem7.alfajralarm

import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class ReleaseSafetyInstrumentedTest {
    // Method names must not contain spaces: androidTest methods are dexed and
    // D8 rejects spaces in method names below DEX version 040 (minSdk 26).
    @Test
    fun applicationIdMatchesTheConfiguredPackage() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.sourcem7.alfajralarm", appContext.packageName)
    }

    @Test
    fun releasePackageRequestsNoInternetLocationOrStoragePermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val requestedPermissions = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        ).requestedPermissions.orEmpty()

        val prohibited = setOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
        )

        assertTrue(requestedPermissions.intersect(prohibited).isEmpty())
    }

    @Test
    fun backupRulesExcludeDeviceLocalAlarmState() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val expectedPath = "datastore/alarm_state.preferences_pb"

        assertEquals(
            listOf(expectedPath),
            excludedPaths(resources.getXml(R.xml.backup_rules)),
        )
        assertEquals(
            listOf(expectedPath, expectedPath),
            excludedPaths(resources.getXml(R.xml.data_extraction_rules)),
        )
    }

    @Test
    fun mainActivitySurvivesProcessConfigurationRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
            }
        }
    }

    private fun excludedPaths(parser: XmlResourceParser): List<String> = try {
        buildList {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "exclude") {
                    parser.getAttributeValue(null, "path")?.let(::add)
                }
            }
        }
    } finally {
        parser.close()
    }
}
