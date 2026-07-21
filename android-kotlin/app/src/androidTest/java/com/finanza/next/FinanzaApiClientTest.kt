package com.finanza.next

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaApiClientTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs by lazy { context.getSharedPreferences("finanza_api_contract_test", Context.MODE_PRIVATE) }

    @Before
    fun prepare() {
        prefs.edit().clear()
            .putBoolean("online_mode", true)
            .putString("api_url", "https://finanza.invalid")
            .putString("api_key", "test-key")
            .commit()
    }

    @After
    fun cleanup() {
        prefs.edit().clear().commit()
    }

    @Test
    fun unauthorizedExpiresSessionWithoutDeletingCredentials() {
        val error = finanzaApiException(401, "Chave invalida")
        assertTrue(error is FinanzaAuthenticationException)

        markAuthenticationExpired(prefs, error.message.orEmpty())
        val client = FinanzaApiClient(prefs)

        assertTrue(client.hasCredentials)
        assertFalse(client.isConfigured)
        assertEquals("Chave invalida", prefs.getString("last_sync_error", ""))
    }

    @Test
    fun forbiddenDoesNotExpireSession() {
        val error = finanzaApiException(403, "Sem permissao")

        assertFalse(error is FinanzaAuthenticationException)
        assertEquals(403, error.statusCode)
        assertTrue(FinanzaApiClient(prefs).isConfigured)
    }

    @Test
    fun singletonQueueKeepsOnlyLatestStateMutation() {
        FinanzaSyncQueue.enqueueSingleton(prefs, "accounts", JSONObject().put("version", 1))
        FinanzaSyncQueue.enqueueSingleton(prefs, "accounts", JSONObject().put("version", 2))
        FinanzaSyncQueue.enqueueSingleton(prefs, "future_state", JSONObject().put("version", 1))

        assertEquals(2, FinanzaSyncQueue.pendingCount(prefs))
    }
}
