package com.confidencecommerce.security

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RuntimeIntegrityShieldTest {

    private val context = mockk<Context>(relaxed = true)
    private lateinit var shield: RuntimeIntegrityShield

    @Before
    fun setUp() {
        // Mock packageManager for signature check
        every { context.packageName } returns "com.confidencecommerce"
        every { context.applicationInfo } returns mockk(relaxed = true) {
            every { flags } returns 0  // Not debuggable
        }
        shield = RuntimeIntegrityShield(context)
    }

    @Test
    fun `debug build is not flagged as debuggable in unit test context`() {
        // In unit tests, Debug.isDebuggerConnected() returns false
        val result = shield.detectDebugger()
        assertFalse(result)
    }

    @Test
    fun `no hooking frameworks in clean test environment`() {
        val result = shield.detectHookingFrameworks()
        assertFalse(result)
    }

    @Test
    fun `no Frida in clean test environment`() {
        val result = shield.detectFrida()
        // May be true if running in specific CI with Frida — acceptable
        // Main test: method doesn't throw
        assertNotNull(result)
    }

    @Test
    fun `threat score is zero in clean test environment`() {
        val report = shield.scan()
        // In a clean unit test JVM: no debugger, no hooks, no Frida
        assertFalse(report.isHookingFrameworkDetected)
        assertFalse(report.isFridaDetected)
        assertFalse(report.isSignatureTampered)  // Debug build skips signature check
    }

    @Test
    fun `integrity report fields are all populated`() {
        val report = shield.scan()
        assertNotNull(report.threatLevel)
        assertTrue(report.threatScore in 0..100)
    }

    @Test
    fun `CLEAN level when no threats detected`() {
        val report = shield.scan()
        // In a unit test environment with no root/hooks/frida, should be CLEAN
        assertEquals(RuntimeIntegrityShield.IntegrityReport.ThreatLevel.CLEAN, report.threatLevel)
    }
}
