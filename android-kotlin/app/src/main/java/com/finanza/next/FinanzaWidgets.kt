package com.finanza.next

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FinanzaWidgets {
    const val ACTION_QUICK_EXPENSE_INLINE = "com.finanza.next.widget.ACTION_QUICK_EXPENSE_INLINE"
    private const val QUICK_CHANNEL_ID = "finanza_quick_expense"
    private const val QUICK_NOTIFICATION_ID = 4101
    private const val QUICK_REMOTE_INPUT_KEY = "quick_expense_text"

    private val ptBr = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    private val money = NumberFormat.getCurrencyInstance(ptBr)
    private val shortDate = DateTimeFormatter.ofPattern("dd MMM", ptBr)

    fun updateAll(context: Context) {
        FinanzaPreferences.repairLegacyTypes(context)
        updateAllSafely(context)
    }

    private fun updateAllSafely(context: Context) = runCatching {
        val manager = AppWidgetManager.getInstance(context)
        val quickIds = manager.getAppWidgetIds(ComponentName(context, FinanzaQuickExpenseWidgetProvider::class.java))
        val summaryIds = manager.getAppWidgetIds(ComponentName(context, FinanzaSummaryWidgetProvider::class.java))
        val agendaIds = manager.getAppWidgetIds(ComponentName(context, FinanzaAgendaWidgetProvider::class.java))
        if (quickIds.isNotEmpty()) {
            quickIds.forEach { manager.updateAppWidget(it, quickExpenseRemoteViews(context)) }
        }
        if (summaryIds.isNotEmpty()) {
            summaryIds.forEach { manager.updateAppWidget(it, summaryRemoteViews(context)) }
        }
        if (agendaIds.isNotEmpty()) {
            agendaIds.forEach { manager.updateAppWidget(it, agendaRemoteViews(context)) }
        }
    }.getOrDefault(Unit)

    fun quickExpenseRemoteViews(context: Context): RemoteViews {
        FinanzaPreferences.repairLegacyTypes(context)
        val prefs = FinanzaPreferences.get(context)
        return RemoteViews(context.packageName, R.layout.widget_quick_expense).apply {
            setTextColor(R.id.widget_quick_title, Color.WHITE)
            setTextColor(R.id.widget_quick_subtitle, Color.parseColor("#B8C1CC"))
            setTextColor(R.id.widget_quick_account, Color.parseColor("#EFF5FF"))
            setTextColor(R.id.widget_quick_mark, Color.parseColor("#08110B"))
            setTextColor(R.id.widget_quick_button, Color.parseColor("#08110B"))
            setTextViewText(R.id.widget_quick_title, context.getString(R.string.widget_quick_title))
            setTextViewText(R.id.widget_quick_subtitle, context.getString(R.string.widget_quick_subtitle))
            setTextViewText(R.id.widget_quick_account, defaultAccountSummary(prefs))
            setOnClickPendingIntent(R.id.widget_quick_button, openQuickExpenseIntent(context, 1002))
            setOnClickPendingIntent(R.id.widget_quick_root, openQuickExpenseIntent(context, 1003))
        }
    }

    fun showQuickExpenseNotification(context: Context) {
        FinanzaPreferences.repairLegacyTypes(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            QUICK_CHANNEL_ID,
            "Folha rapida",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Atalho persistente para captura rapida"
            setShowBadge(false)
        })

        val builder = Notification.Builder(context, QUICK_CHANNEL_ID)
        builder.addAction(
            Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_finanza),
                "Folha",
                openQuickExpenseIntent(context, 2002)
            ).build()
        )
        builder.addAction(
            Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_finanza),
                "Digitar",
                inlineQuickExpenseIntent(context, 2003)
            ).addRemoteInput(
                RemoteInput.Builder(QUICK_REMOTE_INPUT_KEY)
                    .setLabel("Ex.: 34 mercado")
                    .build()
            ).build()
        )
        val notification = builder
            .setSmallIcon(R.drawable.ic_stat_finanza)
            .setContentTitle("Folha rapida")
            .setContentText("Digite 34 mercado ou abra a captura.")
            .setSubText(defaultAccountSummary(context.getSharedPreferences("finanza_next_native", Context.MODE_PRIVATE)))
            .setContentIntent(openQuickExpenseIntent(context, 2001))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
        manager.notify(QUICK_NOTIFICATION_ID, notification)
    }

    fun summaryRemoteViews(context: Context): RemoteViews {
        FinanzaPreferences.repairLegacyTypes(context)
        val prefs = FinanzaPreferences.get(context)
        val monthlyBudget = prefs.getFloat("monthly_budget", 5000f).toDouble()
        val userName = prefs.getString("user_name", "Voce") ?: "Voce"
        val accent = accentColor(prefs.getString("accent", "green") ?: "green")

        val entries = parseEntries(prefs.getString("entries", "[]"))
        val monthEntries = entries.filter { isCurrentMonth(it.date) }
        val income = monthEntries.filter { it.type == "income" }.sumOf { it.amount }
        val spent = monthEntries.filter { it.type == "expense" }.sumOf { it.amount }
        val available = monthlyBudget + income - spent
        val dueCount = parseDueItems(prefs.getString("future", "[]")).count {
            runCatching { LocalDate.parse(it.date) <= LocalDate.now().plusDays(7) }.getOrDefault(false)
        }
        val progress = if (monthlyBudget > 0) ((spent / monthlyBudget) * 100).toInt().coerceIn(0, 100) else 0

        return RemoteViews(context.packageName, R.layout.widget_summary).apply {
            setTextViewText(R.id.widget_title, firstName(userName))
            setTextViewText(R.id.widget_amount, moneyText(prefs, available))
            setTextViewText(R.id.widget_spent, "Gasto: ${moneyText(prefs, spent)}")
            setTextViewText(R.id.widget_budget, "Base: ${moneyText(prefs, monthlyBudget)}")
            setTextViewText(R.id.widget_due, "$dueCount vencimento(s) em breve")
            setTextColor(R.id.widget_title, Color.WHITE)
            setTextColor(R.id.widget_amount, Color.WHITE)
            setTextColor(R.id.widget_spent, Color.parseColor("#B8C1CC"))
            setTextColor(R.id.widget_budget, Color.parseColor("#B8C1CC"))
            setTextColor(R.id.widget_due, Color.parseColor("#EEF3F7"))
            setProgressBar(R.id.widget_progress, 100, progress, false)
            setInt(R.id.widget_accent, "setBackgroundColor", accent)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        }
    }

    fun agendaRemoteViews(context: Context): RemoteViews {
        FinanzaPreferences.repairLegacyTypes(context)
        val prefs = FinanzaPreferences.get(context)
        val dueItems = parseDueItems(prefs.getString("future", "[]"))
            .sortedBy { it.date }
        val nextDue = dueItems.firstOrNull()
        val pending = dueItems.count {
            runCatching { LocalDate.parse(it.date) >= LocalDate.now() }.getOrDefault(false)
        }

        return RemoteViews(context.packageName, R.layout.widget_agenda).apply {
            setTextViewText(R.id.widget_agenda_title, "Agenda financeira")
            setTextViewText(R.id.widget_agenda_count, "$pending item(ns) futuros")
            setTextViewText(R.id.widget_agenda_name, nextDue?.name ?: "Sem contas na agenda")
            setTextViewText(
                R.id.widget_agenda_meta,
                if (nextDue != null) "${formatDate(nextDue.date)} - ${moneyText(prefs, nextDue.amount)}" else "Adicione vencimentos no app"
            )
            setTextViewText(R.id.widget_agenda_category, nextDue?.category ?: "Finext")
            setTextColor(R.id.widget_agenda_title, Color.WHITE)
            setTextColor(R.id.widget_agenda_count, Color.parseColor("#B8C1CC"))
            setTextColor(R.id.widget_agenda_name, Color.WHITE)
            setTextColor(R.id.widget_agenda_meta, Color.parseColor("#B8C1CC"))
            setTextColor(R.id.widget_agenda_category, Color.parseColor("#08110B"))
            setInt(R.id.widget_agenda_category, "setBackgroundColor", Color.parseColor("#F4CF45"))
            setOnClickPendingIntent(R.id.widget_agenda_root, openAppIntent(context))
        }
    }

    private fun openAppIntent(context: Context, requestCode: Int = 1001): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun openQuickExpenseIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = QuickExpenseShortcutActivity.createIntent(context)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun inlineQuickExpenseIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, FinanzaQuickExpenseInlineReceiver::class.java).apply {
            action = ACTION_QUICK_EXPENSE_INLINE
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun handleInlineQuickExpense(context: Context, rawText: String): Boolean {
        val parsed = parseInlineQuickExpense(rawText) ?: return false
        val prefs = context.getSharedPreferences("finanza_next_native", Context.MODE_PRIVATE)
        val accountId = defaultAccountId(prefs)
        val category = inferCategory(prefs, parsed.description)
        val localId = System.currentTimeMillis()
        val array = JSONArray(prefs.getString("entries", "[]"))
        array.put(org.json.JSONObject().apply {
            put("id", localId)
            put("title", parsed.description)
            put("category", category)
            put("amount", parsed.amount)
            put("type", "expense")
            put("date", LocalDate.now().toString())
            put("accountId", accountId)
        })
        prefs.edit().putString("entries", array.toString()).apply()
        syncInlineQuickExpenseAsync(prefs, localId, parsed.amount, parsed.description, category, accountId)
        updateAll(context)
        showQuickExpenseNotification(context)
        return true
    }

    private fun parseInlineQuickExpense(rawText: String): InlineQuickExpense? {
        val text = rawText.trim()
        if (text.isBlank()) return null
        val match = Regex("""^(?:R\$\s*)?([0-9][0-9.\,]*)\s+(.+)$""", RegexOption.IGNORE_CASE).find(text) ?: return null
        val amount = parseFlexibleAmount(match.groupValues[1]) ?: return null
        val description = match.groupValues[2].trim().ifBlank { "Gasto rapido" }
        if (amount <= 0) return null
        return InlineQuickExpense(amount, description)
    }

    private fun parseFlexibleAmount(raw: String): Double? {
        val cleaned = raw.trim().replace(" ", "")
        val normalized = if (cleaned.contains(",")) cleaned.replace(".", "").replace(",", ".") else cleaned
        return normalized.toDoubleOrNull()
    }

    private fun moneyText(prefs: android.content.SharedPreferences, value: Double): String =
        if (prefs.getBoolean("privacy_mode", false)) "R$ --" else money.format(value)

    private fun defaultAccountId(prefs: android.content.SharedPreferences): String {
        val accounts = runCatching { JSONArray(prefs.getString("accounts", "[]")) }.getOrDefault(JSONArray())
        return accounts.optJSONObject(0)?.optString("id").orEmpty()
    }

    private fun defaultAccountSummary(prefs: android.content.SharedPreferences): String {
        val accounts = runCatching { JSONArray(prefs.getString("accounts", "[]")) }.getOrDefault(JSONArray())
        val first = accounts.optJSONObject(0) ?: JSONObject()
        val name = first.optString("name").ifBlank { "Principal" }
        return name
    }

    private fun inferCategory(prefs: android.content.SharedPreferences, title: String): String {
        val normalized = title.trim().lowercase()
        if (normalized.isBlank()) return "A classificar"
        val entries = runCatching { JSONArray(prefs.getString("entries", "[]")) }.getOrDefault(JSONArray())
        for (index in entries.length() - 1 downTo 0) {
            val item = entries.optJSONObject(index) ?: continue
            if (item.optString("type") != "expense") continue
            if (item.optString("title").trim().lowercase() == normalized) {
                return FinanzaCategories.infer(title, item.optString("category"))
            }
        }
        return FinanzaCategories.infer(title)
    }

    private fun syncInlineQuickExpenseAsync(
        prefs: android.content.SharedPreferences,
        localId: Long,
        amount: Double,
        title: String,
        category: String,
        accountId: String
    ) {
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
        kotlin.concurrent.thread {
            runCatching { FinanzaRemoteRepository(client).createTransaction(payload) }
                .onFailure {
                    FinanzaSyncQueue.enqueueRequest(prefs, "POST", FinanzaApiRoutes.TRANSACTIONS, payload, localId)
                }
        }
    }

    private fun accentColor(id: String): Int = when (id) {
        "yellow" -> Color.parseColor("#F4CF45")
        "blue" -> Color.parseColor("#5682FF")
        "violet" -> Color.parseColor("#A07EFF")
        "coral" -> Color.parseColor("#FF7E62")
        else -> Color.parseColor("#35C96F")
    }

    private fun firstName(name: String): String = name.trim().split(" ").firstOrNull().orEmpty().ifBlank { "Voce" }

    private fun isCurrentMonth(date: String): Boolean = runCatching {
        val parsed = LocalDate.parse(date)
        parsed.month == LocalDate.now().month && parsed.year == LocalDate.now().year
    }.getOrDefault(false)

    private fun formatDate(date: String): String = runCatching {
        LocalDate.parse(date).format(shortDate).replaceFirstChar { if (it.isLowerCase()) it.titlecase(ptBr) else it.toString() }
    }.getOrDefault(date)

    private fun parseEntries(raw: String?): List<WidgetEntry> {
        val array = runCatching { JSONArray(raw ?: "[]") }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    WidgetEntry(
                        amount = item.optDouble("amount", 0.0),
                        type = item.optString("type", "expense"),
                        date = item.optString("date", LocalDate.now().toString()).take(10)
                    )
                )
            }
        }
    }

    private fun parseDueItems(raw: String?): List<WidgetDueItem> {
        val array = runCatching { JSONArray(raw ?: "[]") }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    WidgetDueItem(
                        name = item.optString("name", item.optString("title", "Conta")),
                        category = item.optString("category", "Geral"),
                        amount = item.optDouble("amount", 0.0),
                        date = item.optString("date", LocalDate.now().toString()).take(10)
                    )
                )
            }
        }
    }

    private data class WidgetEntry(
        val amount: Double,
        val type: String,
        val date: String
    )

    private data class WidgetDueItem(
        val name: String,
        val category: String,
        val amount: Double,
        val date: String
    )

    private data class InlineQuickExpense(
        val amount: Double,
        val description: String
    )
}

class FinanzaSummaryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, FinanzaWidgets.summaryRemoteViews(context)) }
    }
}

class FinanzaQuickExpenseWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, FinanzaWidgets.quickExpenseRemoteViews(context)) }
    }
}

class FinanzaAgendaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, FinanzaWidgets.agendaRemoteViews(context)) }
    }
}

class FinanzaQuickExpenseInlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != FinanzaWidgets.ACTION_QUICK_EXPENSE_INLINE) return
        val results = RemoteInput.getResultsFromIntent(intent)
        val text = results?.getCharSequence("quick_expense_text")?.toString().orEmpty()
        val ok = FinanzaWidgets.handleInlineQuickExpense(context, text)
        val message = if (ok) "Gasto salvo pela notificacao." else "Use: valor descricao. Ex.: 34 mercado"
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
