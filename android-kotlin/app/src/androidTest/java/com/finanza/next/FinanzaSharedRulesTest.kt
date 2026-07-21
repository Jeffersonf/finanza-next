package com.finanza.next

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaSharedRulesTest {
    private val space = JSONObject()
        .put("ownerPersonId", "owner")
        .put("people", JSONArray()
            .put(JSONObject().put("id", "owner").put("name", "Eu"))
            .put(JSONObject().put("id", "ana").put("name", "Ana")))

    @Test
    fun approvedExpenseAndSettlementFollowWebBalanceSigns() {
        val expense = JSONObject()
            .put("kind", "equal")
            .put("payerId", "owner")
            .put("participants", JSONArray().put("owner").put("ana"))
            .put("approval", JSONObject().put("status", "approved"))
        val settlement = JSONObject()
            .put("kind", "settlement")
            .put("fromPersonId", "ana")
            .put("toPersonId", "owner")

        val balances = FinanzaSharedRules.balances(space, listOf(
            FinanzaSharedTransaction("expense", 100.0, expense.toString()),
            FinanzaSharedTransaction("income", 20.0, settlement.toString())
        ))

        assertEquals(30.0, balances.getValue("ana"), 0.001)
    }

    @Test
    fun pendingOrRejectedExpensesDoNotChangeBalances() {
        val transactions = listOf("pending", "rejected").map { status ->
            val meta = JSONObject()
                .put("kind", "equal")
                .put("payerId", "owner")
                .put("participants", JSONArray().put("owner").put("ana"))
                .put("approval", JSONObject().put("status", status))
            FinanzaSharedTransaction("expense", 100.0, meta.toString())
        }

        assertEquals(0.0, FinanzaSharedRules.balances(space, transactions).getValue("ana"), 0.001)
    }
}
