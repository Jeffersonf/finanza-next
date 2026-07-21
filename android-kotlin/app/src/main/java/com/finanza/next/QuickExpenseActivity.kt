package com.finanza.next

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.finanza.next.ui.screens.PaymentMethodUi
import com.finanza.next.ui.screens.QuickExpenseFlow
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import kotlin.concurrent.thread

class QuickExpenseActivity : ComponentActivity() {
    private val prefs by lazy { FinanzaPreferences.get(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FinanzaPreferences.repairLegacyTypes(this)
        setFinishOnTouchOutside(true)
        // Keep the compact floating activity below the system status area.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.decorView.importantForContentCapture = View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply { blurBehindRadius = dp(30) }
        }
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        val methods = paymentMethods()
        setContent {
            FinanceAppTheme(darkTheme = isDarkTheme(), experience = visualExperience()) {
                QuickExpenseFlow(
                    methods = methods,
                    onComplete = { rawAmount, rawDescription, method ->
                        saveAndClose(rawAmount, rawDescription, "", method)
                    },
                    onDismiss = ::finish,
                    modifier = Modifier.padding(horizontal = 30.dp, vertical = 14.dp)
                )
            }
        }

        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        window.attributes = window.attributes.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            dimAmount = 0.10f
            y = dp(4)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }

    private fun saveAndClose(rawAmount: String, rawDescription: String, category: String, method: PaymentMethodUi) {
        val amount = parseMoneyAmount(rawAmount)
        if (amount == null || amount <= 0.0) {
            Toast.makeText(this, "Digite um valor valido.", Toast.LENGTH_SHORT).show()
            return
        }
        val title = rawDescription.trim().ifBlank { "Gasto rapido" }
        val selectedCategory = category.ifBlank { inferQuickCategory(title) }
        val localId = saveQuickExpense(amount, title, selectedCategory, method.id)
        syncQuickExpenseAsync(localId, amount, title, selectedCategory, method.id)
        Toast.makeText(this, "Gasto salvo.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun paymentMethods(): List<PaymentMethodUi> {
        val accounts = runCatching { JSONArray(prefs.getString("accounts", "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until accounts.length()) {
                val account = accounts.optJSONObject(index) ?: continue
                val id = account.optString("id")
                val name = account.optString("name").ifBlank { "Conta" }
                val type = account.optString("type")
                add(PaymentMethodUi(id, name, if (type == "credit") Icons.Rounded.CreditCard else Icons.Rounded.AccountBalanceWallet))
            }
        }.ifEmpty { listOf(PaymentMethodUi("", "Principal")) }
    }

    private fun saveQuickExpense(amount: Double, title: String, category: String, accountId: String): Long {
        val localId = System.currentTimeMillis()
        val array = runCatching { JSONArray(prefs.getString("entries", "[]")) }.getOrDefault(JSONArray())
        array.put(JSONObject().apply {
            put("id", localId)
            put("title", title)
            put("category", category)
            put("amount", amount)
            put("type", "expense")
            put("date", LocalDate.now().toString())
            put("accountId", accountId)
        })
        prefs.edit().putString("entries", array.toString()).apply()
        FinanzaWidgets.updateAll(this)
        return localId
    }

    private fun syncQuickExpenseAsync(localId: Long, amount: Double, title: String, category: String, accountId: String) {
        val client = FinanzaApiClient(prefs)
        val payload = FinanzaApiClient.quickExpensePayload(
            amount = amount,
            title = title,
            category = category,
            accountId = accountId,
            date = LocalDate.now().toString()
        )
        if (!client.hasCredentials) return
        if (!client.isConfigured) {
            FinanzaSyncQueue.enqueueRequest(prefs, "POST", FinanzaApiRoutes.TRANSACTIONS, payload, localId)
            return
        }
        thread {
            runCatching { FinanzaRemoteRepository(client).createTransaction(payload) }
                .onFailure {
                    FinanzaSyncQueue.enqueueRequest(prefs, "POST", FinanzaApiRoutes.TRANSACTIONS, payload, localId)
                }
        }
    }

    private fun parseMoneyAmount(value: String): Double? {
        val raw = value.trim().replace("R$", "", ignoreCase = true).replace(" ", "")
        val normalized = if (raw.contains(',')) raw.replace(".", "").replace(',', '.') else raw
        return normalized.toDoubleOrNull()
    }

    private fun inferQuickCategory(title: String): String {
        val normalized = title.trim().lowercase()
        val entries = runCatching { JSONArray(prefs.getString("entries", "[]")) }.getOrDefault(JSONArray())
        for (index in entries.length() - 1 downTo 0) {
            val item = entries.optJSONObject(index) ?: continue
            if (item.optString("type") == "expense" && item.optString("title").trim().lowercase() == normalized) {
                return FinanzaCategories.infer(title, item.optString("category"))
            }
        }
        return FinanzaCategories.infer(title)
    }

    private fun isDarkTheme(): Boolean = prefs.getString("theme_mode", "light") == "dark"
    private fun visualExperience(): AppExperience =
        AppExperience.fromId(prefs.getString("visual_experience", AppExperience.NEXT.id))
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
