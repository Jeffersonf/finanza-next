package com.finanza.next

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaAccountRepositoryTest {
    @Test
    fun accountSecurityAndAdminUseWebRoutes() {
        val requests = mutableListOf<String>()
        val repository = FinanzaAccountRepository(FinanzaJsonTransport { method, path, _ ->
            requests += "$method $path"
            when (path) {
                FinanzaAccountRoutes.USERS, FinanzaAccountRoutes.audit(30) -> JSONObject().put("data", JSONArray())
                FinanzaAccountRoutes.RECOVERY_CODE -> JSONObject().put("recovery_code", "safe-code")
                else -> JSONObject()
            }
        })

        repository.profile()
        assertEquals("safe-code", repository.recoveryCode())
        repository.beginTwoFactor()
        repository.confirmTwoFactor("123456")
        repository.disableTwoFactor("654321")
        repository.users()
        repository.adminOverview()
        repository.audit()

        assertEquals(listOf(
            "GET /api/me",
            "POST /api/me/recovery-code",
            "POST /api/me/2fa/setup",
            "POST /api/me/2fa/confirm",
            "DELETE /api/me/2fa",
            "GET /api/users",
            "GET /api/admin/overview",
            "GET /api/audit-log?limit=30"
        ), requests)
    }

    @Test
    fun rolesPasswordsAndSelfAdministrationAreValidatedLocally() {
        val repository = FinanzaAccountRepository(FinanzaJsonTransport { _, _, _ -> JSONObject() })

        assertEquals(listOf("admin", "editor", "read", "guest"), FinanzaAccountRules.roles)
        assertEquals(false, FinanzaAccountRules.canWrite("read"))
        assertEquals(false, FinanzaAccountRules.canAdmin("owner"))
        assertThrows(IllegalArgumentException::class.java) { repository.createUser("Ana", "ana", "short", "editor") }
        assertThrows(IllegalArgumentException::class.java) { repository.updateUserRole("42", "42", "read") }
        assertThrows(IllegalArgumentException::class.java) { repository.deleteUser("42", "42") }
    }
}
