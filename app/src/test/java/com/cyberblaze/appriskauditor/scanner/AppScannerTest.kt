package com.cyberblaze.appriskauditor.scanner

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.cyberblaze.appriskauditor.data.Constants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pure-JVM unit tests for the scanner package.
 *
 * No Robolectric, no Mockito — uses the [PermissionInfoSource] functional interface
 * as a seam and [fakePermissionInfo] to build stub objects without reflection pain.
 *
 * Run with: `./gradlew :app:testDebugUnitTest`
 */
class AppScannerTest {

    private val cache = HashMap<String, String>()

    @Before fun setUp() = cache.clear()

    // ── resolvePermLevel ──────────────────────────────────────────────────────

    @Test fun `dangerous permission maps to dangerous`() {
        assertEquals(Constants.LEVEL_DANGEROUS, resolvePermLevel(
            "android.permission.READ_SMS",
            fakeSource("android.permission.READ_SMS", PermissionInfo.PROTECTION_DANGEROUS, 0),
            cache))
    }

    @Test fun `normal permission maps to normal`() {
        assertEquals(Constants.LEVEL_NORMAL, resolvePermLevel(
            "android.permission.INTERNET",
            fakeSource("android.permission.INTERNET", PermissionInfo.PROTECTION_NORMAL, 0),
            cache))
    }

    @Test fun `signature permission maps to signature`() {
        assertEquals(Constants.LEVEL_SIGNATURE, resolvePermLevel(
            "android.permission.CHANGE_COMPONENT_ENABLED_STATE",
            fakeSource("android.permission.CHANGE_COMPONENT_ENABLED_STATE",
                PermissionInfo.PROTECTION_SIGNATURE, 0),
            cache))
    }

    @Test fun `APPOP flag overrides base level to special`() {
        assertEquals(Constants.LEVEL_SPECIAL, resolvePermLevel(
            "android.permission.PACKAGE_USAGE_STATS",
            fakeSource("android.permission.PACKAGE_USAGE_STATS",
                PermissionInfo.PROTECTION_NORMAL, PermissionInfo.PROTECTION_FLAG_APPOP),
            cache))
    }

    @Test fun `SYSTEM_ALERT_WINDOW is always special even without APPOP flag`() {
        assertEquals(Constants.LEVEL_SPECIAL, resolvePermLevel(
            "android.permission.SYSTEM_ALERT_WINDOW",
            fakeSource("android.permission.SYSTEM_ALERT_WINDOW",
                PermissionInfo.PROTECTION_NORMAL, 0),
            cache))
    }

    @Test fun `REQUEST_INSTALL_PACKAGES is special`() {
        assertEquals(Constants.LEVEL_SPECIAL, resolvePermLevel(
            "android.permission.REQUEST_INSTALL_PACKAGES",
            fakeSource("android.permission.REQUEST_INSTALL_PACKAGES",
                PermissionInfo.PROTECTION_NORMAL, 0),
            cache))
    }

    @Test fun `BIND_ACCESSIBILITY_SERVICE is special`() {
        assertEquals(Constants.LEVEL_SPECIAL, resolvePermLevel(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            fakeSource("android.permission.BIND_ACCESSIBILITY_SERVICE",
                PermissionInfo.PROTECTION_SIGNATURE, 0),
            cache))
    }

    @Test fun `NameNotFoundException defaults to normal`() {
        val notFound = PermissionInfoSource { throw PackageManager.NameNotFoundException("x") }
        assertEquals(Constants.LEVEL_NORMAL,
            resolvePermLevel("com.custom.PERM", notFound, cache))
    }

    @Test fun `level is cached — source called only once per permission`() {
        var calls = 0
        val src = PermissionInfoSource { calls++; fakePermissionInfo(PermissionInfo.PROTECTION_DANGEROUS, 0) }
        resolvePermLevel("android.permission.CAMERA", src, cache)
        resolvePermLevel("android.permission.CAMERA", src, cache)
        assertEquals(1, calls)
    }

    @Test fun `all SPECIAL_PERMISSION_NAMES resolve to special regardless of base`() {
        val src = PermissionInfoSource { fakePermissionInfo(PermissionInfo.PROTECTION_NORMAL, 0) }
        Constants.SPECIAL_PERMISSION_NAMES.forEach { name ->
            assertEquals("Expected SPECIAL for $name",
                Constants.LEVEL_SPECIAL,
                resolvePermLevel(name, src, HashMap()))
        }
    }

    // ── fakeApps ──────────────────────────────────────────────────────────────

    @Test fun `fakeApps returns at least 4 apps`() {
        assertTrue(AppScanner.fakeApps().size >= 4)
    }

    @Test fun `Torch Master is sideloaded with SMS permissions granted`() {
        val app = AppScanner.fakeApps().first { it.pkg == "com.fake.torch" }
        assertNull(app.installer)
        assertTrue(app.perms.any { it.name == "android.permission.READ_SMS"    && it.granted })
        assertTrue(app.perms.any { it.name == "android.permission.RECEIVE_SMS" && it.granted })
        assertTrue(app.perms.any { it.name == "android.permission.READ_CONTACTS" && it.granted })
    }

    @Test fun `QuickChat is from Play Store`() {
        val app = AppScanner.fakeApps().first { it.pkg == "com.fake.quickchat" }
        assertEquals(Constants.INSTALLER_PLAY_STORE, app.installer)
        assertTrue(app.perms.any { it.name == "android.permission.READ_CALL_LOG" && it.granted })
        assertTrue(app.perms.any { it.name == "android.permission.RECORD_AUDIO"  && it.granted })
    }

    @Test fun `Polite Notes has only INTERNET`() {
        val app = AppScanner.fakeApps().first { it.pkg == "com.fake.politenotes" }
        assertEquals(1, app.perms.size)
        assertEquals("android.permission.INTERNET", app.perms[0].name)
    }

    @Test fun `FakeCam Pro has declared-but-not-granted location permissions`() {
        val app = AppScanner.fakeApps().first { it.pkg == "com.fake.fakecampro" }
        val fine = app.perms.first { it.name == "android.permission.ACCESS_FINE_LOCATION" }
        assertFalse("fine location should NOT be granted", fine.granted)
        val bg = app.perms.first { it.name == "android.permission.ACCESS_BACKGROUND_LOCATION" }
        assertFalse("bg location should NOT be granted", bg.granted)
    }

    @Test fun `all four protection levels appear in fakeApps`() {
        val levels = AppScanner.fakeApps().flatMap { it.perms }.map { it.level }.toSet()
        assertTrue(Constants.LEVEL_NORMAL    in levels)
        assertTrue(Constants.LEVEL_DANGEROUS in levels)
        assertTrue(Constants.LEVEL_SIGNATURE in levels)
        assertTrue(Constants.LEVEL_SPECIAL   in levels)
    }

    @Test fun `fakeApps special field is empty at scan time`() {
        AppScanner.fakeApps().forEach { assertTrue(it.special.isEmpty()) }
    }

    // ── room mapping ─────────────────────────────────────────────────────────

    @Test fun `known permissions map to correct rooms`() {
        assertEquals(Constants.ROOM_MESSAGES,   Constants.PERMISSION_TO_ROOM["android.permission.READ_SMS"])
        assertEquals(Constants.ROOM_LOCATION,   Constants.PERMISSION_TO_ROOM["android.permission.ACCESS_FINE_LOCATION"])
        assertEquals(Constants.ROOM_MICROPHONE, Constants.PERMISSION_TO_ROOM["android.permission.RECORD_AUDIO"])
        assertEquals(Constants.ROOM_CAMERA,     Constants.PERMISSION_TO_ROOM["android.permission.CAMERA"])
        assertEquals(Constants.ROOM_CONTACTS,   Constants.PERMISSION_TO_ROOM["android.permission.READ_CONTACTS"])
        assertEquals(Constants.ROOM_PHONE,      Constants.PERMISSION_TO_ROOM["android.permission.READ_CALL_LOG"])
        assertEquals(Constants.ROOM_STORAGE,    Constants.PERMISSION_TO_ROOM["android.permission.READ_EXTERNAL_STORAGE"])
        assertEquals(Constants.ROOM_SENSORS,    Constants.PERMISSION_TO_ROOM["android.permission.BODY_SENSORS"])
    }

    @Test fun `INTERNET has no room`() {
        assertNull(Constants.PERMISSION_TO_ROOM["android.permission.INTERNET"])
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private fun fakeSource(name: String, base: Int, flags: Int) = PermissionInfoSource { requested ->
        if (requested == name) fakePermissionInfo(base, flags)
        else throw PackageManager.NameNotFoundException(requested)
    }
}
