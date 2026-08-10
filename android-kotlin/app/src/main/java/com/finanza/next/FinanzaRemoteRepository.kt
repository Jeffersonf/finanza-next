package com.finanza.next

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal object FinanzaApiRoutes {
    const val TRANSACTIONS = "/api/transactions"
    const val BUDGETS = "/api/budgets"
    const val GOALS = "/api/goals"
    const val STATE = "/api/state"
    const val IMPORT = "/api/import"

    fun transactions(limit: Int, cursor: String? = null, offset: Int? = null): String = buildString {
        append("$TRANSACTIONS?limit=$limit")
        cursor?.takeIf(String::isNotBlank)?.let {
            append("&cursor=")
            append(URLEncoder.encode(it, StandardCharsets.UTF_8.name()))
        }
        if (cursor.isNullOrBlank()) {
            offset?.takeIf { it > 0 }?.let {
                append("&offset=")
                append(it)
            }
        }
    }
    fun transaction(id: Long): String = "$TRANSACTIONS/$id"
    fun budget(id: String): String = "$BUDGETS/$id"
    fun goal(id: String): String = "$GOALS/$id"
    fun goalContribution(id: String): String = "${goal(id)}/add"
}

internal fun interface FinanzaJsonTransport {
    fun request(method: String, path: String, body: JSONObject?): JSONObject
}

internal data class FinanzaRemoteSnapshot(
    val transactions: JSONArray,
    val transactionTotal: Int,
    val budgets: JSONArray,
    val goals: JSONArray,
    val state: JSONObject
)

internal class FinanzaRemoteRepository(
    private val transport: FinanzaJsonTransport
) {
    constructor(client: FinanzaApiClient) : this(
        FinanzaJsonTransport { method, path, body -> client.requestJson(method, path, body) }
    )

    fun loadSnapshot(transactionLimit: Int = 1_000): FinanzaRemoteSnapshot {
        var transactionResponse = transport.request("GET", FinanzaApiRoutes.transactions(transactionLimit), null)
        val transactions = JSONArray()
        val transactionIds = mutableSetOf<String>()
        val expectedTotal = transactionResponse.optInt("total", -1)
        val cursors = mutableSetOf<String>()
        var pageOffset = 0

        while (true) {
            val page = transactionResponse.optJSONArray("data") ?: JSONArray()
            for (index in 0 until page.length()) {
                val item = page.optJSONObject(index) ?: continue
                val id = item.opt("id")?.toString().orEmpty()
                if (id.isBlank() || transactionIds.add(id)) transactions.put(item)
            }
            pageOffset += page.length()
            val cursor = transactionResponse.nextTransactionCursor()
            when {
                !cursor.isNullOrBlank() && cursors.add(cursor) -> {
                    transactionResponse = transport.request(
                        "GET",
                        FinanzaApiRoutes.transactions(transactionLimit, cursor = cursor),
                        null
                    )
                }
                page.length() > 0 && expectedTotal > pageOffset -> {
                    transactionResponse = transport.request(
                        "GET",
                        FinanzaApiRoutes.transactions(transactionLimit, offset = pageOffset),
                        null
                    )
                }
                else -> break
            }
        }
        return FinanzaRemoteSnapshot(
            transactions = transactions,
            transactionTotal = expectedTotal.takeIf { it >= 0 } ?: transactions.length(),
            budgets = transport.request("GET", FinanzaApiRoutes.BUDGETS, null)
                .optJSONArray("data") ?: JSONArray(),
            goals = transport.request("GET", FinanzaApiRoutes.GOALS, null)
                .optJSONArray("data") ?: JSONArray(),
            state = loadState()
        )
    }

    fun createTransaction(payload: JSONObject): JSONObject =
        transport.request("POST", FinanzaApiRoutes.TRANSACTIONS, payload)

    fun updateTransaction(id: Long, payload: JSONObject): JSONObject =
        transport.request("PUT", FinanzaApiRoutes.transaction(id), payload)

    fun deleteTransaction(id: Long) {
        transport.request("DELETE", FinanzaApiRoutes.transaction(id), null)
    }

    fun createBudget(payload: JSONObject): JSONObject = transport.request("POST", FinanzaApiRoutes.BUDGETS, payload)

    fun deleteBudget(id: String) {
        transport.request("DELETE", FinanzaApiRoutes.budget(id), null)
    }

    fun createGoal(payload: JSONObject): JSONObject = transport.request("POST", FinanzaApiRoutes.GOALS, payload)

    fun addGoalContribution(id: String, amount: Double): JSONObject =
        transport.request("PATCH", FinanzaApiRoutes.goalContribution(id), JSONObject().put("amount", amount))

    fun deleteGoal(id: String) {
        transport.request("DELETE", FinanzaApiRoutes.goal(id), null)
    }

    fun importBackup(payload: JSONObject): JSONObject = transport.request("PUT", FinanzaApiRoutes.IMPORT, payload)

    fun loadState(): JSONObject = transport.request("GET", FinanzaApiRoutes.STATE, null)

    fun saveState(state: JSONObject): JSONObject =
        transport.request("PUT", FinanzaApiRoutes.STATE, state)

    fun updateState(change: (JSONObject) -> Unit): JSONObject {
        val state = JSONObject(loadState().toString())
        change(state)
        return saveState(state)
    }
}

private fun JSONObject.nextTransactionCursor(): String? = sequenceOf(
    optString("next_cursor"),
    optString("nextCursor")
).firstOrNull { it.isNotBlank() }
