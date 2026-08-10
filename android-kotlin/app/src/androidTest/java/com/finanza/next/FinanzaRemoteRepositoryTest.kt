package com.finanza.next

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaRemoteRepositoryTest {
    @Test
    fun snapshotUsesCanonicalCoreRoutes() {
        val requests = mutableListOf<String>()
        val repository = FinanzaRemoteRepository(FinanzaJsonTransport { method, path, _ ->
            requests += "$method $path"
            when (path) {
                FinanzaApiRoutes.transactions(250) -> JSONObject().put("data", JSONArray()).put("total", 267)
                FinanzaApiRoutes.STATE -> JSONObject().put("accounts", JSONArray())
                else -> JSONObject().put("data", JSONArray())
            }
        })

        val snapshot = repository.loadSnapshot(250)

        assertEquals(
            listOf(
                "GET /api/transactions?limit=250",
                "GET /api/budgets",
                "GET /api/goals",
                "GET /api/state"
            ),
            requests
        )
        assertEquals(267, snapshot.transactionTotal)
    }

    @Test
    fun snapshotFollowsCursorPagesAndRemovesRepeatedTransactions() {
        val requests = mutableListOf<String>()
        val repository = FinanzaRemoteRepository(FinanzaJsonTransport { _, path, _ ->
            requests += path
            when (path) {
                FinanzaApiRoutes.transactions(2) -> JSONObject()
                    .put("total", 3)
                    .put("next_cursor", "next page")
                    .put("data", JSONArray()
                        .put(JSONObject().put("id", 1))
                        .put(JSONObject().put("id", 2)))
                FinanzaApiRoutes.transactions(2, "next page") -> JSONObject()
                    .put("data", JSONArray()
                        .put(JSONObject().put("id", 2))
                        .put(JSONObject().put("id", 3)))
                FinanzaApiRoutes.STATE -> JSONObject().put("accounts", JSONArray())
                else -> JSONObject().put("data", JSONArray())
            }
        })

        val snapshot = repository.loadSnapshot(2)

        assertEquals(3, snapshot.transactionTotal)
        assertEquals(3, snapshot.transactions.length())
        assertEquals(
            listOf(
                "/api/transactions?limit=2",
                "/api/transactions?limit=2&cursor=next+page",
                "/api/budgets",
                "/api/goals",
                "/api/state"
            ),
            requests
        )
    }

    @Test
    fun snapshotFallsBackToOffsetPagesWhenTheServerHasNoCursor() {
        val requests = mutableListOf<String>()
        val repository = FinanzaRemoteRepository(FinanzaJsonTransport { _, path, _ ->
            requests += path
            when (path) {
                FinanzaApiRoutes.transactions(2) -> JSONObject()
                    .put("total", 3)
                    .put("data", JSONArray()
                        .put(JSONObject().put("id", 1))
                        .put(JSONObject().put("id", 2)))
                FinanzaApiRoutes.transactions(2, offset = 2) -> JSONObject()
                    .put("total", 3)
                    .put("data", JSONArray().put(JSONObject().put("id", 3)))
                FinanzaApiRoutes.STATE -> JSONObject().put("accounts", JSONArray())
                else -> JSONObject().put("data", JSONArray())
            }
        })

        val snapshot = repository.loadSnapshot(2)

        assertEquals(3, snapshot.transactions.length())
        assertEquals(
            listOf(
                "/api/transactions?limit=2",
                "/api/transactions?limit=2&offset=2",
                "/api/budgets",
                "/api/goals",
                "/api/state"
            ),
            requests
        )
    }

    @Test
    fun stateUpdatePreservesUnknownWebModules() {
        var saved: JSONObject? = null
        val remoteState = JSONObject()
            .put("accounts", JSONArray())
            .put("web_only_module", JSONObject().put("enabled", true))
        val repository = FinanzaRemoteRepository(FinanzaJsonTransport { method, path, body ->
            if (method == "GET" && path == FinanzaApiRoutes.STATE) {
                remoteState
            } else {
                saved = body
                body ?: JSONObject()
            }
        })

        repository.updateState { state ->
            state.put("accounts", JSONArray().put(JSONObject().put("id", "wallet")))
        }

        assertNotNull(saved?.optJSONObject("web_only_module"))
        assertEquals(true, saved?.optJSONObject("web_only_module")?.optBoolean("enabled"))
        assertEquals("wallet", saved?.optJSONArray("accounts")?.optJSONObject(0)?.optString("id"))
    }

    @Test
    fun planningAndImportMutationsUseWebContracts() {
        val requests = mutableListOf<String>()
        val repository = FinanzaRemoteRepository(FinanzaJsonTransport { method, path, _ ->
            requests += "$method $path"
            JSONObject()
        })

        repository.createBudget(JSONObject().put("category", "Mercado").put("limit", 500))
        repository.deleteBudget("12")
        repository.createGoal(JSONObject().put("name", "Reserva"))
        repository.addGoalContribution("8", 100.0)
        repository.deleteGoal("8")
        repository.importBackup(JSONObject().put("version", "1"))

        assertEquals(
            listOf(
                "POST /api/budgets",
                "DELETE /api/budgets/12",
                "POST /api/goals",
                "PATCH /api/goals/8/add",
                "DELETE /api/goals/8",
                "PUT /api/import"
            ),
            requests
        )
    }
}
