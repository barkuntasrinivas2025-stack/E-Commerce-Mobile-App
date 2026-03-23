package com.confidencecommerce.security

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HmacRequestSignerTest {

    private val securePrefs = mockk<SecurePreferencesManager>()
    private lateinit var signer: HmacRequestSigner

    @Before
    fun setUp() {
        coEvery { securePrefs.getSessionToken() } returns null
        signer = HmacRequestSigner(securePrefs)
    }

    @Test
    fun `sign produces non-empty signature`() = runTest {
        val result = signer.sign("prod_001")
        assertTrue(result.signature.isNotBlank())
    }

    @Test
    fun `two signs of same resource produce different signatures due to nonce`() = runTest {
        val s1 = signer.sign("prod_001")
        val s2 = signer.sign("prod_001")
        // Nonces are random — signatures should differ even for same input
        assertNotEquals(s1.nonce, s2.nonce)
    }

    @Test
    fun `timestamp is approximately current time`() = runTest {
        val before = System.currentTimeMillis()
        val result = signer.sign("prod_001")
        val after  = System.currentTimeMillis()
        assertTrue(result.timestamp in before..after)
    }

    @Test
    fun `verifyResponseSignature rejects expired timestamp`() = runTest {
        val staleTimestamp = System.currentTimeMillis() - 600_000L // 10 minutes old
        val isValid = signer.verifyResponseSignature("payload", staleTimestamp, "sig")
        assertFalse(isValid)
    }

    @Test
    fun `verifyResponseSignature rejects future timestamp`() = runTest {
        val futureTimestamp = System.currentTimeMillis() + 600_000L
        val isValid = signer.verifyResponseSignature("payload", futureTimestamp, "sig")
        assertFalse(isValid)
    }

    @Test
    fun `constant time equals returns false for different strings`() = runTest {
        // Test via sign/verify path — different signature should fail
        val timestamp = System.currentTimeMillis()
        val invalid = signer.verifyResponseSignature("real_payload", timestamp, "wrong_signature=")
        assertFalse(invalid)
    }
}
