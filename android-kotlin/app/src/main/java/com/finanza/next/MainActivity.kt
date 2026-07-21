package com.finanza.next

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.CheckBox
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.abs
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.finanza.next.navigation.AppActions
import com.finanza.next.navigation.AppScaffold
import com.finanza.next.navigation.AppUiState
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.AppTab
import com.finanza.next.ui.components.BillStatus
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.screens.CategoryUi
import com.finanza.next.ui.screens.ConfigActions
import com.finanza.next.ui.screens.ConfigUiState
import com.finanza.next.ui.screens.PaymentMethodUi
import com.finanza.next.ui.screens.MonthTrendUi
import com.finanza.next.ui.screens.LoginScreen
import com.finanza.next.ui.components.categoryColor
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.FinanceAppTheme
import com.finanza.next.features.FeatureActions
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.features.FeatureItemUi
import com.finanza.next.features.FeatureModuleUi
import com.finanza.next.features.FeatureMutation
import com.finanza.next.features.FinanzaFeatureStore

private class DialogScrollView(context: Context) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = View.MeasureSpec.getSize(heightMeasureSpec)
        val maximum = (resources.displayMetrics.heightPixels * 0.68f).toInt()
        val height = if (available > 0) minOf(available, maximum) else maximum
        super.onMeasure(widthMeasureSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST))
    }
}

class MainActivity : ComponentActivity() {
    private val prefs by lazy { FinanzaPreferences.get(this) }
    private val apiClient by lazy { FinanzaApiClient(prefs) }
    private val remoteRepository by lazy { FinanzaRemoteRepository(apiClient) }
    private val accountRepository by lazy { FinanzaAccountRepository(apiClient) }
    private val featureStore by lazy { FinanzaFeatureStore(this) }
    private val ptBr = Locale.Builder().setLanguage("pt").setRegion("BR").build()
    private val money = NumberFormat.getCurrencyInstance(ptBr)
    private val shortDate = DateTimeFormatter.ofPattern("dd MMM", ptBr)

    private var bootSyncTriggered = false

    private var activeTab = "home"
    private var accentId = "green"
    private var composeRevision by mutableIntStateOf(0)
    private var loginVisible by mutableStateOf(false)
    private var loginBusy by mutableStateOf(false)
    private var loginError by mutableStateOf<String?>(null)
    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showQuickExpenseNotificationSafely()
    }
    private val backupImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::importBackupFromUri)
    }
    private val transactionImportLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(::importTransactionsFromUri)
    }

    private val isFinanzaExperience get() = visualExperience() == AppExperience.FINANZA
    private val bg get() = when {
        isDarkTheme() -> Color.BLACK
        isFinanzaExperience -> Color.rgb(247, 247, 243)
        else -> Color.rgb(242, 242, 247)
    }
    private val paper get() = when {
        isDarkTheme() && isFinanzaExperience -> Color.rgb(18, 18, 18)
        isDarkTheme() -> Color.rgb(28, 28, 30)
        else -> Color.WHITE
    }
    private val paper2 get() = when {
        isDarkTheme() && isFinanzaExperience -> Color.rgb(34, 34, 34)
        isDarkTheme() -> Color.rgb(44, 44, 46)
        isFinanzaExperience -> Color.rgb(237, 237, 232)
        else -> Color.rgb(229, 229, 234)
    }
    private val ink get() = if (isDarkTheme()) Color.rgb(248, 248, 244) else Color.rgb(16, 16, 16)
    private val muted get() = when {
        isDarkTheme() && isFinanzaExperience -> Color.rgb(198, 198, 190)
        isDarkTheme() -> Color.rgb(174, 174, 178)
        isFinanzaExperience -> Color.rgb(111, 111, 104)
        else -> Color.rgb(99, 99, 102)
    }
    private val soft get() = when {
        isDarkTheme() -> Color.argb(60, 255, 255, 255)
        isFinanzaExperience -> Color.argb(216, 255, 255, 255)
        else -> Color.argb(176, 255, 255, 255)
    }
    private val line get() = if (isDarkTheme()) Color.argb(46, 255, 255, 255) else Color.argb(31, 16, 16, 16)
    private val accent get() = when {
        isFinanzaExperience -> Color.rgb(216, 255, 95)
        isDarkTheme() -> Color.WHITE
        else -> Color.rgb(28, 28, 30)
    }
    private val danger get() = Color.rgb(238, 86, 104)

    private data class Accent(val id: String, val name: String, val color: Int)
    private data class Entry(
        val id: Long,
        val title: String,
        val category: String,
        val amount: Double,
        val type: String,
        val date: String,
        val accountId: String = "",
        val note: String = "",
        val purchaseDate: String = date,
        val installmentGroup: String = "",
        val installmentNum: Int = 0,
        val installmentTotal: Int = 0,
        val recurGroup: String = "",
        val splitMeta: String = "",
        val paid: Boolean = false,
        val pending: Boolean = false,
        val sourceModule: String = "",
        val sourceItemId: String = ""
    )
    private data class Account(
        val id: String,
        val name: String,
        val icon: String,
        val type: String,
        val balance: Double,
        val yieldRate: Double = 0.0,
        val yieldType: String = "manual",
        val yieldVal: Double = 0.0,
        val calcBase: String = "du",
        val startDate: String = "",
        val cardClosingDay: Int = 0,
        val cardDueDay: Int = 0,
        val cardLast4: String = "",
        val cardExpiry: String = "",
        val note: String = ""
    )
    private data class MonthSnapshot(
        val entries: List<Entry>,
        val income: Double,
        val spent: Double
    )
    private val accents = listOf(
        Accent("green", "Verde", Color.rgb(53, 201, 111)),
        Accent("yellow", "Amarelo", Color.rgb(244, 207, 69)),
        Accent("blue", "Azul", Color.rgb(91, 140, 255)),
        Accent("violet", "Lilas", Color.rgb(122, 167, 255)),
        Accent("coral", "Coral", Color.rgb(255, 127, 102))
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        FinanzaPreferences.repairLegacyTypes(this)
        accentId = prefs.getString("accent", "green") ?: "green"
        activeTab = intent.getStringExtra("start_tab")?.takeIf { it in setOf("home", "accounts", "analysis", "settings") } ?: activeTab
        loginVisible = !prefs.getBoolean("login_completed", false) || prefs.getBoolean("session_expired", false)
        seedIfEmpty()

        setContent {
            @Suppress("UNUSED_VARIABLE")
            val revision = composeRevision
            FinanceAppTheme(darkTheme = isDarkTheme(), experience = visualExperience()) {
                if (loginVisible) {
                    LoginScreen(
                        initialUrl = prefs.getString("api_url", "https://finanza-api.onrender.com").orEmpty().ifBlank { "https://finanza-api.onrender.com" },
                        initialUsername = prefs.getString("login_name", "").orEmpty(),
                        busy = loginBusy,
                        error = loginError,
                        onLogin = ::submitLogin,
                        onContinueOffline = ::continueOffline
                    )
                } else {
                    val initialModule = intent.getStringExtra("feature_module")
                    AppScaffold(composeUiState(), composeActions(), intent.getBooleanExtra("open_features", false) || initialModule != null, initialModule)
                }
            }
        }
        configureSystemBars()
        window.decorView.post {
            consumeSharedInvite(intent)
            if (intent.getBooleanExtra("open_account", false)) {
                intent.removeExtra("open_account")
                showOnlineAccessDialog()
            }
        }
        if (!loginVisible) maybeSyncOnLaunch()
        if (prefs.getBoolean("quick_notification_enabled", false)) {
            ensureQuickExpenseNotification(requestPermission = false)
        }
    }

    private fun submitLogin(url: String, username: String, password: String, otp: String) {
        if (url.isBlank() || username.isBlank() || password.isBlank()) {
            loginError = "Preencha a URL, o usuário e a senha."
            return
        }
        loginBusy = true
        loginError = null
        loginOnline(url, username, password, otp,
            onSuccess = {
                prefs.edit().putBoolean("login_completed", true).apply()
                loginBusy = false
                loginVisible = false
                maybeSyncOnLaunch()
            },
            onError = { message ->
                loginBusy = false
                loginError = message
            }
        )
    }

    private fun continueOffline(displayName: String) {
        prefs.edit()
            .putBoolean("login_completed", true)
            .putBoolean("session_expired", false)
            .putString("user_name", displayName.ifBlank { "Você" })
            .apply()
        loginError = null
        loginVisible = false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("start_tab")?.takeIf { it in setOf("home", "accounts", "analysis", "settings") }?.let {
            activeTab = it
            render()
        }
        consumeSharedInvite(intent)
        if (intent.getBooleanExtra("open_account", false)) {
            intent.removeExtra("open_account")
            showOnlineAccessDialog()
        }
    }

    private fun consumeSharedInvite(source: Intent) {
        val data = source.data ?: return
        val raw = data.getQueryParameter("invite")?.trim().orEmpty()
        if (raw.isBlank()) return
        source.data = null
        val payload = runCatching {
            val decoded = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
            JSONObject(decoded.toString(Charsets.UTF_8))
        }.getOrElse {
            Toast.makeText(this, "Este convite não é válido.", Toast.LENGTH_LONG).show()
            return
        }
        val people = payload.optJSONArray("people")?.length() ?: 0
        val name = payload.optString("name", "Finanza compartilhado")
        showAppDialog("Entrar em $name?", "$people pessoas fazem parte deste espaço.") { dialog ->
            addView(body("O convite será mesclado ao seu espaço. Seus dados e seu perfil principal serão preservados.", 13, muted))
            addView(primaryButton("Aceitar convite") {
                val mutation = featureStore.mergeSharedInvite(payload)
                if (mutation == null) {
                    Toast.makeText(this@MainActivity, "Não foi possível aplicar o convite.", Toast.LENGTH_LONG).show()
                    return@primaryButton
                }
                syncFeatureMutationAsync(mutation)
                dialog.dismiss()
                render()
                Toast.makeText(this@MainActivity, "Convite aplicado.", Toast.LENGTH_SHORT).show()
            })
            addView(tertiaryButton("Agora não") { dialog.dismiss() })
        }
    }

    private fun maybeSyncOnLaunch() {
        if (!bootSyncTriggered && isOnlineMode()) {
            bootSyncTriggered = true
            syncRemoteNow(silent = true)
        }
    }

    private fun openQuickExpenseScreen() {
        startActivity(QuickExpenseShortcutActivity.createIntent(this))
    }

    private fun ensureQuickExpenseNotification(requestPermission: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            if (requestPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        showQuickExpenseNotificationSafely()
    }

    private fun showQuickExpenseNotificationSafely() {
        runCatching { FinanzaWidgets.showQuickExpenseNotification(this) }
            .onFailure { error ->
                prefs.edit().putString("last_notification_error", error.javaClass.simpleName).apply()
            }
    }

    private fun configureSystemBars() {
        val decorView = window.decorView
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            decorView.windowInsetsController?.setSystemBarsAppearance(
                if (isDarkTheme()) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            decorView.systemUiVisibility =
                if (isDarkTheme()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            window.navigationBarDividerColor = Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    private fun render() {
        composeRevision++
        FinanzaWidgets.updateAll(this)
    }

   private fun showEntryDialog(type: String, existing: Entry? = null) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = dialogPanel(dialog)
        val allAccounts = accounts()
        val titleInput = input("Descrição", existing?.title ?: "")
        val categoryInput = input("Categoria", existing?.category ?: "")
        val amountInput = input("Valor", existing?.amount?.toString() ?: "", true)
        val dateInput = input("Data (AAAA-MM-DD)", existing?.date ?: LocalDate.now().toString())
        val noteInput = input("Observação", existing?.note ?: "")
        val installmentsInput = input("Parcelas", existing?.installmentTotal?.takeIf { it > 0 }?.toString() ?: "1", true)
        val recurrenceInput = input("Repetir por quantos meses", "1", true)
        val sharedPeople = featureStore.sharedPeopleOptions()
        val existingSplit = runCatching { JSONObject(existing?.splitMeta.orEmpty()) }.getOrDefault(JSONObject())
        var selectedPayerId = existingSplit.optString("payerId", featureStore.sharedOwnerId())
        val selectedParticipantIds = mutableSetOf<String>().apply {
            val saved = existingSplit.optJSONArray("participants")
            if (saved != null) for (index in 0 until saved.length()) add(saved.optString(index))
            if (isEmpty()) addAll(sharedPeople.map { it.first })
        }
        val splitCheck = CheckBox(this).apply {
            text = "Dividir igualmente no espaço compartilhado"
            setTextColor(ink)
            textSize = 13f
            isChecked = existing?.splitMeta?.isNotBlank() == true
            visibility = if (type == "expense" && featureStore.sharedPeopleCount() > 1) View.VISIBLE else View.GONE
        }
        val approvalCheck = CheckBox(this).apply {
            text = "Solicitar aprovacao antes de entrar nos saldos"
            setTextColor(ink)
            textSize = 13f
            isChecked = existingSplit.optJSONObject("approval")?.optString("status") == "pending"
        }
        val sharedDetails = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = if (splitCheck.isChecked) View.VISIBLE else View.GONE
            addView(body("Quem pagou", 11, muted).apply { setPadding(0, dp(10), 0, dp(6)) })
            addView(choicePickerRow(sharedPeople, selectedPayerId) { selectedPayerId = it })
            addView(body("Quem participa", 11, muted).apply { setPadding(0, dp(10), 0, dp(4)) })
            sharedPeople.forEach { (id, name) ->
                addView(CheckBox(this@MainActivity).apply {
                    text = name
                    setTextColor(ink)
                    textSize = 13f
                    isChecked = id in selectedParticipantIds
                    setOnCheckedChangeListener { _, checked -> if (checked) selectedParticipantIds += id else selectedParticipantIds -= id }
                })
            }
            addView(approvalCheck)
        }
        splitCheck.setOnCheckedChangeListener { _, checked -> sharedDetails.visibility = if (checked) View.VISIBLE else View.GONE }
        val suggestions = suggestedCategories(type, existing?.category)
        var selectedAccountId = existing?.accountId?.ifBlank { null } ?: allAccounts.firstOrNull()?.id
        panel.addView(ImageView(this).apply {
            setImageResource(if (type == "income") android.R.drawable.arrow_down_float else android.R.drawable.arrow_up_float)
            imageTintList = android.content.res.ColorStateList.valueOf(ink)
            background = rounded(soft, dp(15), line)
            setPadding(dp(11), dp(11), dp(11), dp(11))
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply {
            setMargins(0, 0, 0, dp(14))
        })
        panel.addView(statusPill(if (existing != null) "Edicao" else when (type) {
            "income" -> "Receita"
            "due" -> "Compromisso"
            else -> "Gasto"
        }).apply {
            setTextColor(ink)
            background = rounded(soft, dp(99), line)
        })
        panel.addView(label(entryDialogTitle(type, existing != null), 27, ink, true).apply {
            setPadding(0, dp(12), 0, 0)
        })
        panel.addView(body(entryDialogSubtitle(type), 13, muted))
        if (type != "due") {
            panel.addView(LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(2))
                addView(infoPill(if (allAccounts.isEmpty()) "Sem conta definida" else "Conta ${allAccounts.size} base(s)"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(infoPill(if (type == "income") "Entrada no saldo" else "Sai do saldo"), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(dp(8), 0, 0, 0)
                })
            })
        }
        panel.addView(titleInput); panel.addView(categoryInput)
        if (suggestions.isNotEmpty()) {
            panel.addView(body("Categorias sugeridas", 11, muted).apply {
                setPadding(0, dp(12), 0, dp(8))
            })
            panel.addView(categorySuggestionRow(suggestions) { categoryInput.setText(it) })
        }
        if (allAccounts.isNotEmpty() && type != "due") {
            panel.addView(body("Conta usada", 11, muted).apply {
                setPadding(0, dp(12), 0, dp(8))
            })
            panel.addView(accountPickerRow(allAccounts, selectedAccountId) { selectedAccountId = it })
        }
        panel.addView(amountInput); panel.addView(dateInput)
        panel.addView(body("Data rapida", 11, muted).apply {
            setPadding(0, dp(12), 0, dp(8))
        })
        panel.addView(dateSuggestionRow(type) { dateInput.setText(it) })
        panel.addView(noteInput)
        if (type == "expense" && existing == null) {
            panel.addView(installmentsInput)
            panel.addView(recurrenceInput)
            panel.addView(splitCheck)
            panel.addView(sharedDetails)
        }
        panel.addView(primaryButton(entryDialogButtonLabel(type, existing != null)) {
            val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull()
            val date = dateInput.text.toString().ifBlank { LocalDate.now().toString() }
            if (titleInput.text.isBlank() || amount == null || amount <= 0 || !validDate(date)) {
                Toast.makeText(this, "Confira descrição, valor e data.", Toast.LENGTH_SHORT).show()
                return@primaryButton
            }
            val accountId = if (type == "due") "" else (selectedAccountId ?: allAccounts.firstOrNull()?.id).orEmpty()
            val purchaseDate = date
            val effectiveDate = if (type == "expense") {
                allAccounts.firstOrNull { it.id == accountId }?.let { creditEffectiveDate(it, date) } ?: date
            } else date
            val splitMeta = if (splitCheck.isChecked) {
                featureStore.equalSplitMeta(selectedPayerId, selectedParticipantIds.toList(), approvalCheck.isChecked)?.toString().orEmpty()
            } else existing?.splitMeta.orEmpty()
            if (splitCheck.isChecked && splitMeta.isBlank()) {
                Toast.makeText(this, "Escolha um pagador e pelo menos duas pessoas.", Toast.LENGTH_SHORT).show()
                return@primaryButton
            }
            val baseItem = Entry(
                existing?.id ?: System.currentTimeMillis(),
                titleInput.text.toString().trim(),
                categoryInput.text.toString().ifBlank { "Geral" }.trim(),
                amount, type, effectiveDate, accountId,
                note = noteInput.text.toString().trim(),
                purchaseDate = existing?.purchaseDate ?: purchaseDate,
                installmentGroup = existing?.installmentGroup.orEmpty(),
                installmentNum = existing?.installmentNum ?: 0,
                installmentTotal = existing?.installmentTotal ?: 0,
                recurGroup = existing?.recurGroup.orEmpty(),
                splitMeta = splitMeta,
                paid = existing?.paid ?: false,
                pending = existing?.pending ?: false
            )
            if (existing != null) {
                val item = baseItem
                replaceEntry(existing, item)
                syncEntryChangeAsync("update", entry = item, previous = existing)
            } else {
                val installments = installmentsInput.text.toString().toIntOrNull()?.coerceIn(1, 60) ?: 1
                val repetitions = recurrenceInput.text.toString().toIntOrNull()?.coerceIn(1, 60) ?: 1
                val generated = when {
                    type == "expense" && installments > 1 -> {
                        val group = "android_installment_${System.currentTimeMillis()}"
                        val perInstallment = amount / installments
                        (0 until installments).map { index ->
                            baseItem.copy(
                                id = System.currentTimeMillis() + index,
                                title = "${baseItem.title} (${index + 1}/$installments)",
                                amount = perInstallment,
                                date = LocalDate.parse(effectiveDate).plusMonths(index.toLong()).toString(),
                                installmentGroup = group,
                                installmentNum = index + 1,
                                installmentTotal = installments
                            )
                        }
                    }
                    type == "expense" && repetitions > 1 -> {
                        val group = "android_recur_${System.currentTimeMillis()}"
                        (0 until repetitions).map { index ->
                            baseItem.copy(
                                id = System.currentTimeMillis() + index,
                                date = LocalDate.parse(effectiveDate).plusMonths(index.toLong()).toString(),
                                recurGroup = group
                            )
                        }
                    }
                    else -> listOf(baseItem)
                }
                generated.forEach { item ->
                    saveEntry(item)
                    syncEntryChangeAsync("create", entry = item)
                }
            }
            dialog.dismiss(); render()
        })
        if (existing == null && type == "expense") {
            panel.addView(secondaryButton("Usar folha rapida") {
                dialog.dismiss()
                openQuickExpenseScreen()
            })
        }
        if (existing != null && type == "due") {
            panel.addView(secondaryButton("Marcar como pago") {
                dialog.dismiss()
                settleDue(existing)
            })
            panel.addView(tertiaryButton("Adiar 7 dias") {
                dialog.dismiss()
                postponeDue(existing, 7)
            })
        }
        if (existing != null) panel.addView(dangerButton("Excluir") {
            deleteEntry(existing)
            syncEntryChangeAsync("delete", previous = existing)
            dialog.dismiss()
            render()
        })
        setDialogContent(dialog, panel)
        styleDialog(dialog)
        dialog.show()
        titleInput.postDelayed({
            focusField(titleInput)
        }, 120)
    }

    private fun showTextSetting(title: String, value: String, numeric: Boolean = false, onSave: (String) -> Unit) {
        showAppDialog(title, "Atualize este campo no padrão do seu espaço.") { dialog ->
            val field = input(title, value, numeric).apply {
                setText(value)
                selectAll()
            }
            addView(field)
            addView(primaryButton("Salvar") {
                onSave(field.text.toString())
                dialog.dismiss()
            })
            field.postDelayed({ focusField(field) }, 120)
        }
    }

    private fun showAccountDialog(existing: Account? = null) {
        val types = listOf(
            "checking" to "Conta corrente",
            "savings" to "Reserva",
            "investment" to "Investimento",
            "credit" to "Cartão",
            "cash" to "Carteira"
        )
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = dialogPanel(dialog)
        val nameInput = input("Nome da conta", existing?.name ?: "")
        val balanceInput = input("Saldo inicial", existing?.balance?.toString() ?: "", true)
        val yieldInput = input("Rendimento mensal (%)", existing?.yieldRate?.toString() ?: "", true)
        val closingInput = input("Dia de fechamento do cartão", existing?.cardClosingDay?.takeIf { it > 0 }?.toString() ?: "", true)
        val dueInput = input("Dia de vencimento do cartão", existing?.cardDueDay?.takeIf { it > 0 }?.toString() ?: "", true)
        val last4Input = input("Ultimos 4 digitos", existing?.cardLast4 ?: "", true)
        val expiryInput = input("Validade MM/AA", existing?.cardExpiry ?: "")
        val noteInput = input("Observação", existing?.note ?: "")
        var selectedType = existing?.type ?: "checking"
        panel.addView(ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_manage)
            imageTintList = android.content.res.ColorStateList.valueOf(ink)
            background = rounded(soft, dp(15), line)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }, LinearLayout.LayoutParams(dp(42), dp(42)).apply {
            setMargins(0, 0, 0, dp(14))
        })
        panel.addView(statusPill(if (existing != null) "Edicao" else "Conta").apply {
            setTextColor(ink)
            background = rounded(soft, dp(99), line)
        })
        panel.addView(label(existing?.let { "Editar conta" } ?: "Nova conta", 27, ink, true).apply {
            setPadding(0, dp(12), 0, 0)
        })
        panel.addView(body("Monte uma base simples para saldo, caixa e cartoes.", 13, muted))
        panel.addView(nameInput)
        panel.addView(body("Tipo da conta", 11, muted).apply {
            setPadding(0, dp(12), 0, dp(8))
        })
        panel.addView(accountTypePickerRow(types, selectedType) { selectedType = it })
        panel.addView(balanceInput)
        panel.addView(yieldInput)
        panel.addView(body("Dados de cartão", 11, muted).apply { setPadding(0, dp(12), 0, dp(4)) })
        panel.addView(closingInput); panel.addView(dueInput); panel.addView(last4Input); panel.addView(expiryInput); panel.addView(noteInput)
        panel.addView(primaryButton("Salvar conta") {
            val name = nameInput.text.toString().trim()
            val balance = balanceInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            if (name.isBlank()) {
                Toast.makeText(this, "Digite o nome da conta.", Toast.LENGTH_SHORT).show()
                return@primaryButton
            }
            val account = Account(
                id = existing?.id ?: "acc_${System.currentTimeMillis()}",
                name = name,
                icon = accountTypeIcon(selectedType),
                type = selectedType,
                balance = balance,
                yieldRate = yieldInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0,
                yieldType = existing?.yieldType ?: if (selectedType == "investment") "cdi_pct" else "manual",
                yieldVal = yieldInput.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0,
                calcBase = existing?.calcBase ?: "du",
                startDate = existing?.startDate ?: LocalDate.now().toString(),
                cardClosingDay = if (selectedType == "credit") closingInput.text.toString().toIntOrNull()?.coerceIn(1, 31) ?: 0 else 0,
                cardDueDay = if (selectedType == "credit") dueInput.text.toString().toIntOrNull()?.coerceIn(1, 31) ?: 0 else 0,
                cardLast4 = if (selectedType == "credit") last4Input.text.toString().filter(Char::isDigit).takeLast(4) else "",
                cardExpiry = if (selectedType == "credit") expiryInput.text.toString().trim() else "",
                note = noteInput.text.toString().trim()
            )
            if (selectedType == "credit" && (account.cardClosingDay == 0 || account.cardDueDay == 0)) {
                Toast.makeText(this, "Informe fechamento e vencimento do cartão.", Toast.LENGTH_SHORT).show()
                return@primaryButton
            }
            saveAccount(account)
            syncAccountsRemoteAsync()
            dialog.dismiss()
            render()
        })
        if (existing != null) {
            panel.addView(dangerButton("Excluir conta") {
                deleteAccount(existing.id)
                syncAccountsRemoteAsync()
                dialog.dismiss()
                render()
            })
        }
        setDialogContent(dialog, panel)
        styleDialog(dialog)
        dialog.show()
        nameInput.postDelayed({ focusField(nameInput) }, 120)
    }

    private fun showTransferDialog() {
        val accountItems = accounts()
        if (accountItems.size < 2) {
            Toast.makeText(this, "Cadastre pelo menos duas contas.", Toast.LENGTH_SHORT).show()
            return
        }
        showAppDialog("Transferir", "Mova saldo entre suas contas sem alterar o resultado do mes.") { dialog ->
            var fromId = accountItems.first().id
            var toId = accountItems[1].id
            val amountInput = input("Valor", "", true)
            addView(body("Conta de origem", 11, muted))
            addView(accountPickerRow(accountItems, fromId) { fromId = it })
            addView(body("Conta de destino", 11, muted).apply { setPadding(0, dp(10), 0, 0) })
            addView(accountPickerRow(accountItems, toId) { toId = it })
            addView(amountInput)
            addView(primaryButton("Transferir") {
                val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull()
                if (amount == null || amount <= 0 || fromId == toId) {
                    Toast.makeText(this@MainActivity, "Confira valor e contas.", Toast.LENGTH_SHORT).show()
                    return@primaryButton
                }
                val now = System.currentTimeMillis()
                val date = LocalDate.now().toString()
                val from = accountItems.first { it.id == fromId }
                val to = accountItems.first { it.id == toId }
                val out = Entry(now, "Transferencia para ${to.name}", "Transferencia", amount, "expense", date, fromId, "Transferencia interna")
                val income = Entry(now + 1, "Transferencia de ${from.name}", "Transferencia", amount, "income", date, toId, "Transferencia interna")
                saveEntry(out); saveEntry(income)
                syncEntryChangeAsync("create", entry = out); syncEntryChangeAsync("create", entry = income)
                dialog.dismiss(); render()
            })
            amountInput.postDelayed({ focusField(amountInput) }, 120)
        }
    }

    private fun showInfo(title: String, copy: String) {
        showAppDialog(title, null) { dialog ->
            addView(body(copy, 13, muted).apply { setPadding(0, dp(8), 0, 0) })
            addView(primaryButton("Entendi") { dialog.dismiss() })
        }
    }

    private fun confirmReset() {
        showAppDialog("Apagar dados locais?", "Esta acao remove lancamentos, contas e modulos somente deste aparelho.") { dialog ->
            addView(dangerButton("Apagar") {
                prefs.edit()
                    .remove("entries").remove("future").remove("accounts")
                    .remove("feature_budgets").remove("feature_goals").remove("feature_shopping")
                    .remove("feature_car").remove("feature_shared").remove("feature_commitments")
                    .remove("feature_sync_queue").remove("core_sync_queue")
                    .apply()
                seedIfEmpty()
                FinanzaWidgets.updateAll(this@MainActivity)
                dialog.dismiss()
                render()
            })
        }
    }

    private fun themeMode(): String = prefs.getString("theme_mode", "light") ?: "light"

    private fun isDarkTheme(): Boolean = themeMode() == "dark"

    private fun visualExperience(): AppExperience =
        AppExperience.fromId(prefs.getString("visual_experience", AppExperience.NEXT.id))

    private fun applyTheme(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        configureSystemBars()
        FinanzaWidgets.updateAll(this)
        render()
    }

    private fun applyVisualExperience(experience: AppExperience) {
        prefs.edit().putString("visual_experience", experience.id).apply()
        configureSystemBars()
        FinanzaWidgets.updateAll(this)
        render()
    }

    private fun showThemeDialog() {
        showAppDialog("Aparência", "Defina o modo de cor e a linguagem visual do Finext.") { dialog ->
            addView(dialogSectionLabel("Modo de cor"))
            addView(dialogChoice("Claro", "Fundo claro e contraste suave", !isDarkTheme()) {
                applyTheme("light")
                dialog.dismiss()
            })
            addView(dialogChoice("Escuro", "Superfícies escuras e baixo brilho", isDarkTheme()) {
                applyTheme("dark")
                dialog.dismiss()
            })
            addView(dialogSectionLabel("Tema", topMargin = 16))
            AppExperience.entries.forEach { experience ->
                addView(dialogChoice(experience.label, experience.description, visualExperience() == experience) {
                    applyVisualExperience(experience)
                    dialog.dismiss()
                })
            }
        }
    }

    private fun onlineStatusText(): String {
        if (!prefs.getBoolean("online_mode", false)) return "Entre para sincronizar o Finext com sua conta Finanza."
        val name = prefs.getString("user_name", "Voce") ?: "Voce"
        val url = prefs.getString("api_url", "") ?: ""
        val host = urlHostLabel(url)
        return if (host.isBlank()) "Conectado como $name" else "Conectado como $name em $host"
    }

    @SuppressLint("SetTextI18n")
    private fun showOnlineAccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = dialogPanel(dialog).apply {
            addView(statusPill(if (isOnlineMode()) "Conta conectada" else "Conta web"))
            addView(label(if (isOnlineMode()) "Sua conta Finanza" else "Entrar na sua conta", 27, ink, true).apply {
                setPadding(0, dp(12), 0, 0)
            })
            addView(body("Use a mesma conta do Finanza web para sincronizar movimentos, contas e agenda.", 13, muted))
        }
        val savedUrl = prefs.getString("api_url", "https://finanza-api.onrender.com") ?: "https://finanza-api.onrender.com"
        val urlInput = input("URL da API", savedUrl)
        val userInput = input("Usuário", prefs.getString("login_name", "") ?: "")
        val passInput = input("Senha", "").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val otpInput = input("Codigo 2FA (opcional)", "").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        val status = body(if (prefs.getBoolean("online_mode", false)) onlineStatusText() else "Modo local ativo neste aparelho.", 12, muted).apply {
            setPadding(0, dp(12), 0, 0)
        }
        val destination = infoPill("Destino ${urlHostLabel(savedUrl)}").apply {
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        val sameLoginPill = infoPill("Mesmo login do web").apply {
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        panel.addView(destination)
        panel.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, 0)
            addView(sameLoginPill, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(infoPill(if (isOnlineMode()) "Sync ativa" else "Modo local").apply {
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(dp(8), 0, 0, 0)
            })
        })
        panel.addView(urlInput)
        panel.addView(userInput)
        panel.addView(passInput)
        panel.addView(otpInput)
        panel.addView(status)
        lateinit var connectButton: TextView
        connectButton = primaryButton("Entrar e sincronizar") {
            val baseUrl = urlInput.text.toString().trim()
            val username = userInput.text.toString().trim()
            val password = passInput.text.toString()
            val otp = otpInput.text.toString().trim()
            if (baseUrl.isBlank() || username.isBlank() || password.isBlank()) {
                status.text = "Preencha URL, usuário e senha."
                return@primaryButton
            }
            destination.text = "Destino ${urlHostLabel(baseUrl)}"
            connectButton.isEnabled = false
            connectButton.text = "Conectando..."
            status.text = "Verificando servidor e credenciais..."
            loginOnline(baseUrl, username, password, otp,
                onSuccess = { name ->
                    dialog.dismiss()
                    Toast.makeText(this@MainActivity, "Bem-vindo, $name!", Toast.LENGTH_SHORT).show()
                    render()
                    syncRemoteNow(silent = false)
                },
                onError = { message ->
                    connectButton.isEnabled = true
                    connectButton.text = "Entrar e sincronizar"
                    status.text = message
                }
            )
        }
        panel.addView(connectButton)
        if (prefs.getBoolean("online_mode", false)) {
            panel.addView(secondaryButton("Sincronizar agora") {
                status.text = "Sincronizando o Finext..."
                syncRemoteNow(
                    silent = true,
                    onComplete = { status.text = "Dados sincronizados com sucesso." },
                    onError = { status.text = it }
                )
            })
            panel.addView(tertiaryButton("Sair desta conta", danger) {
                disconnectOnline()
                dialog.dismiss()
                render()
            })
        }
        setDialogContent(dialog, panel)
        styleDialog(dialog)
        dialog.show()
        val initialField = if (userInput.text?.isNotBlank() == true) passInput else userInput
        initialField.postDelayed({ focusField(initialField) }, 120)
    }

    private fun disconnectOnline() {
        prefs.edit()
            .putBoolean("online_mode", false)
            .remove("api_url")
            .remove("api_key")
            .remove("login_name")
            .remove("role")
            .remove("user_id")
            .remove("user_remote_id")
            .remove("two_factor_enabled")
            .remove("session_expired")
            .remove("login_completed")
            .apply()
        loginVisible = true
        loginBusy = false
        loginError = null
        FinanzaWidgets.updateAll(this)
    }

    private fun showSecurityDialog() {
        if (!isOnlineMode()) {
            showOnlineAccessDialog()
            return
        }
        showAppDialog("Seguranca", "Proteja a mesma conta usada no Finanza web.") { dialog ->
            addView(healthRow("2FA", "Autenticacao em duas etapas", if (prefs.getBoolean("two_factor_enabled", false)) "Ativa" else "Desativada"))
            addView(healthRow("ID", "Sessao neste aparelho", "${prefs.getString("login_name", "")} em ${urlHostLabel(prefs.getString("api_url", "").orEmpty())}"))
            addView(secondaryButton("Atualizar perfil e sessao") {
                refreshOnlineProfile(dialog)
            })
            addView(primaryButton(if (prefs.getBoolean("two_factor_enabled", false)) "Reconfigurar 2FA" else "Ativar 2FA") {
                dialog.dismiss()
                beginTwoFactorSetup()
            })
            addView(secondaryButton("Gerar codigo de recuperacao") {
                thread {
                    runCatching { accountRepository.recoveryCode() }
                        .onSuccess { code -> runOnUiThread { showInfo("Codigo de recuperacao", code) } }
                        .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                }
            })
            addView(secondaryButton("Alterar senha com codigo") {
                dialog.dismiss()
                showPasswordResetDialog()
            })
            if (prefs.getBoolean("two_factor_enabled", false)) {
                addView(dangerButton("Desativar 2FA") {
                    dialog.dismiss()
                    showTwoFactorDisableDialog()
                })
            }
            addView(dangerButton("Encerrar sessao neste aparelho") {
                disconnectOnline()
                dialog.dismiss()
                render()
            })
        }
    }

    private fun refreshOnlineProfile(dialog: Dialog? = null) {
        thread {
            runCatching { accountRepository.profile() }
                .onSuccess { profile ->
                    prefs.edit()
                        .putString("user_name", profile.optString("name", prefs.getString("user_name", "Voce").orEmpty()))
                        .putString("role", profile.optString("role", prefs.getString("role", "").orEmpty()))
                        .putString("user_remote_id", profile.optString("id", prefs.getString("user_remote_id", "").orEmpty()))
                        .putBoolean("two_factor_enabled", profile.optBoolean("two_factor_enabled", prefs.getBoolean("two_factor_enabled", false)))
                        .putBoolean("session_expired", false)
                        .apply()
                    runOnUiThread {
                        dialog?.dismiss()
                        render()
                        Toast.makeText(this@MainActivity, "Perfil e sessao atualizados.", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
        }
    }

    private fun showPasswordResetDialog() {
        showAppDialog("Alterar senha", "Use o codigo de recuperacao gerado para sua conta.") { dialog ->
            val username = input("Usuário", prefs.getString("login_name", "").orEmpty())
            val password = input("Nova senha", "").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val confirmation = input("Confirmar nova senha", "").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val recovery = input("Codigo de recuperacao", "")
            addView(username); addView(password); addView(confirmation); addView(recovery)
            addView(primaryButton("Alterar senha") {
                if (password.text.toString() != confirmation.text.toString()) {
                    Toast.makeText(this@MainActivity, "As senhas não conferem.", Toast.LENGTH_SHORT).show()
                    return@primaryButton
                }
                thread {
                    runCatching { accountRepository.resetPasswordWithRecovery(username.text.toString(), password.text.toString(), recovery.text.toString()) }
                        .onSuccess {
                            runOnUiThread {
                                disconnectOnline()
                                dialog.dismiss()
                                render()
                                Toast.makeText(this@MainActivity, "Senha alterada. Entre novamente.", Toast.LENGTH_LONG).show()
                            }
                        }
                        .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                }
            })
        }
    }

    private fun showProfileDialog() {
        showAppDialog("Seu perfil", if (isOnlineMode()) "Dados da conta conectada e nome exibido neste aparelho." else "Perfil local deste aparelho.") { dialog ->
            addView(healthRow("ID", "Usuário", prefs.getString("login_name", "Local").orEmpty().ifBlank { "Local" }))
            addView(healthRow("ACL", "Acesso", if (isOnlineMode()) roleLabel(prefs.getString("role", "editor").orEmpty()) else "Local"))
            val name = input("Nome exibido", prefs.getString("user_name", "Voce").orEmpty())
            addView(name)
            addView(primaryButton("Salvar nome exibido") {
                prefs.edit().putString("user_name", name.text.toString().trim().ifBlank { "Voce" }).apply()
                dialog.dismiss()
                render()
            })
            if (isOnlineMode()) addView(secondaryButton("Atualizar dados da conta") { refreshOnlineProfile(dialog) })
        }
    }

    private fun beginTwoFactorSetup() {
        thread {
            runCatching { accountRepository.beginTwoFactor() }
                .onSuccess { data ->
                    runOnUiThread {
                        showAppDialog("Ativar 2FA", "Adicione a chave no autenticador e confirme o codigo.") { dialog ->
                            addView(body(data.optString("secret", data.optString("otpauth_url", "Chave gerada")), 13, ink))
                            val codeInput = input("Codigo de 6 digitos", "", true)
                            addView(codeInput)
                            addView(primaryButton("Confirmar") {
                                val code = codeInput.text.toString().trim()
                                if (code.length < 6) return@primaryButton
                                thread {
                                    runCatching { accountRepository.confirmTwoFactor(code) }
                                        .onSuccess {
                                            prefs.edit().putBoolean("two_factor_enabled", true).apply()
                                            runOnUiThread { dialog.dismiss(); render(); Toast.makeText(this@MainActivity, "2FA ativado.", Toast.LENGTH_SHORT).show() }
                                        }
                                        .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                                }
                            })
                            codeInput.postDelayed({ focusField(codeInput) }, 120)
                        }
                    }
                }
                .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
        }
    }

    private fun showTwoFactorDisableDialog() {
        showAppDialog("Desativar 2FA", "Confirme com um codigo atual do autenticador.") { dialog ->
            val codeInput = input("Codigo", "", true)
            addView(codeInput)
            addView(dangerButton("Desativar") {
                val code = codeInput.text.toString().trim()
                thread {
                    runCatching { accountRepository.disableTwoFactor(code) }
                        .onSuccess {
                            prefs.edit().putBoolean("two_factor_enabled", false).apply()
                            runOnUiThread { dialog.dismiss(); render(); Toast.makeText(this@MainActivity, "2FA desativado.", Toast.LENGTH_SHORT).show() }
                        }
                        .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                }
            })
        }
    }

    private fun showAdminDialog() {
        if (!isOnlineMode() || !FinanzaAccountRules.canAdmin(prefs.getString("role", "").orEmpty())) {
            showInfo("Administração", "Este recurso está disponível apenas para administradores online.")
            return
        }
        thread {
            runCatching {
                val users = accountRepository.users()
                val overview = runCatching { accountRepository.adminOverview() }.getOrDefault(JSONObject())
                val audit = runCatching { accountRepository.audit(30) }.getOrDefault(JSONArray())
                Triple(users, overview, audit)
            }
                .onSuccess { (users, overview, audit) -> runOnUiThread { showAdminUsers(users, overview, audit) } }
                .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
        }
    }

    private fun showAdminUsers(users: JSONArray, overview: JSONObject, audit: JSONArray) {
        showAppDialog("Administração", "Usuários e níveis de acesso do ambiente online.") { dialog ->
            val server = overview.optJSONObject("server")
            val transactions = overview.optJSONObject("transactions")
            addView(healthRow("API", "Servidor", if (server?.optString("status") == "ok") "Operacional" else "Sem diagnostico"))
            addView(healthRow("TX", "Transações online", transactions?.optInt("total", 0)?.toString() ?: "0"))
            addView(healthRow("LOG", "Eventos de auditoria", audit.length().toString()))
            addView(primaryButton("Novo usuário") { dialog.dismiss(); showCreateAdminUserDialog() })
            for (index in 0 until users.length()) {
                val user = users.optJSONObject(index) ?: continue
                addView(settingsCard(
                    user.optString("name", user.optString("username", "Usuario")),
                    "${user.optString("username")} - ${roleLabel(user.optString("role", "editor"))}",
                    { dialog.dismiss(); showAdminUserActions(user) },
                    icon = "U"
                ))
            }
            if (audit.length() > 0) {
                addView(body("Atividade recente", 11, muted).apply { setPadding(0, dp(14), 0, dp(4)) })
                for (index in 0 until minOf(audit.length(), 5)) {
                    val item = audit.optJSONObject(index) ?: continue
                    addView(body("${item.optString("action", "Evento")} - ${item.optString("actor", "Sistema")}", 12, ink))
                }
            }
        }
    }

    private fun showCreateAdminUserDialog() {
        showAppDialog("Novo usuário", "Crie um acesso para o mesmo ambiente do Finanza.") { dialog ->
            val name = input("Nome", "")
            val username = input("Usuário", "")
            val password = input("Senha", "").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val role = input("Perfil (admin/editor/read/guest)", "editor")
            addView(name); addView(username); addView(password); addView(role)
            addView(primaryButton("Criar usuário") {
                thread {
                    runCatching { accountRepository.createUser(name.text.toString(), username.text.toString(), password.text.toString(), role.text.toString().trim()) }
                        .onSuccess { runOnUiThread { dialog.dismiss(); showAdminDialog() } }
                        .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                }
            })
        }
    }

    private fun showAdminUserActions(user: JSONObject) {
        val id = user.optString("id")
        showAppDialog(user.optString("name", "Usuario"), user.optString("username")) { dialog ->
            FinanzaAccountRules.roles.forEach { role ->
                addView(secondaryButton("Definir como ${roleLabel(role)}") {
                    thread {
                        runCatching { accountRepository.updateUserRole(id, prefs.getString("user_remote_id", "").orEmpty(), role) }
                            .onSuccess { runOnUiThread { dialog.dismiss(); showAdminDialog() } }
                            .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                    }
                })
            }
            addView(dangerButton("Excluir usuário") {
                thread {
                    runCatching { accountRepository.deleteUser(id, prefs.getString("user_remote_id", "").orEmpty()) }
                        .onSuccess { runOnUiThread { dialog.dismiss(); showAdminDialog() } }
                        .onFailure { error -> runOnUiThread { Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_LONG).show() } }
                }
            })
        }
    }

    private fun roleLabel(role: String): String = when (role) {
        "admin", "owner" -> "Administrador"
        "read" -> "Leitura"
        "guest" -> "Convidado"
        else -> "Editor"
    }

    private fun loginOnline(url: String, username: String, password: String, otp: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        thread {
            try {
                val baseUrl = url.removeSuffix("/")
                FinanzaApiClient.checkHealth(baseUrl)
                val data = FinanzaApiClient.login(baseUrl, username, password, otp)
                val apiKey = data.optString("api_key")
                val profile = FinanzaApiClient.verifySession(baseUrl, apiKey)
                val name = profile.optString("name", data.optString("name")).ifBlank { username }
                prefs.edit()
                    .putBoolean("online_mode", true)
                    .putString("api_url", baseUrl)
                    .putString("api_key", apiKey)
                    .putString("login_name", username)
                    .putString("user_name", name)
                    .putString("role", profile.optString("role", data.optString("role")))
                    .putLong("user_id", profile.optLong("id", data.optLong("id")))
                    .putString("user_remote_id", profile.optString("id", data.optString("id")))
                    .putBoolean("two_factor_enabled", profile.optBoolean("two_factor_enabled", data.optBoolean("two_factor_enabled")))
                    .putBoolean("session_expired", false)
                    .remove("last_sync_error")
                    .apply()
                runOnUiThread { onSuccess(name) }
            } catch (e: Exception) {
                runOnUiThread { onError(e.message ?: "Falha ao conectar.") }
            }
        }
    }

    private fun showAppDialog(title: String, subtitle: String? = null, content: LinearLayout.(Dialog) -> Unit) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val panel = dialogPanel().apply {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    addView(label(title, 27, ink, true))
                    if (!subtitle.isNullOrBlank()) addView(body(subtitle, 12, muted).apply { setPadding(0, dp(6), 0, 0) })
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                addView(iconButton("\u2715", "Fechar") { dialog.dismiss() }.apply {
                    setTextColor(ink)
                    background = rounded(paper2, dp(17), line)
                })
            })
            content(dialog)
        }
        setDialogContent(dialog, panel)
        styleDialog(dialog)
        dialog.show()
    }

    private fun setDialogContent(dialog: Dialog, panel: View) {
        dialog.setContentView(DialogScrollView(this).apply {
            isFillViewport = false
            isVerticalScrollBarEnabled = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(panel, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        })
    }

    private fun dialogPanel(dialog: Dialog? = null): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(12), dp(20), dp(18))
        val fill = when {
            isDarkTheme() && isFinanzaExperience -> Color.argb(232, 18, 18, 18)
            isDarkTheme() -> Color.argb(226, 22, 30, 44)
            isFinanzaExperience -> Color.argb(235, 255, 255, 255)
            else -> Color.argb(228, 255, 255, 255)
        }
        background = rounded(fill, dp(if (isFinanzaExperience) 22 else 26), Color.argb(if (isDarkTheme()) 34 else 52, 255, 255, 255))
        val topRow = LinearLayout(this@MainActivity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(View(this@MainActivity).apply {
                background = rounded(if (isDarkTheme()) Color.argb(76, 255, 255, 255) else Color.argb(44, 25, 25, 29), dp(999), Color.TRANSPARENT)
            }, LinearLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.CENTER_VERTICAL })
            addView(View(this@MainActivity), LinearLayout.LayoutParams(0, 1, 1f))
            if (dialog != null) addView(iconButton("x", "Fechar") { dialog.dismiss() }.apply {
                minimumWidth = dp(36); minimumHeight = dp(36)
                textSize = 17f
                background = rounded(if (isDarkTheme()) paper2 else Color.argb(214, 255, 255, 255), dp(14), line)
            }, LinearLayout.LayoutParams(dp(36), dp(36)))
        }
        addView(topRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(36)).apply { setMargins(0, 0, 0, dp(8)) })
    }

    private fun styleDialog(dialog: Dialog) {
        dialog.setOnShowListener {
            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                setLayout((resources.displayMetrics.widthPixels * 0.90f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                setGravity(Gravity.BOTTOM)
                attributes = attributes.apply {
                    width = (resources.displayMetrics.widthPixels * 0.90f).toInt()
                    height = WindowManager.LayoutParams.WRAP_CONTENT
                    dimAmount = 0.34f
                }
                addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
    }

    private fun dialogSectionLabel(text: String, topMargin: Int = 0): TextView = body(text, 12, muted).apply {
        typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(topMargin), 0, dp(6))
    }

    private fun dialogChoice(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(12), dp(12), dp(12))
        background = rounded(
            if (selected) accent else if (isDarkTheme()) paper2 else Color.argb(210, 255, 255, 255),
            dp(16),
            if (selected) Color.argb(36, 255, 255, 255) else line
        )
        val selectedText = selected && (isDarkTheme() || isFinanzaExperience)
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 15, if (selectedText) Color.BLACK else ink, true))
            addView(body(subtitle, 12, if (selectedText) Color.argb(180, 0, 0, 0) else muted).apply { setPadding(0, dp(3), 0, 0) })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label(if (selected) "Selecionado" else "", 11, if (selectedText) Color.BLACK else muted, true))
        setOnClickListener { onClick() }
    }.also { view ->
        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, dp(6), 0, 0) }
    }

    private fun urlHostLabel(raw: String): String = runCatching {
        val normalized = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
        val host = URL(normalized).host.removePrefix("www.")
        host.ifBlank { raw }
    }.getOrDefault(raw.removePrefix("https://").removePrefix("http://").removePrefix("www."))


    private fun isOnlineMode(): Boolean = apiClient.isConfigured

    private fun canWriteAccess(): Boolean = !isOnlineMode() || FinanzaAccountRules.canWrite(prefs.getString("role", "editor").orEmpty())

    private fun requireWriteAccess(): Boolean {
        if (canWriteAccess()) return true
        Toast.makeText(this, "Sua conta possui acesso somente para leitura.", Toast.LENGTH_SHORT).show()
        return false
    }

    private fun syncRemoteNow(silent: Boolean = false, onComplete: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (prefs.getBoolean("session_expired", false)) {
            val message = "Sessão expirada. Entre novamente para enviar as pendências."
            if (!silent) Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            onError?.invoke(message)
            return
        }
        if (!isOnlineMode()) {
            onComplete?.invoke()
            return
        }
        thread {
            try {
                flushCoreQueueBlocking()
                flushFeatureQueueBlocking()
                val snapshot = remoteRepository.loadSnapshot()
                val txData = snapshot.transactions
                val syncedEntries = mutableListOf<Entry>()
                for (index in 0 until txData.length()) {
                    val item = txData.optJSONObject(index) ?: continue
                    val type = item.optString("type", "expense")
                    if (type != "income" && type != "expense") continue
                    syncedEntries += Entry(
                        id = item.optLong("id"),
                        title = item.optString("description", "Lancamento"),
                        category = item.optString("category", "Geral"),
                        amount = item.optDouble("amount", 0.0),
                        type = type,
                        date = item.optString("date", LocalDate.now().toString()).take(10),
                        accountId = item.optString("account_id", item.optString("accountId", "")),
                        note = item.optString("note", ""),
                        purchaseDate = item.optString("purchase_date", item.optString("date", LocalDate.now().toString())).take(10),
                        installmentGroup = item.optString("installment_group", ""),
                        installmentNum = item.optInt("installment_num", 0),
                        installmentTotal = item.optInt("installment_total", 0),
                        recurGroup = item.optString("recur_group", ""),
                        splitMeta = item.optJSONObject("split_meta")?.toString().orEmpty(),
                        paid = item.optBoolean("paid", false),
                        pending = item.optBoolean("pending", false)
                    )
                }

                val stateResponse = snapshot.state
                val remoteAccounts = mutableListOf<Account>()
                val accountArray = stateResponse.optJSONArray("accounts") ?: JSONArray()
                for (index in 0 until accountArray.length()) {
                    val item = accountArray.optJSONObject(index) ?: continue
                    remoteAccounts += Account(
                        id = item.optString("id", "acc_$index"),
                        name = item.optString("name", "Conta"),
                        icon = item.optString("icon", accountTypeIcon(item.optString("type", "checking"))),
                        type = item.optString("type", "checking"),
                        balance = item.optDouble("balance", 0.0).takeIf(Double::isFinite) ?: 0.0,
                        yieldRate = item.optDouble("yield_rate", item.optDouble("yieldRate", 0.0)),
                        yieldType = item.optString("yield_type", item.optString("yieldType", "manual")),
                        yieldVal = item.optDouble("yield_val", item.optDouble("yieldVal", 0.0)),
                        calcBase = item.optString("calc_base", item.optString("calcBase", "du")),
                        startDate = item.optString("start_date", item.optString("startDate", "")).take(10),
                        cardClosingDay = item.optInt("card_closing_day", item.optInt("cardClosingDay", 0)),
                        cardDueDay = item.optInt("card_due_day", item.optInt("cardDueDay", 0)),
                        cardLast4 = item.optString("card_last4", item.optString("cardLast4", "")),
                        cardExpiry = item.optString("card_expiry", item.optString("cardExpiry", "")),
                        note = item.optString("note", "")
                    )
                }
                val remoteFuture = mutableListOf<Entry>()
                val remoteSettings = stateResponse.optJSONObject("settings") ?: JSONObject()
                val dueArray = remoteSettings.optJSONArray("android_next_due_items")
                    ?: remoteSettings.optJSONObject("rates")?.optJSONArray("dueItems")
                    ?: stateResponse.optJSONArray("dueItems") ?: JSONArray()
                for (index in 0 until dueArray.length()) {
                    val item = dueArray.optJSONObject(index) ?: continue
                    remoteFuture += Entry(
                        id = item.optLong("id", System.currentTimeMillis() + index),
                        title = item.optString("title", item.optString("name", "Conta")),
                        category = item.optString("category", "Geral"),
                        amount = item.optDouble("amount", 0.0),
                        type = "due",
                        date = item.optString("date", item.optString("nextDueDate", LocalDate.now().toString())).take(10),
                        note = item.optString("notes", item.optString("note", "")),
                        sourceModule = item.optString("sourceModule", item.optString("source_module", "")),
                        sourceItemId = item.optString("sourceItemId", item.optString("source_item_id", ""))
                    )
                }

                runOnUiThread {
                    featureStore.applyRemote(
                        snapshot.budgets,
                        snapshot.goals,
                        stateResponse
                    )
                    if (remoteAccounts.isNotEmpty()) saveAccounts(remoteAccounts)
                    saveList("entries", syncedEntries)
                    saveList("future", remoteFuture)
                    prefs.edit()
                        .putString("last_sync_date", LocalDate.now().toString())
                        .remove("last_sync_error")
                        .apply()
                    FinanzaWidgets.updateAll(this@MainActivity)
                    render()
                    if (!silent) Toast.makeText(this@MainActivity, "Next sincronizado.", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    val message = e.message ?: "Falha ao sincronizar."
                    rememberSyncError(message)
                    render()
                    if (!silent) Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    onError?.invoke(message)
                }
            }
        }
    }

    private fun syncEntryChangeAsync(mode: String, entry: Entry? = null, previous: Entry? = null) {
        if (!isOnlineMode()) {
            if (apiClient.hasCredentials) {
                when {
                    mode == "delete" && previous != null && isTempLocalId(previous.id) ->
                        FinanzaSyncQueue.cancelPendingCreate(prefs, previous.id)
                    mode == "update" && entry != null && previous != null && isTempLocalId(previous.id) -> {
                        FinanzaSyncQueue.cancelPendingCreate(prefs, previous.id)
                        enqueueCoreMutation("create", entry, null)
                    }
                    else -> enqueueCoreMutation(mode, entry, previous)
                }
            }
            FinanzaWidgets.updateAll(this)
            render()
            return
        }
        thread {
            val mutation = runCatching {
                when {
                    entry?.type == "due" || previous?.type == "due" -> syncFutureStateRemote()
                    mode == "delete" && previous != null && isTempLocalId(previous.id) -> {
                        FinanzaSyncQueue.cancelPendingCreate(prefs, previous.id)
                    }
                    mode == "delete" && previous != null && !isTempLocalId(previous.id) -> remoteRepository.deleteTransaction(previous.id)
                    mode == "update" && entry != null && previous != null -> {
                        if (isTempLocalId(previous.id)) {
                            FinanzaSyncQueue.cancelPendingCreate(prefs, previous.id)
                            remoteRepository.createTransaction(transactionPayload(entry))
                        }
                        else remoteRepository.updateTransaction(previous.id, transactionPayload(entry))
                    }
                    mode == "create" && entry != null -> remoteRepository.createTransaction(transactionPayload(entry))
                }
            }
            if (mutation.isFailure) {
                enqueueCoreMutation(mode, entry, previous)
                runOnUiThread {
                    rememberSyncError(mutation.exceptionOrNull()?.message ?: "Falha ao sincronizar lancamento.")
                    Toast.makeText(this@MainActivity, "Não foi possível sincronizar agora. Seus dados locais foram mantidos.", Toast.LENGTH_SHORT).show()
                    FinanzaWidgets.updateAll(this@MainActivity)
                }
                return@thread
            }
            syncRemoteNow(silent = true)
        }
    }

    private fun syncFutureStateRemote() {
        val state = remoteRepository.loadState()
        val settings = state.optJSONObject("settings") ?: JSONObject()
        val duePayload = JSONArray().apply {
            futureEntries().forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.title)
                    put("title", item.title)
                    put("category", item.category)
                    put("amount", item.amount)
                    put("date", item.date)
                    put("nextDueDate", item.date)
                    put("notes", item.note)
                    put("frequency", "once")
                    put("status", "active")
                    if (item.sourceModule.isNotBlank()) put("sourceModule", item.sourceModule)
                    if (item.sourceItemId.isNotBlank()) put("sourceItemId", item.sourceItemId)
                })
            }
        }
        settings.put("android_next_due_items", duePayload)
        val rates = settings.optJSONObject("rates") ?: JSONObject()
        rates.put("dueItems", duePayload)
        settings.put("rates", rates)
        state.put("settings", settings)
        remoteRepository.saveState(state)
    }

    private fun syncAccountsRemoteAsync() {
        if (!isOnlineMode()) {
            if (apiClient.hasCredentials) {
                FinanzaSyncQueue.enqueueSingleton(prefs, "accounts", JSONObject().put("mode", "accounts"))
                render()
            }
            return
        }
        thread {
            runCatching(::syncAccountsRemoteBlocking)
                .onSuccess { prefs.edit().remove("last_sync_error").apply() }
                .onFailure { error ->
                    FinanzaSyncQueue.enqueueSingleton(prefs, "accounts", JSONObject().put("mode", "accounts"))
                    runOnUiThread {
                        rememberSyncError(error.message ?: "Falha ao sincronizar contas.")
                        render()
                        Toast.makeText(this@MainActivity, "Contas salvas no aparelho e adicionadas a fila.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun syncAccountsRemoteBlocking() {
        remoteRepository.updateState { state ->
            state.put("accounts", JSONArray().apply {
                accounts().forEach { account ->
                    put(JSONObject().apply {
                        put("id", account.id)
                        put("name", account.name)
                        put("icon", account.icon)
                        put("type", account.type)
                        put("balance", account.balance)
                        put("yield_rate", account.yieldRate)
                        put("yield_type", account.yieldType)
                        put("yield_val", account.yieldVal)
                        put("calc_base", account.calcBase)
                        put("start_date", account.startDate)
                        put("card_closing_day", account.cardClosingDay)
                        put("card_due_day", account.cardDueDay)
                        put("card_last4", account.cardLast4)
                        put("card_expiry", account.cardExpiry)
                        put("note", account.note)
                    })
                }
            })
        }
    }

    private fun enqueueCoreMutation(mode: String, entry: Entry?, previous: Entry?) {
        if (entry?.type == "due" || previous?.type == "due") {
            FinanzaSyncQueue.enqueueSingleton(prefs, "future_state", JSONObject().put("mode", "future_state"))
            return
        }
        FinanzaSyncQueue.enqueueLegacy(prefs, JSONObject().apply {
            put("mode", mode)
            put("entry", entry?.let(::entryJson) ?: JSONObject.NULL)
            put("previous", previous?.let(::entryJson) ?: JSONObject.NULL)
            put("createdAt", System.currentTimeMillis())
        })
    }

    private fun flushCoreQueueBlocking() = synchronized(FinanzaSyncQueue) {
        val queue = runCatching { JSONArray(prefs.getString("core_sync_queue", "[]")) }.getOrDefault(JSONArray())
        if (queue.length() == 0) return@synchronized
        for (index in 0 until queue.length()) {
            val item = queue.optJSONObject(index) ?: continue
            try {
                val requestPath = item.optString("path")
                if (item.optString("mode") in setOf("accounts", "future_state")) {
                    if (item.optString("mode") == "accounts") syncAccountsRemoteBlocking() else syncFutureStateRemote()
                } else if (requestPath.isNotBlank()) {
                    requestJson(
                        item.optString("method", "POST"),
                        requestPath,
                        item.optJSONObject("body")
                    )
                } else {
                    val mode = item.optString("mode")
                    val entry = item.optJSONObject("entry")?.let(::entryFromJson)
                    val previous = item.optJSONObject("previous")?.let(::entryFromJson)
                    when {
                        entry?.type == "due" || previous?.type == "due" -> syncFutureStateRemote()
                        mode == "delete" && previous != null && !isTempLocalId(previous.id) -> remoteRepository.deleteTransaction(previous.id)
                        mode == "update" && entry != null && previous != null -> {
                            if (isTempLocalId(previous.id)) remoteRepository.createTransaction(transactionPayload(entry))
                            else remoteRepository.updateTransaction(previous.id, transactionPayload(entry))
                        }
                        mode == "create" && entry != null -> remoteRepository.createTransaction(transactionPayload(entry))
                    }
                }
            } catch (error: Exception) {
                persistRemainingCoreQueue(queue, index)
                throw error
            }
        }
        prefs.edit().putString("core_sync_queue", "[]").apply()
    }

    private fun persistRemainingCoreQueue(queue: JSONArray, failedIndex: Int) {
        val remaining = JSONArray()
        for (index in failedIndex until queue.length()) remaining.put(queue.opt(index))
        prefs.edit().putString("core_sync_queue", remaining.toString()).apply()
    }

    private fun rememberSyncError(message: String) {
        prefs.edit().putString("last_sync_error", message.trim().take(120)).apply()
    }

    private fun entryFromJson(item: JSONObject): Entry? = runCatching {
        Entry(
            item.optLong("id"), item.optString("title"), item.optString("category"), item.optDouble("amount"),
            item.optString("type"), item.optString("date"), item.optString("accountId"), item.optString("note"),
            item.optString("purchaseDate", item.optString("date")), item.optString("installmentGroup"), item.optInt("installmentNum"),
            item.optInt("installmentTotal"), item.optString("recurGroup"), item.optJSONObject("splitMeta")?.toString().orEmpty(),
            item.optBoolean("paid"), item.optBoolean("pending"), item.optString("sourceModule"), item.optString("sourceItemId")
        )
    }.getOrNull()

    private fun syncFeatureMutationAsync(mutation: FeatureMutation) {
        if (!isOnlineMode()) {
            featureStore.enqueue(mutation)
            return
        }
        thread {
            runCatching { syncFeatureMutationBlocking(mutation) }
                .onSuccess { syncRemoteNow(silent = true) }
                .onFailure {
                    featureStore.enqueue(mutation)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Alteração salva no aparelho e adicionada à fila de sincronização.", Toast.LENGTH_SHORT).show()
                        render()
                    }
                }
        }
    }

    private fun flushFeatureQueueBlocking() {
        val pending = featureStore.pendingMutations()
        if (pending.isEmpty()) return
        pending.forEach { mutation ->
            syncFeatureMutationBlocking(mutation)
            featureStore.acknowledge(mutation)
        }
    }

    private fun syncFeatureMutationBlocking(mutation: FeatureMutation) {
        val payload = runCatching { JSONObject(mutation.payload) }.getOrDefault(JSONObject())
        when (mutation.moduleId) {
            "budgets" -> when (mutation.action) {
                "delete" -> if (!mutation.itemId.startsWith("android_")) remoteRepository.deleteBudget(mutation.itemId)
                else -> {
                    if (mutation.action == "update" && !mutation.itemId.startsWith("android_")) {
                        remoteRepository.deleteBudget(mutation.itemId)
                    }
                    remoteRepository.createBudget(JSONObject().apply {
                        put("category", payload.optString("category", "Geral"))
                        put("limit", payload.optDouble("limit", 0.0))
                    })
                }
            }
            "goals" -> when (mutation.action) {
                "delete" -> if (!mutation.itemId.startsWith("android_")) remoteRepository.deleteGoal(mutation.itemId)
                "primary" -> if (mutation.itemId.startsWith("android_")) {
                    remoteRepository.createGoal(goalPayload(payload))
                } else {
                    remoteRepository.addGoalContribution(mutation.itemId, payload.optDouble("monthly", 1.0).coerceAtLeast(1.0))
                }
                else -> {
                    if (mutation.action == "update" && !mutation.itemId.startsWith("android_")) {
                        remoteRepository.deleteGoal(mutation.itemId)
                    }
                    remoteRepository.createGoal(goalPayload(payload))
                }
            }
            else -> {
                remoteRepository.updateState { state ->
                    val featureState = featureStore.statePayload(state)
                    val keys = featureState.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        state.put(key, featureState.opt(key))
                    }
                }
            }
        }
    }

    private fun goalPayload(source: JSONObject) = JSONObject().apply {
        put("name", source.optString("name", "Meta"))
        put("icon", source.optString("icon", "\uD83C\uDFAF"))
        put("target", source.optDouble("target", 0.0))
        put("current", source.optDouble("current", 0.0))
        put("deadline", source.optString("deadline", LocalDate.now().plusMonths(6).toString()))
        put("description", source.optString("desc", source.optString("description", "")))
        put("monthly", source.optDouble("monthly", 0.0))
    }

    private fun exportLocalBackup() {
        val payload = buildFullBackup().toString(2)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "Backup Next Android")
            putExtra(Intent.EXTRA_TEXT, payload)
        }
        runCatching {
            startActivity(Intent.createChooser(shareIntent, "Compartilhar backup"))
        }.onFailure {
            Toast.makeText(this, "Não foi possível abrir o compartilhamento.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun prefArray(key: String): JSONArray =
        runCatching { JSONArray(prefs.getString(key, "[]") ?: "[]") }.getOrDefault(JSONArray())

    private fun buildFullBackup(): JSONObject {
        val features = featureStore.backupPayload()
        val settings = features.optJSONObject("settings") ?: JSONObject()
        settings.put("user_name", prefs.getString("user_name", "Voce") ?: "Voce")
            .put("monthly_budget", prefs.getFloat("monthly_budget", 5000f).toDouble())
            .put("theme_mode", themeMode())
            .put("visual_experience", visualExperience().id)
            .put("accent", accentId)
            .put("last_sync_date", prefs.getString("last_sync_date", "") ?: "")
            .put("widget_prefs", JSONObject(prefs.getString("widget_prefs", "{}") ?: "{}"))
            .put("widget_order", prefArray("widget_order"))
            .put("android_next_due_items", prefArray("future"))
        return JSONObject().apply {
            put("version", "1.2.2-next")
            put("app", "Finanza")
            put("exported_at", java.time.Instant.now().toString())
            put("privacy_mode", privacyMode())
            put("online_mode", isOnlineMode())
            put("transactions", JSONArray().apply {
                entries().forEach { entry -> put(transactionPayload(entry).put("id", entry.id)) }
            })
            put("entries", prefArray("entries"))
            put("future", prefArray("future"))
            put("dueItems", prefArray("future"))
            put("accounts", prefArray("accounts"))
            put("budgets", features.optJSONArray("budgets") ?: JSONArray())
            put("goals", features.optJSONArray("goals") ?: JSONArray())
            put("shopping", features.optJSONObject("shopping") ?: JSONObject())
            put("car", features.optJSONObject("car") ?: JSONObject())
            put("settings", settings)
        }
    }

    private fun importBackupFromUri(uri: Uri) {
        thread {
            runCatching {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Arquivo vazio")
                JSONObject(raw)
            }.onSuccess { data ->
                runOnUiThread { showBackupImportPreview(data) }
            }.onFailure { error ->
                runOnUiThread { Toast.makeText(this@MainActivity, error.message ?: "Falha ao importar backup.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun showBackupImportPreview(data: JSONObject) {
        val importedTransactions = data.optJSONArray("transactions") ?: data.optJSONArray("entries") ?: JSONArray()
        val known = entries().map(::entryFingerprint).toSet()
        var duplicateCount = 0
        for (index in 0 until importedTransactions.length()) {
            val item = importedTransactions.optJSONObject(index) ?: continue
            val candidate = Entry(
                id = item.optLong("id", index.toLong()),
                title = item.optString("description", item.optString("title", item.optString("desc", "Lancamento"))),
                category = item.optString("category", "A classificar"),
                amount = item.optDouble("amount", 0.0),
                type = item.optString("type", "expense"),
                date = item.optString("date", LocalDate.now().toString()).take(10)
            )
            if (entryFingerprint(candidate) in known) duplicateCount++
        }
        showAppDialog("Revisar backup", "Confira o conteudo antes de alterar os dados deste aparelho.") { dialog ->
            addView(healthRow("\uD83E\uDDFE", "Transações", importedTransactions.length().toString()))
            addView(healthRow("\uD83C\uDFE6", "Contas", (data.optJSONArray("accounts") ?: JSONArray()).length().toString()))
            addView(healthRow("\uD83C\uDFAF", "Orçamentos e metas", "${(data.optJSONArray("budgets") ?: JSONArray()).length()} + ${(data.optJSONArray("goals") ?: JSONArray()).length()}"))
            addView(healthRow("\uD83D\uDCC5", "Vencimentos", ((data.optJSONArray("dueItems") ?: data.optJSONArray("future")) ?: JSONArray()).length().toString()))
            addView(body("$duplicateCount transacao(oes) ja existem e serao ignoradas ao mesclar.", 12, muted).apply { setPadding(0, dp(10), 0, dp(4)) })
            addView(primaryButton("Mesclar sem duplicar") { performBackupImport(data, true, dialog) })
            addView(secondaryButton("Substituir dados locais") { performBackupImport(data, false, dialog) })
        }
    }

    private fun performBackupImport(data: JSONObject, merge: Boolean, dialog: Dialog) {
        dialog.dismiss()
        thread {
            runCatching {
                applyBackup(data, merge)
                syncFullImportOrQueue()
            }.onSuccess {
                runOnUiThread {
                    render()
                    FinanzaWidgets.updateAll(this@MainActivity)
                    Toast.makeText(this@MainActivity, if (merge) "Backup mesclado sem duplicar." else "Backup restaurado.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                runOnUiThread { Toast.makeText(this@MainActivity, error.message ?: "Falha ao importar backup.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun applyBackup(data: JSONObject, merge: Boolean) {
        featureStore.importBackup(data, merge)
        val importedAccounts = data.optJSONArray("accounts") ?: JSONArray()
        if (importedAccounts.length() > 0 || !merge) {
            val current = if (merge) accounts().associateBy { it.id }.toMutableMap() else mutableMapOf()
            for (index in 0 until importedAccounts.length()) {
                val item = importedAccounts.optJSONObject(index) ?: continue
                val account = Account(
                    item.optString("id", "android_account_$index"),
                    item.optString("name", "Conta"),
                    item.optString("icon", accountTypeIcon(item.optString("type", "checking"))),
                    item.optString("type", "checking"),
                    item.optDouble("balance", 0.0),
                    item.optDouble("yieldRate", item.optDouble("yield_rate", 0.0)),
                    item.optString("yieldType", item.optString("yield_type", "manual")),
                    item.optDouble("yieldVal", item.optDouble("yield_val", 0.0)),
                    item.optString("calcBase", item.optString("calc_base", "du")),
                    item.optString("startDate", item.optString("start_date", "")),
                    item.optInt("cardClosingDay", item.optInt("card_closing_day", 0)),
                    item.optInt("cardDueDay", item.optInt("card_due_day", 0)),
                    item.optString("cardLast4", item.optString("card_last4", "")),
                    item.optString("cardExpiry", item.optString("card_expiry", "")),
                    item.optString("note", "")
                )
                current[account.id] = account
            }
            saveAccounts(current.values.toList().ifEmpty(::defaultAccounts))
        }
        val sourceTransactions = data.optJSONArray("transactions") ?: data.optJSONArray("entries") ?: JSONArray()
        val currentEntries = if (merge) entries().toMutableList() else mutableListOf()
        val fingerprints = currentEntries.map { "${it.date}|${it.type}|${it.amount}|${it.title.lowercase()}" }.toMutableSet()
        for (index in 0 until sourceTransactions.length()) {
            val item = sourceTransactions.optJSONObject(index) ?: continue
            val type = item.optString("type", "expense")
            if (type != "expense" && type != "income") continue
            val entry = Entry(
                item.optLong("id", System.currentTimeMillis() + index),
                item.optString("description", item.optString("title", item.optString("desc", "Lancamento"))),
                item.optString("category", "A classificar"),
                item.optDouble("amount", 0.0),
                type,
                item.optString("date", LocalDate.now().toString()).take(10),
                item.optString("account_id", item.optString("accountId", defaultExpenseAccountId())),
                item.optString("note", ""),
                item.optString("purchase_date", item.optString("purchaseDate", item.optString("date", LocalDate.now().toString()))).take(10),
                item.optString("installment_group", item.optString("installmentGroup", "")),
                item.optInt("installment_num", item.optInt("installmentNum", 0)),
                item.optInt("installment_total", item.optInt("installmentTotal", 0)),
                item.optString("recur_group", item.optString("recurGroup", "")),
                (item.optJSONObject("split_meta") ?: item.optJSONObject("splitMeta"))?.toString().orEmpty(),
                item.optBoolean("paid", false),
                item.optBoolean("pending", false),
                item.optString("sourceModule", item.optString("source_module", "")),
                item.optString("sourceItemId", item.optString("source_item_id", ""))
            )
            val fingerprint = "${entry.date}|${entry.type}|${entry.amount}|${entry.title.lowercase()}"
            if (entry.amount > 0 && fingerprints.add(fingerprint)) currentEntries.add(entry)
        }
        saveList("entries", currentEntries)
        val due = data.optJSONArray("dueItems") ?: data.optJSONArray("future")
            ?: data.optJSONObject("settings")?.optJSONArray("android_next_due_items") ?: JSONArray()
        if (due.length() > 0 || !merge) {
            val currentDue = if (merge) futureEntries().toMutableList() else mutableListOf()
            val known = currentDue.map { it.id }.toMutableSet()
            for (index in 0 until due.length()) {
                val item = due.optJSONObject(index) ?: continue
                val id = item.optLong("id", System.currentTimeMillis() + index)
                if (!known.add(id)) continue
                currentDue.add(Entry(
                    id = id,
                    title = item.optString("name", item.optString("title", "Conta")),
                    category = item.optString("category", "Geral"),
                    amount = item.optDouble("amount"),
                    type = "due",
                    date = item.optString("date", item.optString("nextDueDate", LocalDate.now().toString())).take(10),
                    note = item.optString("notes", item.optString("note", "")),
                    sourceModule = item.optString("sourceModule", item.optString("source_module", "")),
                    sourceItemId = item.optString("sourceItemId", item.optString("source_item_id", ""))
                ))
            }
            saveList("future", currentDue)
        }
        data.optJSONObject("settings")?.let { settings ->
            val widgetPrefs = settings.optJSONObject("widget_prefs")
            val widgetOrder = settings.optJSONArray("widget_order")
            if (widgetPrefs != null) prefs.edit().putString("widget_prefs", widgetPrefs.toString()).remove("dashboard_widget_active").apply()
            if (widgetOrder != null) prefs.edit().putString("widget_order", widgetOrder.toString()).putString("dashboard_widget_order", widgetOrder.toString()).apply()
        }
    }

    private fun importTransactionsFromUri(uri: Uri) {
        thread {
            runCatching {
                val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Arquivo vazio")
                val imported = FinanzaImportParser.parse(raw, defaultExpenseAccountId()).map(::importedEntry)
                if (imported.isEmpty()) error("Nenhuma transacao valida encontrada")
                imported
            }.onSuccess { imported ->
                runOnUiThread { showTransactionImportPreview(imported) }
            }.onFailure { error ->
                runOnUiThread { Toast.makeText(this@MainActivity, error.message ?: "Falha ao importar.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun showTransactionImportPreview(imported: List<Entry>) {
        val known = entries().map(::entryFingerprint).toSet()
        val distinct = imported.distinctBy(::entryFingerprint)
        val unique = distinct.filterNot { entryFingerprint(it) in known }
        val conflicts = distinct.filter { entryFingerprint(it) in known }
        val duplicates = imported.size - distinct.size + conflicts.size
        showAppDialog("Revisar importacao", "CSV e OFX entram como dados a classificar; duplicadas ficam de fora.") { dialog ->
            addView(healthRow("\uD83D\uDCE5", "Reconhecidas", imported.size.toString()))
            addView(healthRow("\u2728", "Novas", unique.size.toString()))
            addView(healthRow("\uD83D\uDD01", "Duplicadas", duplicates.toString()))
            unique.take(5).forEach { entry ->
                addView(body("${formatDate(entry.date)}  ${entry.title}  ${money.format(entry.amount)}", 12, ink).apply {
                    setPadding(0, dp(7), 0, 0)
                })
            }
            if (unique.size > 5) addView(body("+ ${unique.size - 5} outros lancamentos", 12, muted))
            addView(primaryButton("Importar ${unique.size} novos") { performTransactionImport(unique, dialog, false) })
            if (conflicts.isNotEmpty()) {
                addView(body("Conflitos encontrados:", 12, muted).apply { setPadding(0, dp(10), 0, 0) })
                conflicts.take(3).forEach { entry ->
                    addView(body("${formatDate(entry.date)}  ${entry.title}  ${money.format(entry.amount)}", 12, ink).apply { setPadding(0, dp(6), 0, 0) })
                }
                addView(secondaryButton("Usar importado em ${conflicts.size} conflitos") {
                    performTransactionImport(distinct, dialog, true)
                })
            }
        }
    }

    private fun performTransactionImport(imported: List<Entry>, dialog: Dialog, replaceConflicts: Boolean) {
        if (imported.isEmpty()) {
            Toast.makeText(this, "Nenhuma transacao nova para importar.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            return
        }
        dialog.dismiss()
        thread {
            runCatching {
                val current = entries().toMutableList()
                val indexes = current.mapIndexed { index, entry -> entryFingerprint(entry) to index }.toMap()
                imported.forEach { incoming ->
                    val existingIndex = indexes[entryFingerprint(incoming)]
                    if (replaceConflicts && existingIndex != null) {
                        current[existingIndex] = incoming.copy(id = current[existingIndex].id)
                    } else if (existingIndex == null) {
                        current += incoming
                    }
                }
                saveList("entries", current)
                syncFullImportOrQueue()
                imported.size
            }.onSuccess { count ->
                runOnUiThread {
                    render()
                    FinanzaWidgets.updateAll(this@MainActivity)
                    Toast.makeText(this@MainActivity, "$count transacoes importadas.", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                runOnUiThread { Toast.makeText(this@MainActivity, error.message ?: "Falha ao importar.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun entryFingerprint(entry: Entry): String = "${entry.date}|${entry.type}|${"%.2f".format(Locale.US, entry.amount)}|${entry.title.trim().lowercase()}"

    private fun importedEntry(item: FinanzaImportedTransaction): Entry = Entry(
        id = item.id,
        title = item.description,
        category = item.category,
        amount = item.amount,
        type = item.type,
        date = item.date,
        accountId = item.accountId
    )

    private fun syncFullImportOrQueue() {
        if (!apiClient.hasCredentials) return
        val payload = buildFullBackup()
        if (!apiClient.isConfigured) {
            FinanzaSyncQueue.enqueueSingleton(
                prefs,
                "full_import",
                JSONObject().put("method", "PUT").put("path", FinanzaApiRoutes.IMPORT).put("body", payload)
            )
            return
        }
        runCatching { remoteRepository.importBackup(payload) }.onFailure { error ->
            FinanzaSyncQueue.enqueueSingleton(
                prefs,
                "full_import",
                JSONObject().put("method", "PUT").put("path", FinanzaApiRoutes.IMPORT).put("body", payload)
            )
            rememberSyncError(error.message ?: "Importação aguardando sincronização.")
        }
    }

    private fun parseOfx(raw: String): List<Entry> {
        val blocks = Regex("<STMTTRN>(.*?)(?=<STMTTRN>|</BANKTRANLIST>|</CCSTMTRS>)", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(raw).map { it.groupValues[1] }
        return blocks.mapNotNull { block ->
            fun tag(name: String) = Regex("<$name>([^<\\r\\n]+)", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)?.trim().orEmpty()
            val signed = tag("TRNAMT").replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
            val rawDate = tag("DTPOSTED").take(8)
            val date = if (rawDate.length == 8) "${rawDate.take(4)}-${rawDate.substring(4, 6)}-${rawDate.takeLast(2)}" else LocalDate.now().toString()
            Entry(
                id = tag("FITID").hashCode().toLong().let { if (it == 0L) System.currentTimeMillis() else kotlin.math.abs(it) },
                title = tag("NAME").ifBlank { tag("MEMO") }.ifBlank { "Importado OFX" },
                category = "A classificar",
                amount = kotlin.math.abs(signed),
                type = if (signed >= 0) "income" else "expense",
                date = date,
                accountId = defaultExpenseAccountId()
            )
        }.toList()
    }

    private fun parseCsv(raw: String): List<Entry> {
        val lines = raw.lineSequence().filter { it.isNotBlank() }.toList()
        if (lines.size < 2) return emptyList()
        val separator = if (lines.first().count { it == ';' } >= lines.first().count { it == ',' }) ';' else ','
        val headers = parseDelimitedLine(lines.first(), separator).map { it.trim().lowercase().replace("_", " ") }
        fun column(vararg names: String) = headers.indexOfFirst { header -> names.any { header.contains(it) } }
        val dateIndex = column("data", "date")
        val descIndex = column("descricao", "descrição", "description", "historico", "lançamento", "nome")
        val amountIndex = column("valor", "amount", "total")
        val typeIndex = column("tipo", "type")
        val categoryIndex = column("categoria", "category")
        if (amountIndex < 0) return emptyList()
        return lines.drop(1).mapIndexedNotNull { index, line ->
            val cells = parseDelimitedLine(line, separator)
            val signed = parseImportedAmount(cells.getOrNull(amountIndex).orEmpty()) ?: return@mapIndexedNotNull null
            val explicitType = cells.getOrNull(typeIndex).orEmpty().lowercase()
            val type = when {
                explicitType.contains("receita") || explicitType.contains("income") || explicitType.contains("entrada") -> "income"
                explicitType.contains("gasto") || explicitType.contains("expense") || explicitType.contains("saida") -> "expense"
                signed >= 0 -> "income"
                else -> "expense"
            }
            Entry(
                System.currentTimeMillis() + index,
                cells.getOrNull(descIndex).orEmpty().trim().ifBlank { "Importado CSV" },
                cells.getOrNull(categoryIndex).orEmpty().trim().ifBlank { "A classificar" },
                kotlin.math.abs(signed), type,
                normalizeImportedDate(cells.getOrNull(dateIndex).orEmpty()),
                defaultExpenseAccountId()
            )
        }
    }

    private fun parseDelimitedLine(line: String, separator: Char): List<String> {
        val cells = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> { current.append('"'); index++ }
                char == '"' -> quoted = !quoted
                char == separator && !quoted -> { cells.add(current.toString()); current.clear() }
                else -> current.append(char)
            }
            index++
        }
        cells.add(current.toString())
        return cells
    }

    private fun parseImportedAmount(raw: String): Double? {
        val clean = raw.replace("R$", "", ignoreCase = true).replace(" ", "")
        val normalized = if (clean.contains(',') && clean.lastIndexOf(',') > clean.lastIndexOf('.')) clean.replace(".", "").replace(',', '.') else clean.replace(",", "")
        return normalized.toDoubleOrNull()
    }

    private fun normalizeImportedDate(raw: String): String {
        val value = raw.trim().take(10)
        if (runCatching { LocalDate.parse(value) }.isSuccess) return value
        return runCatching { LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrDefault(LocalDate.now()).toString()
    }

    private fun showDataHealth() {
        showAppDialog("Diagnóstico", "Estado deste aparelho no Finanza.") { dialog ->
            val lastSync = prefs.getString("last_sync_date", "")?.ifBlank { "Nunca" } ?: "Nunca"
            addView(healthRow("\uD83D\uDCCB", "Movimentos", entries().size.toString()))
            addView(healthRow("\uD83C\uDFE6", "Contas", accounts().size.toString()))
            addView(healthRow("\uD83D\uDCC5", "Agenda", futureEntries().size.toString()))
            addView(healthRow("\uD83D\uDD10", "Modo", if (isOnlineMode()) "Online" else "Local"))
            addView(healthRow("\uD83D\uDD04", "Ultimo sync", lastSync))
            addView(primaryButton("Sincronizar") {
                if (isOnlineMode()) {
                    syncRemoteNow(silent = false)
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                    showOnlineAccessDialog()
                }
            })
            addView(secondaryButton("Compartilhar backup") { exportLocalBackup() })
        }
    }

    private fun requestJson(method: String, path: String, body: JSONObject? = null): JSONObject {
        return apiClient.requestJson(method, path, body)
    }

    private fun transactionPayload(entry: Entry): JSONObject = JSONObject().apply {
        put("type", entry.type)
        put("description", entry.title)
        put("amount", entry.amount)
        put("category", entry.category)
        put("date", entry.date)
        put("purchase_date", entry.purchaseDate)
        put("note", entry.note)
        if (entry.accountId.isNotBlank()) put("account_id", entry.accountId)
        put("paid", entry.paid)
        put("pending", entry.pending)
        if (entry.installmentGroup.isNotBlank()) put("installment_group", entry.installmentGroup)
        if (entry.installmentNum > 0) put("installment_num", entry.installmentNum)
        if (entry.installmentTotal > 0) put("installment_total", entry.installmentTotal)
        if (entry.recurGroup.isNotBlank()) put("recur_group", entry.recurGroup)
        if (entry.splitMeta.isNotBlank()) put("split_meta", runCatching { JSONObject(entry.splitMeta) }.getOrDefault(JSONObject()))
        if (entry.accountId.isNotBlank()) put("account_id", entry.accountId)
    }

    private fun isTempLocalId(id: Long): Boolean = id > 1000000000000L

    private fun saveList(key: String, items: List<Entry>) {
        val array = JSONArray()
        items.forEach { array.put(entryJson(it)) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun accounts(): List<Account> {
        val array = runCatching { JSONArray(prefs.getString("accounts", "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    Account(
                        id = item.optString("id", "acc_$index"),
                        name = item.optString("name", "Conta"),
                        icon = item.optString("icon", accountTypeIcon(item.optString("type", "checking"))),
                        type = item.optString("type", "checking"),
                        balance = item.optDouble("balance", 0.0),
                        yieldRate = item.optDouble("yieldRate", item.optDouble("yield_rate", 0.0)),
                        yieldType = item.optString("yieldType", item.optString("yield_type", "manual")),
                        yieldVal = item.optDouble("yieldVal", item.optDouble("yield_val", 0.0)),
                        calcBase = item.optString("calcBase", item.optString("calc_base", "du")),
                        startDate = item.optString("startDate", item.optString("start_date", "")),
                        cardClosingDay = item.optInt("cardClosingDay", item.optInt("card_closing_day", 0)),
                        cardDueDay = item.optInt("cardDueDay", item.optInt("card_due_day", 0)),
                        cardLast4 = item.optString("cardLast4", item.optString("card_last4", "")),
                        cardExpiry = item.optString("cardExpiry", item.optString("card_expiry", "")),
                        note = item.optString("note", "")
                    )
                )
            }
        }
    }

    private fun saveAccounts(items: List<Account>) {
        val array = JSONArray()
        items.forEach { account ->
            array.put(JSONObject().apply {
                put("id", account.id)
                put("name", account.name)
                put("icon", account.icon)
                put("type", account.type)
                put("balance", account.balance)
                put("yieldRate", account.yieldRate)
                put("yieldType", account.yieldType)
                put("yieldVal", account.yieldVal)
                put("calcBase", account.calcBase)
                put("startDate", account.startDate)
                put("cardClosingDay", account.cardClosingDay)
                put("cardDueDay", account.cardDueDay)
                put("cardLast4", account.cardLast4)
                put("cardExpiry", account.cardExpiry)
                put("note", account.note)
            })
        }
        prefs.edit().putString("accounts", array.toString()).apply()
    }

    private fun saveAccount(account: Account) {
        val current = accounts().toMutableList()
        val index = current.indexOfFirst { it.id == account.id }
        if (index >= 0) current[index] = account else current.add(account)
        saveAccounts(current)
    }

    private fun deleteAccount(id: String) {
        val remaining = accounts().filterNot { it.id == id }
        saveAccounts(if (remaining.isEmpty()) defaultAccounts() else remaining)
    }

    private fun entries(): List<Entry> = load("entries")
    private fun futureEntries(): List<Entry> = load("future")

    private fun saveEntry(entry: Entry) {
        val key = if (entry.type == "due") "future" else "entries"
        val array = JSONArray(prefs.getString(key, "[]"))
        array.put(entryJson(entry))
        prefs.edit().putString(key, array.toString()).apply()
        FinanzaWidgets.updateAll(this)
    }

    private fun replaceEntry(old: Entry, updated: Entry) {
        deleteEntry(old); saveEntry(updated)
    }

    private fun settleDue(entry: Entry) {
        val expense = Entry(
            id = System.currentTimeMillis(),
            title = entry.title,
            category = entry.category,
            amount = entry.amount,
            type = "expense",
            date = LocalDate.now().toString(),
            accountId = defaultExpenseAccountId(),
            paid = true
        )
        deleteEntry(entry)
        saveEntry(expense)
        syncEntryChangeAsync("delete", previous = entry)
        syncEntryChangeAsync("create", entry = expense)
        if (entry.sourceModule.isNotBlank() && entry.sourceItemId.isNotBlank()) {
            featureStore.advanceAfterDuePaid(entry.sourceModule, entry.sourceItemId)?.let { mutation ->
                syncFeatureMutationAsync(upsertDueFromCommitment(mutation))
            }
        }
        Toast.makeText(this, "Compromisso pago e lancado.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun postponeDue(entry: Entry, days: Long) {
        val updated = entry.copy(date = LocalDate.parse(entry.date).plusDays(days).toString())
        replaceEntry(entry, updated)
        syncEntryChangeAsync("update", entry = updated, previous = entry)
        Toast.makeText(this, "Compromisso adiado.", Toast.LENGTH_SHORT).show()
        render()
    }

    private fun deleteEntry(entry: Entry) {
        val key = if (entry.type == "due") "future" else "entries"
        val filtered = load(key).filterNot { it.id == entry.id }
        val array = JSONArray(); filtered.forEach { array.put(entryJson(it)) }
        prefs.edit().putString(key, array.toString()).apply()
        FinanzaWidgets.updateAll(this)
    }

    private fun entryJson(entry: Entry) = JSONObject().apply {
        put("id", entry.id); put("title", entry.title); put("category", entry.category)
        put("amount", entry.amount); put("type", entry.type); put("date", entry.date); put("accountId", entry.accountId)
        put("note", entry.note); put("purchaseDate", entry.purchaseDate)
        put("installmentGroup", entry.installmentGroup); put("installmentNum", entry.installmentNum); put("installmentTotal", entry.installmentTotal)
        put("recurGroup", entry.recurGroup); put("paid", entry.paid); put("pending", entry.pending)
        put("sourceModule", entry.sourceModule); put("sourceItemId", entry.sourceItemId)
        if (entry.splitMeta.isNotBlank()) put("splitMeta", runCatching { JSONObject(entry.splitMeta) }.getOrDefault(JSONObject()))
    }

    private fun load(key: String): List<Entry> {
        val array = runCatching { JSONArray(prefs.getString(key, "[]")) }.getOrDefault(JSONArray())
        return (0 until array.length()).mapNotNull { index ->
            runCatching {
                val item = array.getJSONObject(index)
                val amount = item.optDouble("amount")
                if (!amount.isFinite()) return@runCatching null
                Entry(
                    item.optLong("id").takeIf { it != 0L } ?: (Long.MIN_VALUE + index),
                    item.optString("title"),
                    item.optString("category"),
                    amount,
                    item.optString("type"),
                    item.optString("date"),
                    item.optString("accountId", item.optString("account_id", accounts().firstOrNull()?.id.orEmpty())),
                    item.optString("note", ""),
                    item.optString("purchaseDate", item.optString("purchase_date", item.optString("date"))),
                    item.optString("installmentGroup", item.optString("installment_group", "")),
                    item.optInt("installmentNum", item.optInt("installment_num", 0)),
                    item.optInt("installmentTotal", item.optInt("installment_total", 0)),
                    item.optString("recurGroup", item.optString("recur_group", "")),
                    (item.optJSONObject("splitMeta") ?: item.optJSONObject("split_meta"))?.toString().orEmpty(),
                    item.optBoolean("paid", false),
                    item.optBoolean("pending", false),
                    item.optString("sourceModule", item.optString("source_module", "")),
                    item.optString("sourceItemId", item.optString("source_item_id", ""))
                )
            }.getOrNull()
        }
    }

    private fun defaultExpenseAccountId(): String {
        val preferred = accounts().firstOrNull { it.type != "credit" }?.id
        return preferred ?: accounts().firstOrNull()?.id.orEmpty()
    }

    private fun accountEffectiveBalance(account: Account): Double {
        val flow = entries()
            .filter { it.accountId == account.id }
            .sumOf { if (it.type == "income") it.amount else -it.amount }
        return account.balance + flow
    }

    private fun seedIfEmpty() {
        if (!prefs.contains("accounts")) {
            saveAccounts(defaultAccounts())
        }
        val primaryAccountId = accounts().firstOrNull()?.id.orEmpty()
        if (!prefs.contains("entries")) {
            prefs.edit().putString("entries", "[]").apply()
            listOf(
                Entry(1, "Salario", "Entrada", 5500.0, "income", LocalDate.now().minusDays(2).toString(), primaryAccountId),
                Entry(2, "Mercado", "Casa", 286.75, "expense", LocalDate.now().toString(), primaryAccountId),
                Entry(3, "Internet", "Assinaturas", 119.9, "expense", LocalDate.now().minusDays(1).toString(), primaryAccountId)
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

    private fun defaultAccounts(): List<Account> = listOf(
        Account("acc_main", "Principal", "\uD83C\uDFE6", "checking", 3200.0),
        Account("acc_save", "Reserva", "\uD83D\uDD12", "savings", 8400.0),
        Account("acc_card", "Cartão", "\uD83D\uDCB3", "credit", -860.0)
    )

   private fun accountPickerRow(items: List<Account>, selectedId: String?, onPick: (String) -> Unit): View {
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var current = selectedId
        fun rerender() {
            row.removeAllViews()
            items.forEach { account ->
                row.addView(TextView(this).apply {
                    setChipStyle(account.name, account.id == current, false) {
                        current = account.id
                        onPick(account.id)
                        rerender()
                    }
                })
            }
        }
        rerender()
        scroller.addView(row)
        return scroller
    }

    private fun choicePickerRow(items: List<Pair<String, String>>, selectedId: String?, onPick: (String) -> Unit): View {
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var current = selectedId
        fun rerender() {
            row.removeAllViews()
            items.forEach { (id, label) ->
                row.addView(TextView(this).apply {
                    setChipStyle(label, id == current, false) {
                        current = id
                        onPick(id)
                        rerender()
                    }
                })
            }
        }
        rerender()
        scroller.addView(row)
        return scroller
    }

    private fun accountTypePickerRow(types: List<Pair<String, String>>, selectedId: String, onPick: (String) -> Unit): View {
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var current = selectedId
        fun rerender() {
            row.removeAllViews()
            types.forEach { type ->
                row.addView(TextView(this).apply {
                    setChipStyle(type.second, current == type.first, false) {
                        current = type.first
                        onPick(type.first)
                        rerender()
                    }
                })
            }
        }
        rerender()
        scroller.addView(row)
        return scroller
    }

   private fun settingsCard(title: String, copy: String, onClick: () -> Unit, titleColor: Int = ink, icon: String = "\u2728", emphasize: Boolean = false): View = card(if (emphasize) heroFill() else paper, if (emphasize) Color.argb(36, 255, 255, 255) else line, 24) {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(84)
        addView(ImageView(this@MainActivity).apply {
            setImageResource(android.R.drawable.ic_menu_more)
            imageTintList = android.content.res.ColorStateList.valueOf(if (emphasize) Color.WHITE else ink)
            background = rounded(if (emphasize) Color.argb(28, 255, 255, 255) else soft, dp(16), if (emphasize) Color.TRANSPARENT else line)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }, LinearLayout.LayoutParams(dp(46), dp(46)).apply { setMargins(0, 0, dp(12), 0) })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(label(title, 16, if (emphasize) Color.WHITE else titleColor, true))
            addView(body(copy, 12, if (emphasize) Color.argb(178, 255, 255, 255) else muted).apply { setPadding(0, dp(5), 0, 0) })
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label("\u203A", 26, if (emphasize) Color.argb(180, 255, 255, 255) else muted, false))
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

   private fun categorySuggestionRow(categories: List<String>, onPick: (String) -> Unit): View {
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categories.forEach { category ->
            row.addView(TextView(this).apply {
                setChipStyle(
                    label = category,
                    selected = false,
                    compact = true
                ) { onPick(category) }
            })
        }
        scroller.addView(row)
        return scroller
    }

    private fun dateSuggestionRow(type: String, onPick: (String) -> Unit): View {
        val scroller = HorizontalScrollView(this).apply { overScrollMode = View.OVER_SCROLL_NEVER }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val options = if (type == "due") {
            listOf(
                "Hoje" to LocalDate.now(),
                "Amanha" to LocalDate.now().plusDays(1),
                "+7 dias" to LocalDate.now().plusDays(7),
                "+30 dias" to LocalDate.now().plusDays(30)
            )
        } else {
            listOf(
                "Hoje" to LocalDate.now(),
                "Ontem" to LocalDate.now().minusDays(1),
                "Amanha" to LocalDate.now().plusDays(1)
            )
        }
        options.forEach { option ->
            row.addView(TextView(this).apply {
                setChipStyle(
                    label = option.first,
                    selected = false,
                    compact = true
                ) { onPick(option.second.toString()) }
            })
        }
        scroller.addView(row)
        return scroller
    }

    private fun suggestedCategories(type: String, current: String?): List<String> {
        val base = FinanzaCategories.suggestions(type)
        val recent = movementFeed("all", "all")
            .filter { it.type == type && !it.category.isNullOrBlank() }
            .map { it.category.trim() }
            .distinct()
        return (listOfNotNull(current?.takeIf { it.isNotBlank() }) + recent + base)
            .distinct()
            .take(6)
    }

   private fun entryIcon(type: String): String = when (type) {
        "income" -> "\uD83D\uDCB0"
        "due" -> "\uD83D\uDCCC"
        else -> "\uD83D\uDCB8"
    }

    private fun entryDialogTitle(type: String, editing: Boolean): String = when {
        editing -> "Editar lancamento"
        type == "income" -> "Nova receita"
        type == "due" -> "Novo compromisso"
        else -> "Novo gasto"
    }

    private fun entryDialogSubtitle(type: String): String = when (type) {
        "income" -> "Registre a entrada e mantenha o saldo do app em dia."
        "due" -> "Programe vencimentos e deixe a agenda financeira organizada."
        else -> "Registre o gasto normal do app. A folha rapida continua no atalho e no widget."
    }

    private fun entryDialogButtonLabel(type: String, editing: Boolean): String = when {
        editing -> "Salvar alteracoes"
        type == "income" -> "Salvar receita"
        type == "due" -> "Salvar compromisso"
        else -> "Salvar gasto"
    }

   private fun card(fill: Int, stroke: Int, radius: Int, children: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(fill, dp(radius), stroke)
        elevation = if (isDarkTheme()) dp(8).toFloat() else dp(12).toFloat()
        children()
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, dp(12))
        }
    }

   private fun privacyMode(): Boolean = prefs.getBoolean("privacy_mode", false)
    private fun moneyText(value: Double): String = if (privacyMode()) "R$ --" else money.format(value)
    private fun label(text: String, size: Int, color: Int, bold: Boolean): TextView = TextView(this).apply {
        this.text = text; setTextColor(color); textSize = size.toFloat(); includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }
    private fun body(text: String, size: Int, color: Int): TextView = label(text, size, color, false).apply { setLineSpacing(dp(2).toFloat(), 1f) }

    private fun input(hint: String, value: String, numeric: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint
        setText(value)
        setHintTextColor(if (isDarkTheme()) Color.argb(125, 255, 255, 255) else Color.argb(118, 17, 24, 36))
        setTextColor(ink)
        textSize = 15f
        setSingleLine(true)
        setPadding(dp(16), 0, dp(16), 0)
        background = rounded(if (isDarkTheme()) paper2 else Color.argb(210, 255, 255, 255), dp(17), line)
        if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { setMargins(0, dp(12), 0, 0) }
    }

    private fun iconButton(text: String, description: String, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(ink)
        textSize = when (text) { "+" -> 25f; "\u22EF" -> 22f; "\u2715" -> 18f; else -> 16f }
        typeface = Typeface.DEFAULT_BOLD
        contentDescription = description
        minimumWidth = dp(48)
        minimumHeight = dp(48)
        background = rounded(if (isDarkTheme()) paper2 else Color.argb(214, 255, 255, 255), dp(18), line)
        setOnClickListener { onClick() }
    }

    private fun focusField(field: EditText) {
        field.requestFocus()
        if (field.text.isNotEmpty()) field.setSelection(field.text.length)
        field.post {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun TextView.setChipStyle(label: String, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
        text = label
        gravity = Gravity.CENTER
        setTextColor(if (selected) Color.rgb(8, 12, 16) else ink)
        textSize = if (compact) 12f else 13f
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(if (compact) 14 else 16), 0, dp(if (compact) 14 else 16), 0)
        background = rounded(
            if (selected) accent else if (isDarkTheme()) paper2 else Color.argb(210, 255, 255, 255),
            dp(if (compact) 16 else 18),
            if (selected) Color.TRANSPARENT else line
        )
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(if (compact) 40 else 46)
        ).apply {
            setMargins(0, 0, dp(8), 0)
        }
    }

   private fun infoPill(text: String): TextView = label(text, 11, ink, true).apply {
        gravity = Gravity.CENTER
        background = rounded(if (isDarkTheme()) paper2 else Color.argb(214, 255, 255, 255), dp(16), line)
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    private fun healthRow(icon: String, title: String, value: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(paper2, dp(18), line)
        addView(ImageView(this@MainActivity).apply {
            setImageResource(android.R.drawable.ic_menu_info_details)
            imageTintList = android.content.res.ColorStateList.valueOf(ink)
            contentDescription = title
            setPadding(dp(7), dp(7), dp(7), dp(7))
        }, LinearLayout.LayoutParams(dp(34), dp(34)).apply {
            setMargins(0, 0, dp(10), 0)
        })
        addView(label(title, 13, ink, true), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(label(value, 13, muted, true))
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, dp(8), 0, 0)
        }
    }

    private fun statusPill(text: String): TextView = label(text, 11, Color.WHITE, true).apply {
        background = rounded(Color.argb(32, 255, 255, 255), dp(99), Color.argb(26, 255, 255, 255))
        setPadding(dp(12), dp(7), dp(12), dp(7))
    }

   private fun primaryButton(text: String, onClick: () -> Unit): TextView = button(text, accent, if (isDarkTheme() || isFinanzaExperience) Color.BLACK else Color.WHITE, onClick)
    private fun secondaryButton(text: String, onClick: () -> Unit): TextView = button(text, if (isDarkTheme()) paper2 else Color.argb(210, 255, 255, 255), ink, onClick)
    private fun tertiaryButton(text: String, color: Int = muted, onClick: () -> Unit): TextView = button(text, Color.TRANSPARENT, color, onClick)
    private fun dangerButton(text: String, onClick: () -> Unit): TextView = tertiaryButton(text, danger, onClick)
    private fun button(text: String, fill: Int, color: Int, onClick: () -> Unit): TextView = TextView(this).apply {
        this.text = text
        gravity = Gravity.CENTER
        setTextColor(color)
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        background = rounded(fill, dp(20), if (fill == Color.TRANSPARENT) line else if (fill == accent) Color.argb(18, 255, 255, 255) else line)
        elevation = if (fill == Color.TRANSPARENT) 0f else dp(3).toFloat()
        setOnClickListener { onClick() }
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56)).apply { setMargins(0, dp(12), 0, 0) }
    }

   private fun heroFill(): Int = if (isDarkTheme()) Color.argb(224, 29, 37, 55) else Color.argb(232, 33, 41, 59)

    private fun pinQuickExpenseShortcut() {
        val shortcutManager = getSystemService(ShortcutManager::class.java)
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) {
            Toast.makeText(this, "Seu launcher nao liberou atalho fixo.", Toast.LENGTH_SHORT).show()
            return
        }
        val shortcut = ShortcutInfo.Builder(this, "quick_expense_pin")
            .setShortLabel(getString(R.string.shortcut_quick_expense_short))
            .setLongLabel(getString(R.string.shortcut_quick_expense_long))
            .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
            .setIntent(QuickExpenseShortcutActivity.createIntent(this).apply {
                action = Intent.ACTION_VIEW
            })
            .build()
        shortcutManager.requestPinShortcut(shortcut, null)
        Toast.makeText(this, "Pedido de atalho enviado para a tela inicial.", Toast.LENGTH_SHORT).show()
    }

    private fun rounded(color: Int, radius: Int, stroke: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = radius.toFloat(); if (stroke != Color.TRANSPARENT) setStroke(dp(1), stroke)
    }

    private fun greeting(): String = when (java.time.LocalTime.now().hour) { in 0..11 -> "Bom dia"; in 12..17 -> "Boa tarde"; else -> "Boa noite" }
    private fun monthLabel(): String = LocalDate.now().month.getDisplayName(java.time.format.TextStyle.SHORT, ptBr).replace(".", "").uppercase()
    private fun isCurrentMonth(date: String): Boolean = runCatching { LocalDate.parse(date).let { it.month == LocalDate.now().month && it.year == LocalDate.now().year } }.getOrDefault(false)
    private fun validDate(date: String): Boolean = try { LocalDate.parse(date); true } catch (_: DateTimeParseException) { false }
    private fun monthSnapshot(): MonthSnapshot {
        val items = entries().filter { isCurrentMonth(it.date) }
        val financialItems = items.filterNot { it.category.equals("Transferencia", ignoreCase = true) }
        return MonthSnapshot(
            entries = items,
            income = financialItems.filter { it.type == "income" }.sumOf { it.amount },
            spent = financialItems.filter { it.type == "expense" }.sumOf { it.amount }
        )
    }

    private fun movementFeed(filter: String, scope: String): List<Entry> {
        val all = (entries() + futureEntries())
            .sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.id })
        val scoped = if (scope == "month") all.filter { it.type == "due" || isCurrentMonth(it.date) } else all
        return when (filter) {
            "expense" -> scoped.filter { it.type == "expense" }
            "income" -> scoped.filter { it.type == "income" }
            "due" -> scoped.filter { it.type == "due" }
            else -> scoped
        }
    }
   private fun accountTypeLabel(type: String): String = when (type) {
        "savings" -> "Reserva"
        "investment" -> "Investimento"
        "credit" -> "Cartão"
        "cash" -> "Carteira"
        else -> "Conta corrente"
    }
    private fun accountTypeIcon(type: String): String = when (type) {
        "savings" -> "\uD83D\uDD12"
        "investment" -> "\uD83D\uDCC8"
        "credit" -> "\uD83D\uDCB3"
        "cash" -> "\uD83D\uDCB5"
        else -> "\uD83C\uDFE6"
    }
    private fun creditEffectiveDate(account: Account, rawDate: String): String {
        if (account.type != "credit" || account.cardClosingDay !in 1..31 || account.cardDueDay !in 1..31) return rawDate
        val purchase = runCatching { LocalDate.parse(rawDate) }.getOrDefault(LocalDate.now())
        var statementMonth = purchase.withDayOfMonth(1)
        if (purchase.dayOfMonth > account.cardClosingDay) statementMonth = statementMonth.plusMonths(1)
        var dueMonth = statementMonth
        if (account.cardDueDay <= account.cardClosingDay) dueMonth = dueMonth.plusMonths(1)
        return dueMonth.withDayOfMonth(account.cardDueDay.coerceAtMost(dueMonth.lengthOfMonth())).toString()
    }
    private fun composeUiState(): AppUiState = runCatching {
        buildComposeUiState()
    }.getOrElse { error ->
        prefs.edit().putString("last_startup_error", error.javaClass.simpleName).apply()
        emptyComposeUiState()
    }

    private fun buildComposeUiState(): AppUiState {
        val month = monthSnapshot()
        val budget = prefs.getFloat("monthly_budget", 5000f).toDouble().takeIf(Double::isFinite) ?: 5000.0
        val available = budget + month.income - month.spent
        val accountItems = accounts().distinctBy { it.id }
        val dueItems = futureEntries().distinctBy { it.id }.sortedBy { it.date }
        val dueTotal = dueItems.sumOf { it.amount }
        val paidDueTotal = month.entries.filter { it.type == "expense" && it.paid }.sumOf { it.amount }
        val transactionItems = entries().distinctBy { it.id }
            .sortedWith(compareByDescending<Entry> { it.date }.thenByDescending { it.id })
        val planningTransactions = transactionItems.mapNotNull { entry ->
            runCatching { LocalDate.parse(entry.date) }.getOrNull()?.let { date ->
                FinanzaPlanningTransaction(entry.type, entry.amount, entry.category, date, entry.paid)
            }
        }
        val categoryTotals = FinanzaPlanningRules.budgetSpending(planningTransactions, YearMonth.now(), LocalDate.now())
            .toList().sortedByDescending { it.second }
        val categoryShares = FinanzaPlanningRules.categoryShares(categoryTotals.map { it.second })
        val trendRows = FinanzaPlanningRules.monthTotals(planningTransactions, YearMonth.now())
        val maxTrend = trendRows.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0

        return AppUiState(
            selectedTab = when (activeTab) {
                "accounts" -> AppTab.CONTAS
                "analysis" -> AppTab.ANALISE
                "settings" -> AppTab.CONFIG
                else -> AppTab.HOME
            },
            userName = prefs.getString("user_name", "Voce") ?: "Voce",
            greeting = greeting(),
            period = monthLabel(),
            balance = moneyText(available),
            accountsTotal = moneyText(accountItems.sumOf(::accountEffectiveBalance)),
            income = moneyText(month.income),
            spent = moneyText(month.spent),
            paid = moneyText(paidDueTotal),
            remaining = moneyText(dueTotal),
            billProgress = if (paidDueTotal + dueTotal > 0) (paidDueTotal / (paidDueTotal + dueTotal)).toFloat() else 0f,
            transactions = transactionItems.map(::composeTransaction),
            accounts = accountItems.map { account ->
                AccountUi(account.id, displayText(account.name), accountTypeLabel(account.type), moneyText(accountEffectiveBalance(account)), account.type)
            },
            bills = dueItems.map { entry ->
                BillUi(
                    entry.id,
                    displayText(entry.title),
                    entry.category.ifBlank { "Geral" },
                    formatDate(entry.date),
                    moneyText(entry.amount),
                    if (runCatching { LocalDate.parse(entry.date).isBefore(LocalDate.now()) }.getOrDefault(false)) BillStatus.ATRASADO else BillStatus.PENDENTE
                )
            },
            categories = categoryTotals.take(8).mapIndexed { index, (name, amount) ->
                CategoryUi(name, moneyText(amount), categoryShares.getOrElse(index) { 0f }, categoryColor(name))
            },
            monthlyTrends = trendRows.map { totals ->
                MonthTrendUi(
                    totals.month.month.getDisplayName(java.time.format.TextStyle.SHORT, ptBr).replace(".", "").replaceFirstChar { it.uppercase() },
                    moneyText(totals.income), moneyText(totals.expense), (totals.income / maxTrend).toFloat(), (totals.expense / maxTrend).toFloat()
                )
            },
            config = ConfigUiState(
                userName = prefs.getString("user_name", "Voce") ?: "Voce",
                accountStatus = accountConnectionStatus(),
                budget = moneyText(budget),
                theme = "${visualExperience().label} / ${if (isDarkTheme()) "Escuro" else "Claro"}",
                accounts = accountItems.size,
                currency = "BRL - R$",
                notifications = prefs.getBoolean("quick_notification_enabled", false),
                privacy = privacyMode(),
                lastSync = prefs.getString("last_sync_date", "")?.ifBlank { "Nunca" } ?: "Nunca",
                pendingSync = FinanzaSyncQueue.pendingCount(prefs) + featureStore.pendingMutations().size,
                syncError = prefs.getString("last_sync_error", "").orEmpty(),
                role = prefs.getString("role", "") ?: "",
                twoFactor = prefs.getBoolean("two_factor_enabled", false)
            ),
            paymentMethods = accountItems.map { account ->
                PaymentMethodUi(
                    account.id,
                    displayText(account.name),
                    if (account.type == "credit") Icons.Rounded.CreditCard else Icons.Rounded.AccountBalanceWallet
                )
            }.ifEmpty { listOf(PaymentMethodUi("", "Principal")) },
            features = featureStore.buildUiState(
                spendingByCategory = categoryTotals.toMap(),
                sharedBalances = sharedBalances(),
                online = isOnlineMode(),
                canWrite = canWriteAccess()
            ).let { featureState ->
                val approvals = sharedApprovalModule()
                featureState.copy(
                    modules = featureState.modules.flatMap { module -> if (module.id == "shared") listOf(module, approvals) else listOf(module) },
                    pendingSync = featureState.pendingSync + prefArray("core_sync_queue").length()
                )
            }
        )
    }

    private fun emptyComposeUiState() = AppUiState(
        selectedTab = AppTab.HOME,
        userName = "Voce",
        greeting = greeting(),
        period = monthLabel(),
        balance = "R$ 0,00",
        accountsTotal = "R$ 0,00",
        income = "R$ 0,00",
        spent = "R$ 0,00",
        paid = "R$ 0,00",
        remaining = "R$ 0,00",
        billProgress = 0f,
        transactions = emptyList(),
        accounts = emptyList(),
        bills = emptyList(),
        categories = emptyList(),
        monthlyTrends = emptyList(),
        config = ConfigUiState(
            userName = "Voce",
            accountStatus = "Modo local",
            budget = "R$ 0,00",
            theme = "${AppExperience.NEXT.label} / Claro",
            accounts = 0,
            currency = "BRL - R$",
            notifications = false,
            privacy = false,
            lastSync = "Nunca",
            pendingSync = 0,
            syncError = "",
            role = "",
            twoFactor = false
        ),
        paymentMethods = listOf(PaymentMethodUi("", "Principal")),
        features = FeatureCenterUiState(emptyList(), 0, false)
    )

    private fun accountConnectionStatus(): String = when {
        prefs.getBoolean("session_expired", false) -> "Sessao expirada"
        isOnlineMode() -> "Conectada"
        else -> "Modo local"
    }

    private fun composeTransaction(entry: Entry): TransactionUi {
        val income = entry.type == "income"
        val formatted = if (privacyMode()) "R$ --" else "${if (income) "+" else "-"}${money.format(entry.amount)}"
        val category = FinanzaCategories.normalize(entry.category)
        return TransactionUi(entry.id, displayText(entry.title), category, formatted, formatDate(entry.date), income, categoryColor(category))
    }

    private fun displayText(value: String): String = when (value) {
        "Cartao" -> "Cartão"
        "Salario" -> "Salário"
        "Voce" -> "Você"
        else -> value
    }

    private fun sharedBalances(): Map<String, Double> {
        val transactions = entries().map { entry ->
            FinanzaSharedTransaction(entry.type, entry.amount, entry.splitMeta)
        }
        return FinanzaSharedRules.balances(featureStore.sharedSpacePayload(), transactions)
    }

    private fun composeActions(): AppActions = AppActions(
        selectTab = { tab ->
            activeTab = when (tab) {
                AppTab.CONTAS -> "accounts"
                AppTab.HOME -> "home"
                AppTab.ANALISE -> "analysis"
                AppTab.CONFIG -> "settings"
            }
        },
        refresh = { finished ->
            if (isOnlineMode()) {
                syncRemoteNow(silent = true, onComplete = finished, onError = { finished() })
            } else {
                render()
                finished()
            }
        },
        openTransaction = { id -> entries().firstOrNull { it.id == id }?.let { showEntryDialog(it.type, it) } },
        openAccount = { id -> accounts().firstOrNull { it.id == id }?.let(::showAccountDialog) },
        openBill = { id -> futureEntries().firstOrNull { it.id == id }?.let { showEntryDialog("due", it) } },
        newAccount = { showAccountDialog() },
        transferAccounts = ::showTransferDialog,
        saveExpense = ::saveComposeExpense,
        config = ConfigActions(
            editProfile = ::showProfileDialog,
            openAccount = ::showOnlineAccessDialog,
            editBudget = {
                showTextSetting("Orçamento mensal", prefs.getFloat("monthly_budget", 5000f).toString(), true) { raw ->
                    raw.replace(",", ".").toFloatOrNull()?.takeIf { it > 0 }?.let { prefs.edit().putFloat("monthly_budget", it).apply() }
                    render()
                }
            },
            changeTheme = ::showThemeDialog,
            toggleNotifications = ::setQuickNotificationEnabled,
            togglePrivacy = { enabled ->
                prefs.edit().putBoolean("privacy_mode", enabled).apply()
                render()
            },
            sync = { if (isOnlineMode()) syncRemoteNow(silent = false) else showOnlineAccessDialog() },
            backup = ::exportLocalBackup,
            diagnostics = ::showDataHealth,
            security = ::showSecurityDialog,
            admin = ::showAdminDialog,
            pinShortcut = ::pinQuickExpenseShortcut,
            clearData = ::confirmReset
        ),
        features = FeatureActions(
            save = save@{ moduleId, itemId, values ->
                if (!requireWriteAccess()) return@save false
                var mutation = featureStore.save(moduleId, itemId, values)
                if (mutation != null) {
                    if (moduleId == "car" && mutation.action == "create") mutation = createTransactionFromCar(mutation)
                    if (moduleId in setOf("subscriptions", "debts", "contracts")) mutation = upsertDueFromCommitment(mutation)
                    syncFeatureMutationAsync(mutation)
                    render()
                    true
                } else false
            },
            delete = delete@{ moduleId, itemId ->
                if (!requireWriteAccess()) return@delete
                featureStore.linkedDueId(moduleId, itemId)?.let(::deleteLinkedDue)
                if (moduleId == "car") {
                    featureStore.carTransactionId(itemId)?.let { transactionId ->
                        entries().firstOrNull { it.id == transactionId }?.let { entry ->
                            deleteEntry(entry)
                            syncEntryChangeAsync("delete", previous = entry)
                        }
                    }
                }
                featureStore.delete(moduleId, itemId)?.let(::syncFeatureMutationAsync)
                render()
            },
            primary = primary@{ moduleId, itemId ->
                if (!requireWriteAccess()) return@primary
                if (moduleId == "shared_approvals") reviewSharedApproval(itemId, "approved")
                else if (moduleId == "shared") showSharedSettlementDialog(itemId)
                else if (moduleId == "shared_space") shareSharedInvite()
                else {
                    val linkedDueId = featureStore.linkedDueId(moduleId, itemId)
                    featureStore.primary(moduleId, itemId)?.let { mutation ->
                        var updatedMutation = mutation
                        if (moduleId == "debts") {
                            linkedDueId?.let(::deleteLinkedDue)
                            createTransactionFromDebt(mutation)
                            updatedMutation = upsertDueFromCommitment(mutation)
                        }
                        syncFeatureMutationAsync(updatedMutation)
                    }
                    render()
                }
            },
            secondary = secondary@{ moduleId, itemId ->
                if (!requireWriteAccess()) return@secondary
                if (moduleId == "shared_approvals") reviewSharedApproval(itemId, "rejected")
            },
            importBackup = { backupImportLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
            importTransactions = { transactionImportLauncher.launch(arrayOf("text/csv", "application/vnd.ms-excel", "application/x-ofx", "text/plain")) },
            exportBackup = ::exportLocalBackup,
            sync = { if (isOnlineMode()) syncRemoteNow(silent = false) else showOnlineAccessDialog() }
        )
    )

    private fun sharedApprovalModule(): FeatureModuleUi {
        val canReview = canWriteAccess()
        val items = entries().mapNotNull { entry ->
            val meta = runCatching { JSONObject(entry.splitMeta) }.getOrNull() ?: return@mapNotNull null
            if (meta.optString("kind") != "equal" || meta.optJSONObject("approval")?.optString("status") != "pending") return@mapNotNull null
            FeatureItemUi(
                id = entry.id.toString(),
                title = entry.title,
                subtitle = "${formatDate(entry.date)} - ${entry.category}",
                value = if (privacyMode()) "R$ --" else money.format(entry.amount),
                status = "Aguardando revisao",
                progress = null,
                emoji = "",
                fields = emptyList(),
                primaryAction = if (canReview) "Aprovar" else "",
                secondaryAction = if (canReview) "Recusar" else "",
                canEdit = false,
                canDelete = false
            )
        }
        return FeatureModuleUi(
            id = "shared_approvals",
            title = "Aprovacoes",
            subtitle = "Gastos compartilhados pendentes",
            emoji = "",
            items = items,
            newFields = emptyList(),
            emptyText = "Nenhum gasto aguardando aprovacao.",
            canCreate = false
        )
    }

    private fun reviewSharedApproval(rawId: String, status: String) {
        val id = rawId.toLongOrNull() ?: return
        val previous = entries().firstOrNull { it.id == id } ?: return
        val meta = runCatching { JSONObject(previous.splitMeta) }.getOrNull() ?: return
        val approval = meta.optJSONObject("approval") ?: JSONObject()
        approval.put("status", status)
            .put("reviewedAt", System.currentTimeMillis())
            .put("reviewedBy", featureStore.sharedSpacePayload().optString("ownerPersonId"))
        meta.put("approval", approval)
        val updated = previous.copy(splitMeta = meta.toString())
        replaceEntry(previous, updated)
        syncEntryChangeAsync("update", entry = updated, previous = previous)
        render()
        Toast.makeText(this, if (status == "approved") "Gasto aprovado." else "Gasto recusado.", Toast.LENGTH_SHORT).show()
    }

    private fun showSharedSettlementDialog(personId: String) {
        showAppDialog("Registrar acerto", "O lançamento entra no histórico e sincroniza com o espaço compartilhado.") { dialog ->
            val balance = sharedBalances()[personId] ?: 0.0
            val amountInput = input("Valor", if (abs(balance) > 0.009) String.format(Locale.US, "%.2f", abs(balance)) else "", true)
            val ownerPaid = CheckBox(this@MainActivity).apply {
                text = "Eu paguei esta pessoa"
                setTextColor(ink)
                textSize = 13f
                isChecked = balance < 0.0
            }
            addView(amountInput)
            addView(ownerPaid)
            addView(primaryButton("Salvar acerto") {
                val amount = amountInput.text.toString().replace(",", ".").toDoubleOrNull()
                val meta = featureStore.settlementMeta(personId, ownerPaid.isChecked)
                if (amount == null || amount <= 0 || meta == null) {
                    Toast.makeText(this@MainActivity, "Confira o valor e o espaço compartilhado.", Toast.LENGTH_SHORT).show()
                    return@primaryButton
                }
                val entry = Entry(
                    id = System.currentTimeMillis(),
                    title = "Acerto do espaço compartilhado",
                    category = "Outros",
                    amount = amount,
                    type = if (ownerPaid.isChecked) "expense" else "income",
                    date = LocalDate.now().toString(),
                    accountId = defaultExpenseAccountId(),
                    note = "Acerto do espaço compartilhado",
                    splitMeta = meta.toString()
                )
                saveEntry(entry)
                syncEntryChangeAsync("create", entry = entry)
                dialog.dismiss()
                render()
            })
            amountInput.postDelayed({ focusField(amountInput) }, 120)
        }
    }

    private fun createTransactionFromCar(mutation: FeatureMutation): FeatureMutation {
        val payload = runCatching { JSONObject(mutation.payload) }.getOrNull() ?: return mutation
        val amount = payload.optDouble("amount", 0.0)
        if (amount <= 0.0) return mutation
        val accountId = payload.optString("accountId", defaultExpenseAccountId()).ifBlank { defaultExpenseAccountId() }
        val entry = Entry(
            id = System.currentTimeMillis(),
            title = payload.optString("title", if (payload.optString("type") == "fuel") "Abastecimento" else "Despesa do carro"),
            category = "Carro",
            amount = amount,
            type = "expense",
            date = payload.optString("date", LocalDate.now().toString()),
            accountId = accountId,
            note = payload.optString("note", "")
        )
        saveEntry(entry)
        syncEntryChangeAsync("create", entry = entry)
        return featureStore.attachCarTransaction(mutation.itemId, entry.id, accountId) ?: mutation
    }

    private fun createTransactionFromDebt(mutation: FeatureMutation) {
        val payload = runCatching { JSONObject(mutation.payload) }.getOrNull() ?: return
        val amount = payload.optDouble("installmentAmount", 0.0)
        if (amount <= 0.0) return
        val entry = Entry(
            id = System.currentTimeMillis(),
            title = payload.optString("name", "Parcela de divida"),
            category = "Outros",
            amount = amount,
            type = "expense",
            date = LocalDate.now().toString(),
            accountId = defaultExpenseAccountId(),
            note = "Parcela registrada pela central de dividas",
            paid = true
        )
        saveEntry(entry)
        syncEntryChangeAsync("create", entry = entry)
    }

    private fun upsertDueFromCommitment(mutation: FeatureMutation): FeatureMutation {
        val payload = runCatching { JSONObject(mutation.payload) }.getOrNull() ?: return mutation
        val linkedDueId = payload.optLong("linkedDueId", 0L).takeIf { it > 0L }
            ?: featureStore.linkedDueId(mutation.moduleId, mutation.itemId)
        val existingDue = linkedDueId?.let { id -> futureEntries().firstOrNull { it.id == id } }
        val status = payload.optString("status", "active")
        val active = when (mutation.moduleId) {
            "subscriptions" -> status == "active"
            "debts" -> status == "active" && payload.optDouble("outstandingAmount") > 0.0 && payload.optInt("remainingInstallments") > 0
            "contracts" -> status != "ended"
            else -> false
        }
        if (!active) {
            existingDue?.let { deleteLinkedDue(it.id) }
            return featureStore.detachLinkedDue(mutation.moduleId, mutation.itemId) ?: mutation
        }
        val amount = when (mutation.moduleId) {
            "subscriptions" -> payload.optDouble("amount", 0.0)
            "debts" -> payload.optDouble("installmentAmount", 0.0)
            else -> payload.optDouble("monthlyAmount", 0.0)
        }
        val date = when (mutation.moduleId) {
            "subscriptions" -> payload.optString("renewalDate")
            "debts" -> payload.optString("nextDueDate")
            else -> payload.optString("renewalDate")
        }.takeIf(::validDate) ?: return mutation
        if (amount <= 0.0) return mutation
        val due = Entry(
            id = existingDue?.id ?: System.currentTimeMillis(),
            title = payload.optString("name", "Compromisso"),
            category = if (mutation.moduleId == "subscriptions") payload.optString("category", "Assinaturas") else "Outros",
            amount = amount,
            type = "due",
            date = date,
            note = payload.optString("notes", ""),
            sourceModule = mutation.moduleId,
            sourceItemId = mutation.itemId
        )
        if (existingDue == null) {
            saveEntry(due)
            syncEntryChangeAsync("create", entry = due)
        } else {
            replaceEntry(existingDue, due)
            syncEntryChangeAsync("update", entry = due, previous = existingDue)
        }
        return if (linkedDueId == due.id) mutation else featureStore.attachLinkedDue(mutation.moduleId, mutation.itemId, due.id) ?: mutation
    }

    private fun deleteLinkedDue(id: Long) {
        futureEntries().firstOrNull { it.id == id }?.let { due ->
            deleteEntry(due)
            syncEntryChangeAsync("delete", previous = due)
        }
    }

    private fun shareSharedInvite() {
        val payload = featureStore.sharedInviteData()
        if (payload == null) {
            Toast.makeText(this, "Adicione pelo menos uma pessoa antes de convidar.", Toast.LENGTH_SHORT).show()
            return
        }
        val encoded = android.util.Base64.encodeToString(payload.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val link = "https://jeffersonf.github.io/finanza-next/?invite=${java.net.URLEncoder.encode(encoded, Charsets.UTF_8.name())}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Convite Finanza")
            putExtra(Intent.EXTRA_TEXT, link)
        }, "Compartilhar convite"))
    }

    private fun saveComposeExpense(rawAmount: String, rawDescription: String, category: String, method: PaymentMethodUi): Boolean {
        val raw = rawAmount.trim().replace("R$", "", ignoreCase = true).replace(" ", "")
        val normalized = if (raw.contains(',')) raw.replace(".", "").replace(',', '.') else raw
        val amount = normalized.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            Toast.makeText(this, "Digite um valor valido.", Toast.LENGTH_SHORT).show()
            return false
        }
        val description = rawDescription.trim().ifBlank { "Gasto" }
        val selectedCategory = category.ifBlank { FinanzaCategories.infer(description) }
        val entry = Entry(System.currentTimeMillis(), description, selectedCategory, amount, "expense", LocalDate.now().toString(), method.id)
        saveEntry(entry)
        syncEntryChangeAsync("create", entry = entry)
        Toast.makeText(this, "Gasto salvo.", Toast.LENGTH_SHORT).show()
        render()
        return true
    }

    private fun setQuickNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("quick_notification_enabled", enabled).apply()
        if (enabled) {
            ensureQuickExpenseNotification(requestPermission = true)
        } else {
            getSystemService(NotificationManager::class.java).cancel(4101)
        }
        render()
    }


    private fun formatDate(date: String): String = runCatching { LocalDate.parse(date).format(shortDate).replaceFirstChar { it.uppercase() } }.getOrDefault(date)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
