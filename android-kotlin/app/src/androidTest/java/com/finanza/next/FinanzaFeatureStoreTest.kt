package com.finanza.next

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finanza.next.features.FeatureMutation
import com.finanza.next.features.FinanzaFeatureStore
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FinanzaFeatureStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs by lazy { context.getSharedPreferences("finanza_feature_store_test", Context.MODE_PRIVATE) }
    private val store by lazy { FinanzaFeatureStore(context, prefs) }

    @Before
    fun prepare() {
        prefs.edit().clear().commit()
    }

    @After
    fun cleanup() {
        prefs.edit().clear().commit()
    }

    @Test
    fun remoteWebStateIsNormalizedForAndroid() {
        val state = JSONObject()
            .put("shopping", JSONObject()
                .put("lists", JSONArray().put(JSONObject().put("id", "l1").put("name", "Mercado").put("icon", "cart")))
                .put("items", JSONArray().put(JSONObject().put("id", "i1").put("list_id", "l1").put("name", "Cafe").put("category", "Mercado"))))
            .put("car", JSONObject()
                .put("vehicles", JSONArray().put(JSONObject().put("id", "v1").put("name", "Carro")))
                .put("events", JSONArray().put(JSONObject().put("id", "e1").put("vehicle_id", "v1").put("type", "fuel").put("liters", 20).put("price_per_liter", 5.5)))
                .put("active_vehicle_id", "v1"))
            .put("settings", JSONObject().put("commitments", JSONObject().put(
                "debts",
                JSONArray().put(JSONObject().put("id", "d1").put("outstanding_amount", 900).put("next_due_date", "2026-08-10"))
            )))

        store.applyRemote(JSONArray(), JSONArray(), state)
        val backup = store.backupPayload()

        assertEquals("l1", backup.getJSONObject("shopping").getJSONArray("items").getJSONObject(0).getString("listId"))
        assertEquals(5.5, backup.getJSONObject("car").getJSONArray("events").getJSONObject(0).getDouble("pricePerLiter"), 0.001)
        assertEquals(900.0, backup.getJSONObject("settings").getJSONObject("commitments").getJSONArray("debts").getJSONObject(0).getDouble("outstandingAmount"), 0.001)
    }

    @Test
    fun statePayloadPreservesUnknownRemoteModules() {
        val base = JSONObject().put("web_only", JSONObject().put("enabled", true)).put(
            "settings",
            JSONObject().put("future_key", 7).put("rates", JSONObject().put("selic", 10.5))
        )

        val payload = store.statePayload(base)

        assertNotNull(payload.optJSONObject("web_only"))
        assertEquals(7, payload.getJSONObject("settings").getInt("future_key"))
        assertEquals(10.5, payload.getJSONObject("settings").getJSONObject("rates").getDouble("selic"), 0.001)
        assertEquals("android_owner", payload.getJSONObject("settings").getJSONObject("rates").getJSONObject("sharedSpace").getString("ownerPersonId"))
    }

    @Test
    fun stateQueueCompactsAndAcknowledgesWithoutDroppingOtherWork() {
        store.enqueue(FeatureMutation("shopping", "update", "i1", "{\"v\":1}"))
        store.enqueue(FeatureMutation("car", "update", "e1", "{\"v\":2}"))
        val budget = FeatureMutation("budgets", "create", "b1", "{\"limit\":100}")
        store.enqueue(budget)

        assertEquals(2, store.pendingMutations().size)
        store.acknowledge(budget)
        assertEquals(1, store.pendingMutations().size)
        assertEquals("car", store.pendingMutations().single().moduleId)
    }

    @Test
    fun backupMergeKeepsListsVehiclesAndTheirItems() {
        store.importBackup(backup("a"), false)
        store.importBackup(backup("b"), true)

        val merged = store.backupPayload()
        assertEquals(2, merged.getJSONObject("shopping").getJSONArray("lists").length())
        assertEquals(2, merged.getJSONObject("shopping").getJSONArray("items").length())
        assertEquals(2, merged.getJSONObject("car").getJSONArray("vehicles").length())
        assertEquals(2, merged.getJSONObject("car").getJSONArray("events").length())
    }

    @Test
    fun vehicleInUseCannotBeDeletedAndCarTransactionStaysLinked() {
        store.importBackup(backup("a"), false)

        assertEquals(null, store.delete("vehicles", "vehicle_a"))
        val linked = store.attachCarTransaction("event_a", 321L, "wallet")

        assertEquals(321L, store.carTransactionId("event_a"))
        assertEquals("wallet", JSONObject(linked?.payload ?: "{}").optString("accountId"))
    }

    @Test
    fun invalidPlanningDataIsRejectedBeforePersistence() {
        val mutation = store.save("budgets", null, mapOf("category" to "Mercado", "limit" to "0"))

        assertEquals(null, mutation)
        assertEquals(0, store.backupPayload().getJSONArray("budgets").length())
    }

    @Test
    fun navigationExposesSharedModulesWithReadOnlyPermissionsAndCarInsights() {
        val data = backup("a")
        data.getJSONObject("car").put("events", JSONArray()
            .put(JSONObject().put("id", "f1").put("vehicleId", "vehicle_a").put("type", "fuel").put("date", "2026-06-01").put("odometer", 10000).put("liters", 40).put("amount", 220))
            .put(JSONObject().put("id", "f2").put("vehicleId", "vehicle_a").put("type", "fuel").put("date", "2026-07-01").put("odometer", 10480).put("liters", 40).put("amount", 230))
            .put(JSONObject().put("id", "m1").put("vehicleId", "vehicle_a").put("type", "expense").put("date", "2026-07-02").put("odometer", 10480).put("title", "Troca de oleo").put("amount", 300)))
        store.importBackup(data, false)

        val state = store.buildUiState(emptyMap(), online = true, canWrite = false)
        val car = state.modules.first { it.id == "car" }
        val debtStatus = state.modules.first { it.id == "debts" }.newFields.first { it.key == "status" }
        val shared = state.modules.first { it.id == "shared" }

        assertEquals(false, shared.canCreate)
        assertEquals(false, shared.items.first().canEdit)
        assertEquals(true, state.modules.any { it.id == "shared_space" })
        assertEquals(listOf("active", "watch", "closed"), debtStatus.options)
        assertEquals(true, car.insights.any { it.label == "Consumo medio" && it.value.contains("12.0") })
        assertEquals(6, car.trends.size)
    }

    @Test
    fun sharedInviteMergesPeopleWithoutReplacingLocalOwner() {
        val local = store.save("shared", "android_owner", mapOf("name" to "Jefferson", "permission" to "owner"))
        assertNotNull(local)
        val invite = JSONObject()
            .put("v", 1)
            .put("name", "Casa")
            .put("mode", "house")
            .put("ownerPersonId", "remote_owner")
            .put("people", JSONArray()
                .put(JSONObject().put("id", "remote_owner").put("name", "Ana").put("permission", "owner"))
                .put(JSONObject().put("id", "guest").put("name", "Bia").put("permission", "read")))

        val mutation = store.mergeSharedInvite(invite)
        val merged = store.sharedSpacePayload()

        assertNotNull(mutation)
        assertEquals("android_owner", merged.getString("ownerPersonId"))
        assertEquals("house", merged.getString("mode"))
        assertEquals(3, merged.getJSONArray("people").length())
        assertEquals("owner", merged.getJSONArray("people").getJSONObject(0).getString("permission"))
    }

    @Test
    fun sharedExpenseKeepsSelectedPayerParticipantsAndApprovalRequest() {
        store.save("shared", null, mapOf("name" to "Ana", "permission" to "editor"))
        store.save("shared", null, mapOf("name" to "Bia", "permission" to "read"))
        val people = store.sharedPeopleOptions()
        val ana = people.first { it.second == "Ana" }.first
        val owner = store.sharedOwnerId()

        val meta = store.equalSplitMeta(ana, listOf(owner, ana), requestApproval = true)

        assertEquals(ana, meta?.getString("payerId"))
        assertEquals(2, meta?.getJSONArray("participants")?.length())
        assertEquals("pending", meta?.getJSONObject("approval")?.getString("status"))
    }

    private fun backup(id: String) = JSONObject()
        .put("shopping", JSONObject()
            .put("lists", JSONArray().put(JSONObject().put("id", "list_$id").put("name", id)))
            .put("items", JSONArray().put(JSONObject().put("id", "item_$id").put("listId", "list_$id").put("name", id))))
        .put("car", JSONObject()
            .put("vehicles", JSONArray().put(JSONObject().put("id", "vehicle_$id").put("name", id)))
            .put("events", JSONArray().put(JSONObject().put("id", "event_$id").put("vehicleId", "vehicle_$id").put("type", "expense").put("amount", 10))))
        .put("settings", JSONObject())
}
