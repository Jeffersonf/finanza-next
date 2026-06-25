package com.finanza.next

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("finanza_next_native", MODE_PRIVATE) }
    private val money = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM")

    private lateinit var root: LinearLayout
    private lateinit var header: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout

    private var activeTab = "home"
    private var accentId = "amber"
    private var accent = Color.rgb(245, 233, 95)

    private val bg = Color.rgb(8, 8, 8)
    private val surface = Color.rgb(21, 21, 21)
    private val surface2 = Color.rgb(29, 29, 29)
    private val ink = Color.rgb(245, 242, 234)
    private val muted = Color.argb(150, 245, 242, 234)
    private val line = Color.argb(24, 255, 255, 255)
    private val green = Color.rgb(158, 217, 168)
    private val danger = Color.rgb(255, 135, 122)
    private val violet = Color.rgb(182, 168, 255)

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
        Accent("amber", "Amarelo", Color.rgb(245, 233, 95)),
        Accent("lime", "Lima", Color.rgb(215, 242, 106)),
        Accent("mint", "Menta", Color.rgb(120, 232, 197)),
        Accent("blue", "Azul", Color.rgb(128, 184, 255)),
        Accent("violet", "Violeta", Color.rgb(182, 168, 255)),
        Accent("coral", "Coral", Color.rgb(255, 155, 121))
    )

    private val tabs = listOf(
        Tab("home", "Início", "●"),
        Tab("transactions", "Gastos", "◆"),
        Tab("future", "Venc.", "◼"),
        Tab("goals", "Metas", "▲"),
        Tab("shopping", "Compras", "◌"),
        Tab("car", "Carro", "▰"),
        Tab("settings", "Ajustes", "⚙")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        accentId = prefs.getString("accent", "amber") ?: "amber"
        accent = accents.firstOrNull { it.id == accentId }?.color ?: accents.first().color
        seedIfEmpty()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(dp(16), dp(24), dp(16), dp(12))
        }

        header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(12))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(20))
        }

        nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(6), dp(6), dp(6))
            background = rounded(surface, dp(22), line)
        }

        scroll.addView(content)
        root.addView(header, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(nav, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        setContentView(root)
        render()
    }

    private fun render() {
        renderHeader()
        renderContent()
        renderNav()
    }

    private fun renderHeader() {
        header.removeAllViews()
        header.addView(label("FINANZA NEXT", 11, accent, true).apply { letterSpacing = .16f })
        header.addView(title(tabTitle(), 34).apply { setPadding(0, dp(4), 0, 0) })
        header.addView(body(tabSubtitle(), 13).apply { setPadding(0, dp(6), 0, 0) })
    }

    private fun renderContent() {
        content.removeAllViews()
        when (activeTab) {
            "transactions" -> transactionsScreen()
            "future" -> futureScreen()
            "goals" -> goalsScreen()
            "shopping" -> simpleModule("Lista de compras", "Separe compras planejadas, recorrentes e impulsos antes de virar gasto.", listOf("Mercado da semana", "Farmácia", "Itens para casa"))
            "car" -> simpleModule("Garagem financeira", "Controle combustível, seguro, IPVA e manutenção em uma visão só.", listOf("Combustível", "Seguro", "Manutenção preventiva"))
            "settings" -> settingsScreen()
            else -> homeScreen()
        }
    }

    private fun renderNav() {
        nav.removeAllViews()
        tabs.forEach { tab ->
            val active = tab.id == activeTab
            val button = TextView(this).apply {
                text = "${tab.icon}\n${tab.label}"
                gravity = Gravity.CENTER
                setTextColor(if (active) Color.rgb(10, 10, 9) else muted)
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(4), dp(8), dp(4), dp(7))
                background = if (active) rounded(accent, dp(17), Color.TRANSPARENT) else null
                setOnClickListener {
                    activeTab = tab.id
                    render()
                }
            }
            nav.addView(button, LinearLayout.LayoutParams(0, dp(56), 1f))
        }
    }

    private fun homeScreen() {
        val entries = entries()
        val income = entries.filter { it.type == "income" }.sumOf { it.amount }
        val expense = entries.filter { it.type == "expense" }.sumOf { it.amount }
        val balance = income - expense

        content.addView(heroCard("Saldo previsto", money.format(balance), "Entradas ${money.format(income)} • saídas ${money.format(expense)}"))
        content.addView(actionRow())
        content.addView(section("O que acompanhar hoje"))
        content.addView(metricGrid(
            listOf(
                "Contas vencendo" to "${futureEntries().size}",
                "Gastos do mês" to money.format(expense),
                "Meta ativa" to "Viagem"
            )
        ))
        content.addView(section("Últimos lançamentos"))
        entries.takeLast(4).reversed().forEach { content.addView(entryRow(it)) }
    }

    private fun transactionsScreen() {
        content.addView(actionRow())
        content.addView(section("Histórico"))
        entries().reversed().forEach { content.addView(entryRow(it)) }
    }

    private fun futureScreen() {
        val due = futureEntries()
        content.addView(card {
            addView(label("PRÓXIMOS 30 DIAS", 11, muted, true))
            addView(title("${due.size} vencimentos", 30))
            addView(body("Contas fixas, boletos e compromissos antes do aperto.", 13))
            addView(primaryButton("+ Vencimento") { showEntryDialog("due") })
        })
        due.forEach { content.addView(entryRow(it, violet)) }
    }

    private fun goalsScreen() {
        content.addView(heroCard("Meta principal", "42%", "Viagem em andamento • ${money.format(4200.0)} de ${money.format(10000.0)}"))
        content.addView(section("Orçamentos"))
        content.addView(metricGrid(listOf("Mercado" to "68%", "Lazer" to "41%", "Transporte" to "57%")))
        content.addView(section("Plano sugerido"))
        content.addView(textCard("Atacar recorrentes", "Revise assinaturas e dívidas pequenas antes de cortar o que te dá clareza no dia a dia."))
    }

    private fun settingsScreen() {
        content.addView(textCard("Visual Next", "O amarelo continua como base do padrão The Box, mas a cor de ação pode mudar sem perder a linguagem escura e modular."))
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        accents.forEach { option ->
            row.addView(TextView(this).apply {
                text = option.name
                setTextColor(if (option.id == accentId) Color.rgb(10, 10, 9) else ink)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(dp(14), 0, dp(14), 0)
                background = rounded(if (option.id == accentId) option.color else surface2, dp(16), line)
                setOnClickListener {
                    accentId = option.id
                    accent = option.color
                    prefs.edit().putString("accent", accentId).apply()
                    render()
                }
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(48)).apply {
                setMargins(0, 0, dp(8), 0)
            })
        }
        scroller.addView(row)
        content.addView(scroller)
        content.addView(textCard("Dados locais", "Esta primeira versão nativa salva lançamentos no aparelho. A próxima camada pode receber sincronização e importação do Finanza web."))
    }

    private fun simpleModule(name: String, copy: String, items: List<String>) {
        content.addView(textCard(name, copy))
        items.forEachIndexed { index, item ->
            content.addView(textCard("${index + 1}. $item", "Módulo nativo preparado para evoluir com campos próprios e histórico."))
        }
    }

    private fun actionRow(): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(18))
        }
        row.addView(primaryButton("+ Gasto") { showEntryDialog("expense") }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(0, 0, dp(8), 0) })
        row.addView(ghostButton("+ Receita") { showEntryDialog("income") }, LinearLayout.LayoutParams(0, dp(48), 1f).apply { setMargins(0, 0, dp(8), 0) })
        row.addView(ghostButton("+ Venc.") { showEntryDialog("due") }, LinearLayout.LayoutParams(0, dp(48), 1f))
        return row
    }

    private fun showEntryDialog(type: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = rounded(surface, dp(28), line)
        }
        val titleInput = input("Descrição")
        val categoryInput = input(if (type == "due") "Categoria / vencimento" else "Categoria")
        val amountInput = input("Valor")
        amountInput.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        panel.addView(title(if (type == "income") "Nova receita" else if (type == "due") "Novo vencimento" else "Novo gasto", 26))
        panel.addView(titleInput)
        panel.addView(categoryInput)
        panel.addView(amountInput)
        panel.addView(primaryButton("Salvar") {
            val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val item = Entry(
                id = System.currentTimeMillis(),
                title = titleInput.text.toString().ifBlank { if (type == "income") "Receita" else "Lançamento" },
                category = categoryInput.text.toString().ifBlank { "Geral" },
                amount = amount,
                type = type,
                date = LocalDate.now().toString()
            )
            saveEntry(item)
            dialog.dismiss()
            render()
        })

        dialog.setContentView(panel)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.show()
    }

    private fun entries(): List<Entry> = load("entries")
    private fun futureEntries(): List<Entry> = load("future")

    private fun saveEntry(entry: Entry) {
        val key = if (entry.type == "due") "future" else "entries"
        val array = JSONArray(prefs.getString(key, "[]"))
        array.put(JSONObject().apply {
            put("id", entry.id)
            put("title", entry.title)
            put("category", entry.category)
            put("amount", entry.amount)
            put("type", entry.type)
            put("date", entry.date)
        })
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun load(key: String): List<Entry> {
        val array = JSONArray(prefs.getString(key, "[]"))
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            Entry(
                item.optLong("id"),
                item.optString("title"),
                item.optString("category"),
                item.optDouble("amount"),
                item.optString("type"),
                item.optString("date")
            )
        }
    }

    private fun seedIfEmpty() {
        if (!prefs.contains("entries")) {
            listOf(
                Entry(1, "Salário", "Entrada", 5500.0, "income", LocalDate.now().minusDays(2).toString()),
                Entry(2, "Mercado", "Casa", 286.75, "expense", LocalDate.now().toString()),
                Entry(3, "Internet", "Fixo", 119.9, "expense", LocalDate.now().minusDays(1).toString())
            ).forEach { saveEntry(it) }
        }
        if (!prefs.contains("future")) {
            listOf(
                Entry(4, "Aluguel", "Moradia", 1280.0, "due", LocalDate.now().plusDays(3).toString()),
                Entry(5, "Cartão", "Fatura", 860.0, "due", LocalDate.now().plusDays(8).toString())
            ).forEach { saveEntry(it) }
        }
    }

    private fun heroCard(kicker: String, value: String, copy: String): View = card {
        setBackgroundColor(accent)
        addView(label(kicker.uppercase(), 11, Color.argb(150, 0, 0, 0), true))
        addView(TextView(this@MainActivity).apply {
            text = value
            setTextColor(Color.rgb(10, 10, 9))
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = -0.06f
            setPadding(0, dp(8), 0, dp(6))
        })
        addView(body(copy, 13, Color.argb(170, 0, 0, 0)))
    }

    private fun metricGrid(items: List<Pair<String, String>>): View {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        items.forEachIndexed { index, item ->
            row.addView(card {
                addView(label(item.first.uppercase(), 9, muted, true))
                addView(title(item.second, 20))
            }, LinearLayout.LayoutParams(0, dp(116), 1f).apply {
                if (index < items.lastIndex) setMargins(0, 0, dp(8), 0)
            })
        }
        return row
    }

    private fun entryRow(entry: Entry, tint: Int = if (entry.type == "income") green else danger): View = card {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val badge = TextView(this@MainActivity).apply {
            text = if (entry.type == "income") "+" else if (entry.type == "due") "!" else "−"
            setTextColor(Color.rgb(10, 10, 9))
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = rounded(tint, dp(15), Color.TRANSPARENT)
        }
        addView(badge, LinearLayout.LayoutParams(dp(48), dp(48)).apply { setMargins(0, 0, dp(12), 0) })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(entry.title, 14, ink, true))
            val date = runCatching { LocalDate.parse(entry.date).format(dateFmt) }.getOrDefault(entry.date)
            addView(body("${entry.category} • $date", 11))
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label(money.format(abs(entry.amount)), 14, if (entry.type == "income") green else ink, true))
    }

    private fun textCard(title: String, copy: String): View = card {
        addView(label(title, 16, ink, true))
        addView(body(copy, 13).apply { setPadding(0, dp(7), 0, 0) })
    }

    private fun section(text: String): View = label(text.uppercase(), 11, muted, true).apply {
        letterSpacing = .14f
        setPadding(dp(2), dp(18), 0, dp(10))
    }

    private fun card(children: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(surface, dp(24), line)
        children()
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dp(10))
        }
    }

    private fun title(text: String, size: Int): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ink)
        textSize = size.toFloat()
        typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
        letterSpacing = -0.045f
    }

    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text
        setTextColor(color)
        textSize = size.toFloat()
        if (bold) typeface = Typeface.DEFAULT_BOLD
        includeFontPadding = false
    }

    private fun body(text: String, size: Int, color: Int = muted): TextView = TextView(this).apply {
        this.text = text
        setTextColor(color)
        textSize = size.toFloat()
        setLineSpacing(dp(2).toFloat(), 1f)
    }

    private fun input(hint: String): EditText = EditText(this).apply {
        this.hint = hint
        setHintTextColor(Color.argb(110, 245, 242, 234))
        setTextColor(ink)
        textSize = 15f
        setSingleLine(true)
        setPadding(dp(14), 0, dp(14), 0)
        background = rounded(surface2, dp(16), line)
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply {
            setMargins(0, dp(12), 0, 0)
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): TextView = button(text, accent, Color.rgb(10, 10, 9), onClick)
    private fun ghostButton(text: String, onClick: () -> Unit): TextView = button(text, surface2, ink, onClick)

    private fun button(text: String, fill: Int, color: Int, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(color)
        textSize = 13f
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(fill, dp(16), line)
        setOnClickListener { onClick() }
    }.also {
        if (it.layoutParams == null) {
            it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)).apply {
                setMargins(0, dp(14), 0, 0)
            }
        }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        if (stroke != Color.TRANSPARENT) setStroke(1, stroke)
    }

    private fun tabTitle(): String = when (activeTab) {
        "transactions" -> "Gastos do dia"
        "future" -> "Vencimentos"
        "goals" -> "Metas"
        "shopping" -> "Compras"
        "car" -> "Carro"
        "settings" -> "Ajustes"
        else -> "Seu mês claro"
    }

    private fun tabSubtitle(): String = when (activeTab) {
        "transactions" -> "Registre rápido, revise sem ruído e mantenha o fluxo."
        "future" -> "Tudo que ainda vai pesar no mês antes de virar surpresa."
        "goals" -> "Limites, planos e pequenas decisões que protegem o dinheiro."
        "settings" -> "Personalize a cor-base sem fugir do padrão visual Next."
        else -> "Finanças pessoais com cards diretos, contraste alto e ação rápida."
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
