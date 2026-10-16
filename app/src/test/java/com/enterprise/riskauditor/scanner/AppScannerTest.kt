package com.enterprise.riskauditor.scanner

import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.enterprise.riskauditor.contract.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the pure logic in the scanner package.
 *
 * No Android runtime, no Robolectric. Tests use [PermissionInfoSource] fakes
 * and call the top-level [resolvePermLevel] function directly.
 * [fakeApps] tests need no Android at all — pure Kotlin data.
 *
 * Run with: `./gradlew :app:test`
 */
class AppScannerTest {

    private val cache = HashMap<String, String>()

    @Before
    fun setUp() {
        cache.clear()
    }

    // ── resolvePermLevel via PermissionInfoSource ─────────────────────────────

    @Test
    fun `dangerous permission maps to dangerous`() {
        val source = fakeSource("android.permission.READ_SMS",
            base = PermissionInfo.PROTECTION_DANGEROUS, flags = 0)
        assertEquals(Constants.LEVEL_DANGEROUS,
            resolvePermLevel("android.permission.READ_SMS", source, cache))
    }

    @Test
    fun `normal permission maps to normal`() {
        val source = fakeSource("android.permission.INTERNET",
            base = PermissionInfo.PROTECTION_NORMAL, flags = 0)
        assertEquals(Constants.LEVEL_NORMAL,
            resolvePermLevel("android.permission.INTERNET", source, cache))
    }

    @Test
    fun `signature permission maps to signature`() {
        val source = fakeSource("android.permission.CHANGE_COMPONENT_ENABLED_STATE",
            base = PermissionInfo.PROTECTION_SIGNATURE, flags = 0)
        assertEquals(Constants.LEVEL_SIGNATURE,
            resolvePermLevel("android.permission.CHANGE_COMPONENT_ENABLED_STATE", source, cache))
    }

    @Test
    fun `APPOP flag overrides base level to special`() {
        // NORMAL base + APPOP flag → "special"
        val source = fakeSource("android.permission.PACKAGE_USAGE_STATS",
            base = PermissionInfo.PROTECTION_NORMAL,
            flags = PermissionInfo.PROTECTION_FLAG_APPOP)
        assertEquals(Constants.LEVEL_SPECIAL,
            resolvePermLevel("android.permission.PACKAGE_USAGE_STATS", source, cache))
    }

    @Test
    fun `SYSTEM_ALERT_WINDOW is always special even without APPOP flag`() {
        // Name is in SPECIAL_PERMISSION_NAMES → always "special"
        val source = fakeSource("android.permission.SYSTEM_ALERT_WINDOW",
            base = PermissionInfo.PROTECTION_NORMAL, flags = 0)
        assertEquals(Constants.LEVEL_SPECIAL,
            resolvePermLevel("android.permission.SYSTEM_ALERT_WINDOW", source, cache))
    }

    @Test
    fun `REQUEST_INSTALL_PACKAGES is special`() {
        val source = fakeSource("android.permission.REQUEST_INSTALL_PACKAGES",
            base = PermissionInfo.PROTECTION_NORMAL, flags = 0)
        assertEquals(Constants.LEVEL_SPECIAL,
            resolvePermLevel("android.permission.REQUEST_INSTALL_PACKAGES", source, cache))
    }

    @Test
    fun `BIND_ACCESSIBILITY_SERVICE is special`() {
        val source = fakeSource("android.permission.BIND_ACCESSIBILITY_SERVICE",
            base = PermissionInfo.PROTECTION_SIGNATURE, flags = 0)
        assertEquals(Constants.LEVEL_SPECIAL,
            resolvePermLevel("android.permission.BIND_ACCESSIBILITY_SERVICE", source, cache))
    }

    @Test
    fun `NameNotFoundException defaults to normal`() {
        val source = PermissionInfoSource { throw PackageManager.NameNotFoundException("unknown") }
        assertEquals(Constants.LEVEL_NORMAL,
            resolvePermLevel("com.custom.some.PERM", source, cache))
    }

    @Test
    fun `level is cached after first lookup`() {
        var callCount = 0
        val source = PermissionInfoSource {
            callCount++
            fakePermissionInfo(PermissionInfo.PROTECTION_DANGEROUS, 0)
        }
        resolvePermLevel("android.permission.CAMERA", source, cache)
        resolvePermLevel("android.permission.CAMERA", source, cache)
        assertEquals("PM should be called exactly once (cache hit on second call)", 1, callCount)
    }

    @Test
    fun `different permissions get independent cache entries`() {
        val source = PermissionInfoSource { name ->
            when (name) {
                "android.permission.CAMERA"      -> fakePermissionInfo(PermissionInfo.PROTECTION_DANGEROUS, 0)
                "android.permission.INTERNET"    -> fakePermissionInfo(PermissionInfo.PROTECTION_NORMAL, 0)
                else                             -> throw PackageManager.NameNotFoundException(name)
            }
        }
        assertEquals(Constants.LEVEL_DANGEROUS,
            resolvePermLevel("android.permission.CAMERA",  source, cache))
        assertEquals(Constants.LEVEL_NORMAL,
            resolvePermLevel("android.permission.INTERNET", source, cache))
    }

    // ── fakeApps content checks ───────────────────────────────────────────────

    @Test
    fun `fakeApps returns at least 4 apps`() {
        assertTrue(AppScanner.fakeApps().size >= 4)
    }

    @Test
    fun `Torch Master is sideloaded with SMS permissions granted`() {
        val torch = AppScanner.fakeApps().first { it.pkg == "com.fake.torch" }
        assertNull("Torch Master should be sideloaded (null installer)", torch.installer)
        assertTrue(torch.perms.any { it.name == "android.permission.READ_SMS"    && it.granted })
        assertTrue(torch.perms.any { it.name == "android.permission.RECEIVE_SMS" && it.granted })
        assertTrue(torch.perms.any { it.name == "android.permission.READ_CONTACTS" && it.granted })
        assertTrue(torch.perms.any { it.name == "android.permission.RECEIVE_BOOT_COMPLETED" && it.granted })
    }

    @Test
    fun `QuickChat is from Play Store with expected permissions`() {
        val chat = AppScanner.fakeApps().first { it.pkg == "com.fake.quickchat" }
        assertEquals(Constants.INSTALLER_PLAY_STORE, chat.installer)
        assertTrue(chat.perms.any { it.name == "android.permission.READ_CALL_LOG" && it.granted })
        assertTrue(chat.perms.any { it.name == "android.permission.RECORD_AUDIO"  && it.granted })
    }

    @Test
    fun `Polite Notes only has INTERNET`() {
        val notes = AppScanner.fakeApps().first { it.pkg == "com.fake.politenotes" }
        assertEquals(Constants.INSTALLER_PLAY_STORE, notes.installer)
        assertEquals(1, notes.perms.size)
        assertEquals("android.permission.INTERNET", notes.perms[0].name)
    }

    @Test
    fun `FakeCam Pro has declared-but-not-granted location permissions`() {
        val cam = AppScanner.fakeApps().first { it.pkg == "com.fake.fakecampro" }
        val fine = cam.perms.firstOrNull { it.name == "android.permission.ACCESS_FINE_LOCATION" }
        assertNotNull("ACCESS_FINE_LOCATION should be declared", fine)
        assertFalse("ACCESS_FINE_LOCATION should NOT be granted", fine!!.granted)

        val bg = cam.perms.firstOrNull { it.name == "android.permission.ACCESS_BACKGROUND_LOCATION" }
        assertNotNull("ACCESS_BACKGROUND_LOCATION should be declared", bg)
        assertFalse("ACCESS_BACKGROUND_LOCATION should NOT be granted", bg!!.granted)
    }

    @Test
    fun `all four protection levels appear across fakeApps`() {
        val allLevels = AppScanner.fakeApps().flatMap { it.perms }.map { it.level }.toSet()
        assertTrue("normal missing",    Constants.LEVEL_NORMAL    in allLevels)
        assertTrue("dangerous missing", Constants.LEVEL_DANGEROUS in allLevels)
        assertTrue("signature missing", Constants.LEVEL_SIGNATURE in allLevels)
        assertTrue("special missing",   Constants.LEVEL_SPECIAL   in allLevels)
    }

    @Test
    fun `fakeApps special field is empty — Person 3 fills it`() {
        AppScanner.fakeApps().forEach { app ->
            assertTrue("${app.label}.special should be empty at scan time", app.special.isEmpty())
        }
    }

    // ── room mapping via Constants ────────────────────────────────────────────

    @Test
    fun `known permissions map to the correct room`() {
        assertEquals(Constants.ROOM_MESSAGES,   Constants.PERMISSION_TO_ROOM["android.permission.READ_SMS"])
        assertEquals(Constants.ROOM_LOCATION,   Constants.PERMISSION_TO_ROOM["android.permission.ACCESS_FINE_LOCATION"])
        assertEquals(Constants.ROOM_MICROPHONE, Constants.PERMISSION_TO_ROOM["android.permission.RECORD_AUDIO"])
        assertEquals(Constants.ROOM_CAMERA,     Constants.PERMISSION_TO_ROOM["android.permission.CAMERA"])
        assertEquals(Constants.ROOM_CONTACTS,   Constants.PERMISSION_TO_ROOM["android.permission.READ_CONTACTS"])
        assertEquals(Constants.ROOM_PHONE,      Constants.PERMISSION_TO_ROOM["android.permission.READ_CALL_LOG"])
        assertEquals(Constants.ROOM_STORAGE,    Constants.PERMISSION_TO_ROOM["android.permission.READ_EXTERNAL_STORAGE"])
        assertEquals(Constants.ROOM_SENSORS,    Constants.PERMISSION_TO_ROOM["android.permission.BODY_SENSORS"])
    }

    @Test
    fun `INTERNET has no room — it is null`() {
        assertNull(Constants.PERMISSION_TO_ROOM["android.permission.INTERNET"])
    }

    @Test
    fun `RECEIVE_BOOT_COMPLETED has no room`() {
        assertNull(Constants.PERMISSION_TO_ROOM["android.permission.RECEIVE_BOOT_COMPLETED"])
    }

    // ── Constants sanity ─────────────────────────────────────────────────────

    @Test
    fun `all SPECIAL_PERMISSION_NAMES entries resolve to special via their name check`() {
        // Source always returns NORMAL base with no flags — the name check must override.
        val source = PermissionInfoSource { fakePermissionInfo(PermissionInfo.PROTECTION_NORMAL, 0) }
        Constants.SPECIAL_PERMISSION_NAMES.forEach { name ->
            val c = HashMap<String, String>()
            assertEquals(
                "Expected SPECIAL for $name",
                Constants.LEVEL_SPECIAL,
                resolvePermLevel(name, source, c),
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a [PermissionInfoSource] that returns a fake [PermissionInfo] for [name]
     * and throws [PackageManager.NameNotFoundException] for any other name.
     */
    private fun fakeSource(name: String, base: Int, flags: Int): PermissionInfoSource =
        PermissionInfoSource { requested ->
            if (requested == name) fakePermissionInfo(base, flags)
            else throw PackageManager.NameNotFoundException(requested)
        }
}
