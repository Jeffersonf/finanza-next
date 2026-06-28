package com.finanza.next

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("finanza_next_native", MODE_PRIVATE) }
    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val shortDate = DateTimeFormatter.ofPattern("dd MMM", Locale("pt", "BR"))

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private lateinit var scroll: ScrollView

    private var activeTab = "home"
    private var accentId = "green"
    private var accent = Color.rgb(53, 201, 111)

    private val bg = Color.rgb(243, 243, 245)
    private val paper = Color.WHITE
    private val ink = Color.rgb(9, 9, 9)
    private val dark = Color.rgb(25, 25, 25)
    private val dark2 = Color.rgb(37, 37, 37)
    private val muted = Color.rgb(137, 137, 145)
    private val soft = Color.rgb(236, 236, 239)
    private val line = Color.rgb(228, 228, 231)
    private val danger = Color.rgb(238, 86, 104)

    private data class Accent(val id: String, val name: String, val color: Int)
    private data class Entry(
        val id: Long,
        val title: String,
        val category: String,
        val amount: Double,
        val type: String,
        val date: String
    )
    private data class Tab(val id: String, val label: String, val icon: String)

    private val accents = listOf(
        Accent("green", "Verde", Color.rgb(53, 201, 111)),
        Accent("yellow", "Amarelo", Color.rgb(244, 207, 69)),
        Accent("blue", "Azul", Color.rgb(91, 140, 255)),
        Accent("violet", "Lilás", Color.rgb(177, 140, 255)),
        Accent("coral", "Coral", Color.rgb(255, 127, 102))
    )

    private val tabs = listOf(
        Tab("accounts", "Contas", "▤"),
        Tab("home", "Home", "⌂"),
        Tab("analysis", "Análise", "▥"),
        Tab("settings", "Config", "⚙")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureSystemBars()
        accentId = prefs.getString("accent", "green") ?: "green"
        accent = accents.firstOrNull { it.id == accentId }?.color ?: accents.first().color
        seedIfEmpty()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(12), dp(16), dp(10))
        }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(24))
        }
        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = rounded(Color.argb(247, 255, 255, 255), dp(22), line)
            elevation = dp(10).toFloat()
        }
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(nav, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(68)))
        setContentView(root)
        render()
    }

    private fun configureSystemBars() {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) window.navigationBarDividerColor = bg
    }

    private fun render() {
        content.removeAllViews()
        renderPageHeader()
        when (activeTab) {
            "accounts" -> accountsScreen()
            "analysis" -> analysisScreen()
            "settings" -> settingsScreen()
            else -> homeScreen()
        }
        renderNav()
        scroll.post { scroll.scrollTo(0, 0) }
    }

    private fun renderPageHeader() {
        val title = when (activeTab) {
            "accounts" -> "Contas"
            "analysis" -> "Análise"
            "settings" -> "Configurações"
            else -> greeting()
        }
        val subtitle = when (activeTab) {
            "accounts" -> "Próximos compromissos"
            "analysis" -> "Seu mês em perspectiva"
            "settings" -> "Conta, dados e aparência"
            else -> prefs.getString("user_name", "Você") ?: "Você"
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            setPadding(dp(5), dp(8), dp(5), dp(18))
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(body(title, 13, muted))
                addView(heading(subtitle, if (activeTab == "home") 38 else 34))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(iconButton("•••", "Abrir todos os módulos") { showModules() })
            addView(iconButton("+", "Nova transação") { showEntryDialog("expense") }.apply {
                setTextColor(Color.WHITE)
                background = rounded(dark, dp(17), Color.TRANSPARENT)
            }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(dp(8), 0, 0, 0) })
        }
        content.addView(header)
    }

    private fun homeScreen() {
        val monthEntries = entries().filter { isCurrentMonth(it.date) }
        val income = monthEntries.filter { it.type == "income" }.sumOf { it.amount }
        val spent = monthEntries.filter { it.type == "expense" }.sumOf { it.amount }
        val budget = prefs.getFloat("monthly_budget", 5000f).toDouble()
        val available = budget + income - spent
        val progress = if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0

        content.addView(budgetHero(available, spent, budget, progress))
        content.addView(quickActions())
        content.addView(sectionTitle("Últimas transações", "Ver todas") { showHistoryDialog() })
        val recent = entries().sortedByDescending { it.date }.take(5)
        if (recent.isEmpty()) content.addView(emptyCard("Nenhuma transação", "Use Gasto ou Receita para começar."))
        else recent.forEach { content.addView(entryRow(it)) }

        val categories = monthEntries.filter { it.type == "expense" }
            .groupBy { it.category }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
        content.addView(sectionTitle("Resumo rápido"))
        content.addView(twoInsights(
            "Maior categoria", categories.firstOrNull()?.first ?: "Sem gastos", categories.firstOrNull()?.second?.let(money::format) ?: "Tudo sob controle",
            "Média diária", money.format(spent / LocalDate.now().dayOfMonth.coerceAtLeast(1)), "${LocalDate.now().lengthOfMonth() - LocalDate.now().dayOfMonth} dias restantes"
        ))
    }

    private fun budgetHero(available: Double, spent: Double, budget: Double, progress: Int): View = card(dark, Color.TRANSPARENT, 30) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(label("ORÇAMENTO MENSAL", 11, Color.argb(145, 255, 255, 255), true), LinearLayout.LayoutParams(0, dp(20), 1f))
            addView(label(monthLabel(), 11, Color.WHITE, true))
        })
        addView(label(money.format(available), 42, Color.WHITE, true).apply {
            setPadding(0, dp(22), 0, dp(10))
            letterSpacing = -.055f
            contentDescription = "Disponível: ${money.format(available)}"
        })
        addView(ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            this.progress = progress
            max = 100
            progressTintList = ColorStateList.valueOf(accent)
            progressBackgroundTintList = ColorStateList.valueOf(dark2)
            contentDescription = "$progress por cento do orçamento utilizado"
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(7)).apply { setMargins(0, 0, 0, dp(18)) })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(metric("Gasto", money.format(spent), false), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(metric("Orçamento mensal", money.format(budget), true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
    }

    private fun quickActions(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(8))
        }
        listOf(
            Triple("+", "Gasto") { showEntryDialog("expense") },
            Triple("↑", "Receita") { showEntryDialog("income") },
            Triple("!", "Conta") { showEntryDialog("due") },
            Triple("⌕", "Buscar") { showHistoryDialog() }
        ).forEachIndexed { index, item ->
            row.addView(actionTile(item.first, item.second, index == 0, item.third), LinearLayout.LayoutParams(0, dp(86), 1f).apply {
                if (index < 3) setMargins(0, 0, dp(8), 0)
            })
        }
        return row
    }

    private fun accountsScreen() {
        val due = futureEntries().sortedBy { it.date }
        val total = due.sumOf { it.amount }
        content.addView(card(dark, Color.TRANSPARENT, 28) {
            addView(label("PRÓXIMOS 30 DIAS", 11, Color.argb(145, 255, 255, 255), true))
            addView(label(money.format(total), 38, Color.WHITE, true).apply { setPadding(0, dp(14), 0, dp(4)) })
            addView(body("${due.size} contas programadas", 13, Color.argb(170, 255, 255, 255)))
        })
        content.addView(primaryButton("+ Adicionar conta") { showEntryDialog("due") })
        content.addView(sectionTitle("Agenda financeira"))
        if (due.isEmpty()) content.addView(emptyCard("Agenda limpa", "Nenhum vencimento cadastrado."))
        else due.forEach { content.addView(entryRow(it)) }
    }

    private fun analysisScreen() {
        val month = entries().filter { isCurrentMonth(it.date) }
        val income = month.filter { it.type == "income" }.sumOf { it.amount }
        val spent = month.filter { it.type == "expense" }.sumOf { it.amount }
        val savings = income - spent
        content.addView(card(dark, Color.TRANSPARENT, 28) {
            addView(label("SALDO DO MÊS", 11, Color.argb(145, 255, 255, 255), true))
            addView(label(money.format(savings), 39, Color.WHITE, true).apply { setPadding(0, dp(14), 0, dp(18)) })
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(metric("Entradas", money.format(income), false), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(metric("Saídas", money.format(spent), true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            })
        })
        content.addView(sectionTitle("Por categoria"))
        val grouped = month.filter { it.type == "expense" }.groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }.toList().sortedByDescending { it.second }
        if (grouped.isEmpty()) content.addView(emptyCard("Ainda sem dados", "Seus gastos aparecerão aqui por categoria."))
        grouped.take(8).forEachIndexed { index, pair ->
            val percent = if (spent > 0) (pair.second / spent * 100).toInt() else 0
            content.addView(analysisRow(pair.first, money.format(pair.second), percent, index))
        }
    }

    private fun settingsScreen() {
        content.addView(settingsCard("Seu perfil", "Nome usado na saudação", onClick = {
            showTextSetting("Seu nome", prefs.getString("user_name", "Você") ?: "Você") {
                prefs.edit().putString("user_name", it.ifBlank { "Você" }).apply(); render()
            }
        }))
        content.addView(settingsCard("Orçamento mensal", money.format(prefs.getFloat("monthly_budget", 5000f).toDouble()), onClick = {
            showTextSetting("Orçamento mensal", prefs.getFloat("monthly_budget", 5000f).toString(), true) {
                val value = it.replace(",", ".").toFloatOrNull()
                if (value != null && value > 0) prefs.edit().putFloat("monthly_budget", value).apply()
                render()
            }
        }))
        content.addView(sectionTitle("Cor de progresso"))
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        accents.forEach { option ->
            row.addView(TextView(this).apply {
                text = option.name
                gravity = Gravity.CENTER
                setTextColor(if (option.id == accentId) ink else muted)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                minWidth = dp(88)
                contentDescription = "Usar destaque ${option.name}"
                isSelected = option.id == accentId
                background = rounded(if (option.id == accentId) option.color else paper, dp(18), line)
                setOnClickListener {
                    accentId = option.id; accent = option.color
                    prefs.edit().putString("accent", accentId).apply(); render()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(52)).apply { setMargins(0, 0, dp(8), dp(12)) })
        }
        scroller.addView(row)
        content.addView(scroller)
        content.addView(settingsCard("Todos os módulos", "Metas, compras, carro e compromissos", onClick = { showModules() }))
        content.addView(settingsCard("Apagar dados locais", "Remove os lançamentos deste aparelho", onClick = { confirmReset() }, titleColor = danger))
        content.addView(body("Finanza Next Android 1.0", 11, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, dp(12))
        })
    }

    private fun renderNav() {
        nav.removeAllViews()
        tabs.forEach { tab ->
            val selected = tab.id == activeTab
            nav.addView(TextView(this).apply {
                text = "${tab.icon}\n${tab.label}"
                gravity = Gravity.CENTER
                setTextColor(if (selected) Color.WHITE else Color.rgb(95, 95, 100))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setLineSpacing(0f, .9f)
                contentDescription = tab.label
                isSelected = selected
                minimumHeight = dp(54)
                background = if (selected) rounded(dark, dp(18), Color.TRANSPARENT) else null
                setOnClickListener { activeTab = tab.id; render() }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
    }

    private fun showModules() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(24))
            background = rounded(dark, dp(30), Color.TRANSPARENT)
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(body("Seu espaço", 12, Color.argb(130, 255, 255, 255)))
                    addView(label("Todos os módulos", 27, Color.WHITE, true))
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(iconButton("×", "Fechar") { dialog.dismiss() }.apply {
                    setTextColor(Color.WHITE); background = rounded(dark2, dp(18), Color.TRANSPARENT)
                })
            })
        }
        listOf(
            Triple("↕", "Transações", "Histórico completo"),
            Triple("▤", "Compromissos", "Assinaturas e dívidas"),
            Triple("◎", "Metas", "Objetivos e progresso"),
            Triple("✓", "Compras", "Listas planejadas"),
            Triple("◇", "Carro", "Combustível e manutenção")
        ).forEachIndexed { index, item ->
            panel.addView(moduleButton(item.first, item.second, item.third) {
                dialog.dismiss()
                when (index) {
                    0 -> showHistoryDialog()
                    1 -> { activeTab = "accounts"; render() }
                    2 -> showInfo("Metas", "Acompanhe objetivos e reserve valores sem misturar com o orçamento diário.")
                    3 -> showInfo("Compras", "Organize mercado, casa e desejos antes de transformar itens em gastos.")
                    else -> showInfo("Carro", "Centralize combustível, seguro, impostos e manutenção preventiva.")
                }
            })
        }
        dialog.setContentView(panel)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.BOTTOM)
            attributes = attributes.apply { width = WindowManager.LayoutParams.MATCH_PARENT }
        }
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showEntryDialog(type: String, existing: Entry? = null) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(24))
            background = rounded(dark, dp(30), Color.TRANSPARENT)
        }
        val titleInput = input("Descrição", existing?.title ?: "")
        val categoryInput = input("Categoria", existing?.category ?: "")
        val amountInput = input("Valor", existing?.amount?.toString() ?: "", true)
        val dateInput = input("Data (AAAA-MM-DD)", existing?.date ?: LocalDate.now().toString())
        panel.addView(label(if (existing != null) "Editar lançamento" else when (type) {
            "income" -> "Nova receita"; "due" -> "Nova conta"; else -> "Novo gasto"
        }, 27, Color.WHITE, true))
        panel.addView(body("Registre em poucos segundos.", 13, Color.argb(145, 255, 255, 255)))
        panel.addView(titleInput); panel.addView(categoryInput); panel.addView(amountInput); panel.addView(dateInput)
        panel.addView(primaryButton("Salvar") {
            val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull()
            val date = dateInput.text.toString().ifBlank { LocalDate.now().toString() }
            if (titleInput.text.isBlank() || amount == null || amount <= 0 || !validDate(date)) {
                Toast.makeText(this, "Confira descrição, valor e data.", Toast.LENGTH_SHORT).show()
                return@primaryButton
            }
            val item = Entry(existing?.id ?: System.currentTimeMillis(), titleInput.text.toString().trim(), categoryInput.text.toString().ifBlank { "Geral" }.trim(), amount, type, date)
            if (existing == null) saveEntry(item) else replaceEntry(existing, item)
            dialog.dismiss(); render()
        })
        if (existing != null) panel.addView(dangerButton("Excluir") { deleteEntry(existing); dialog.dismiss(); render() })
        dialog.setContentView(panel)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun showHistoryDialog() {
        val all = (entries() + futureEntries()).sortedByDescending { it.date }
        val options = all.map { "${it.title}  ·  ${money.format(it.amount)}" }.toTypedArray()
        if (options.isEmpty()) { showInfo("Histórico", "Nenhum lançamento cadastrado."); return }
        AlertDialog.Builder(this)
            .setTitle("Histórico")
            .setItems(options) { _, which -> showEntryDialog(all[which].type, all[which]) }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun showTextSetting(title: String, value: String, numeric: Boolean = false, onSave: (String) -> Unit) {
        val input = EditText(this).apply {
            setText(value); selectAll(); setPadding(dp(16), dp(12), dp(16), dp(12))
            if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        AlertDialog.Builder(this).setTitle(title).setView(input)
            .setPositiveButton("Salvar") { _, _ -> onSave(input.text.toString()) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showInfo(title: String, copy: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(copy).setPositiveButton("Entendi", null).show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this).setTitle("Apagar dados locais?")
            .setMessage("Esta ação remove lançamentos e contas somente deste aparelho.")
            .setPositiveButton("Apagar") { _, _ ->
                prefs.edit().remove("entries").remove("future").apply(); seedIfEmpty(); render()
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun entries(): List<Entry> = load("entries")
    private fun futureEntries(): List<Entry> = load("future")

    private fun saveEntry(entry: Entry) {
        val key = if (entry.type == "due") "future" else "entries"
        val array = JSONArray(prefs.getString(key, "[]"))
        array.put(entryJson(entry)); prefs.edit().putString(key, array.toString()).apply()
    }

    private fun replaceEntry(old: Entry, updated: Entry) {
        deleteEntry(old); saveEntry(updated)
    }

    private fun deleteEntry(entry: Entry) {
        val key = if (entry.type == "due") "future" else "entries"
        val filtered = load(key).filterNot { it.id == entry.id }
        val array = JSONArray(); filtered.forEach { array.put(entryJson(it)) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun entryJson(entry: Entry) = JSONObject().apply {
        put("id", entry.id); put("title", entry.title); put("category", entry.category)
        put("amount", entry.amount); put("type", entry.type); put("date", entry.date)
    }

    private fun load(key: String): List<Entry> {
        val array = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                Entry(item.optLong("id"), item.optString("title"), item.optString("category"), item.optDouble("amount"), item.optString("type"), item.optString("date"))
            }.getOrNull()
        }
    }

    private fun seedIfEmpty() {
        if (!prefs.contains("entries")) {
            prefs.edit().putString("entries", "[]").apply()
            listOf(
                Entry(1, "Salário", "Entrada", 5500.0, "income", LocalDate.now().minusDays(2).toString()),
                Entry(2, "Mercado", "Casa", 286.75, "expense", LocalDate.now().toString()),
                Entry(3, "Internet", "Assinaturas", 119.9, "expense", LocalDate.now().minusDays(1).toString())
            ).forEach(::saveEntry)
        }
        if (!prefs.contains("future")) {
            prefs.edit().putString("future", "[]").apply()
            listOf(
                Entry(4, "Aluguel", "Moradia", 1280.0, "due", LocalDate.now().plusDays(3).toString()),
                Entry(5, "Cartão", "Fatura", 860.0, "due", LocalDate.now().plusDays(8).toString())
            ).forEach(::saveEntry)
        }
    }

    private fun entryRow(entry: Entry): View = card(paper, line, 21) {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        val tone = when (entry.type) { "income" -> accent; "due" -> Color.rgb(244, 207, 69); else -> dark }
        addView(label(when (entry.type) { "income" -> "+"; "due" -> "!"; else -> "−" }, 20, if (entry.type == "expense") Color.WHITE else ink, true).apply {
            gravity = Gravity.CENTER; background = rounded(tone, dp(14), Color.TRANSPARENT)
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { setMargins(0, 0, dp(12), 0) })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(entry.title, 14, ink, true))
            addView(body("${entry.category} · ${formatDate(entry.date)}", 11, muted))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label((if (entry.type == "income") "+ " else "") + money.format(abs(entry.amount)), 13, if (entry.type == "income") Color.rgb(20, 146, 73) else ink, true))
        isClickable = true; isFocusable = true
        contentDescription = "${entry.title}, ${entry.category}, ${money.format(entry.amount)}, ${formatDate(entry.date)}. Toque para editar."
        setOnClickListener { showEntryDialog(entry.type, entry) }
    }

    private fun analysisRow(name: String, value: String, percent: Int, index: Int): View = card(paper, line, 20) {
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(label(name, 14, ink, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(label(value, 13, ink, true))
        })
        addView(ProgressBar(this@MainActivity, null, android.R.attr.progressBarStyleHorizontal).apply {
            progress = percent; max = 100
            progressTintList = ColorStateList.valueOf(if (index == 0) accent else dark)
            progressBackgroundTintList = ColorStateList.valueOf(soft)
            contentDescription = "$name representa $percent por cento dos gastos"
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(6)).apply { setMargins(0, dp(12), 0, 0) })
    }

    private fun twoInsights(k1: String, v1: String, s1: String, k2: String, v2: String, s2: String): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(insight(k1, v1, s1), LinearLayout.LayoutParams(0, dp(120), 1f).apply { setMargins(0, 0, dp(8), 0) })
        row.addView(insight(k2, v2, s2), LinearLayout.LayoutParams(0, dp(120), 1f))
        return row
    }

    private fun insight(kicker: String, value: String, sub: String) = card(paper, line, 20) {
        addView(body(kicker, 11, muted)); addView(label(value, 17, ink, true).apply { setPadding(0, dp(5), 0, dp(4)) }); addView(body(sub, 11, muted))
    }

    private fun settingsCard(title: String, copy: String, onClick: () -> Unit, titleColor: Int = ink): View = card(paper, line, 20) {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL; addView(label(title, 15, titleColor, true)); addView(body(copy, 12, muted))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label("›", 26, muted, false))
        isClickable = true; isFocusable = true; minimumHeight = dp(72); setOnClickListener { onClick() }
    }

    private fun sectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(5), dp(22), dp(5), dp(10))
        addView(label(text, 19, ink, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        if (action != null) addView(TextView(this@MainActivity).apply {
            this.text = action; setTextColor(muted); textSize = 12f; gravity = Gravity.CENTER; minHeight = dp(48); minWidth = dp(64)
            setOnClickListener { onAction?.invoke() }
        })
    }

    private fun actionTile(icon: String, text: String, highlighted: Boolean, onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        background = rounded(paper, dp(19), Color.TRANSPARENT); elevation = dp(2).toFloat()
        addView(label(icon, 18, ink, true).apply {
            gravity = Gravity.CENTER; background = rounded(if (highlighted) accent else soft, dp(12), Color.TRANSPARENT)
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        addView(label(text, 10, ink, true).apply { setPadding(0, dp(6), 0, 0) })
        contentDescription = text; isClickable = true; isFocusable = true; setOnClickListener { onClick() }
    }

    private fun moduleButton(icon: String, title: String, sub: String, onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(dark2, dp(20), Color.TRANSPARENT); isClickable = true; isFocusable = true; contentDescription = "$title. $sub"
        addView(label(icon, 19, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = rounded(Color.argb(18, 255, 255, 255), dp(14), Color.TRANSPARENT) }, LinearLayout.LayoutParams(dp(46), dp(46)).apply { setMargins(0, 0, dp(12), 0) })
        addView(LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; addView(label(title, 15, Color.WHITE, true)); addView(body(sub, 11, Color.argb(125, 255, 255, 255))) }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label("›", 25, Color.argb(120, 255, 255, 255), false)); setOnClickListener { onClick() }
    }.also { it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)).apply { setMargins(0, dp(9), 0, 0) } }

    private fun metric(kicker: String, value: String, right: Boolean): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = if (right) Gravity.END else Gravity.START
        addView(body(kicker, 10, Color.argb(120, 255, 255, 255))); addView(label(value, 14, Color.WHITE, true))
    }

    private fun emptyCard(title: String, copy: String): View = card(paper, Color.TRANSPARENT, 20) {
        gravity = Gravity.CENTER; addView(label(title, 15, ink, true)); addView(body(copy, 12, muted).apply { gravity = Gravity.CENTER })
    }

    private fun card(fill: Int, stroke: Int, radius: Int, children: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); background = rounded(fill, dp(radius), stroke); children()
    }.also { it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 0, 0, dp(10)) } }

    private fun heading(text: String, size: Int): TextView = label(text, size, ink, true).apply { letterSpacing = -.045f }
    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text; setTextColor(color); textSize = size.toFloat(); includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun body(text: String, size: Int, color: Int): TextView = label(text, size, color, false).apply { setLineSpacing(dp(2).toFloat(), 1f) }

    private fun input(hint: String, value: String, numeric: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint; setText(value); setHintTextColor(Color.argb(115, 255, 255, 255)); setTextColor(Color.WHITE); textSize = 15f
        setSingleLine(true); setPadding(dp(14), 0, dp(14), 0); background = rounded(dark2, dp(16), Color.argb(22, 255, 255, 255))
        if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }.also { it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)).apply { setMargins(0, dp(12), 0, 0) } }

    private fun iconButton(text: String, description: String, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text; gravity = Gravity.CENTER; setTextColor(ink); textSize = if (text == "+") 25f else 13f; typeface = Typeface.DEFAULT_BOLD
        contentDescription = description; minimumWidth = dp(48); minimumHeight = dp(48); background = rounded(paper, dp(17), line); setOnClickListener { onClick() }
    }
    private fun primaryButton(text: String, onClick: () -> Unit): TextView = button(text, accent, ink, onClick)
    private fun dangerButton(text: String, onClick: () -> Unit): TextView = button(text, Color.TRANSPARENT, danger, onClick)
    private fun button(text: String, fill: Int, color: Int, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text; gravity = Gravity.CENTER; setTextColor(color); textSize = 14f; typeface = Typeface.DEFAULT_BOLD
        background = rounded(fill, dp(17), if (fill == Color.TRANSPARENT) Color.argb(35, 255, 255, 255) else Color.TRANSPARENT); setOnClickListener { onClick() }
    }.also { it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { setMargins(0, dp(12), 0, 0) } }

    private fun rounded(color: Int, radius: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat(); if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
    }

    private fun greeting(): String = when (java.time.LocalTime.now().hour) { in 0..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    private fun monthLabel(): String = LocalDate.now().month.getDisplayName(java.time.format.TextStyle.SHORT, Locale("pt", "BR")).replace(".", "").uppercase()
    private fun isCurrentMonth(date: String): Boolean = runCatching { LocalDate.parse(date).let { it.month == LocalDate.now().month && it.year == LocalDate.now().year } }.getOrDefault(false)
    private fun validDate(date: String): Boolean = try { LocalDate.parse(date); true } catch (_: DateTimeParseException) { false }
    private fun formatDate(date: String): String = runCatching { LocalDate.parse(date).format(shortDate).replaceFirstChar { it.uppercase() } }.getOrDefault(date)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
