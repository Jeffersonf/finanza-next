package com.finanza.next.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.finanza.next.ui.components.AccountUi
import com.finanza.next.ui.components.AppTab
import com.finanza.next.ui.components.BillUi
import com.finanza.next.ui.components.BottomNavBar
import com.finanza.next.ui.components.TransactionUi
import com.finanza.next.ui.screens.AddTransactionSheet
import com.finanza.next.ui.screens.AnalysisScreen
import com.finanza.next.ui.screens.CategoryUi
import com.finanza.next.ui.screens.ConfigActions
import com.finanza.next.ui.screens.ConfigScreen
import com.finanza.next.ui.screens.ConfigUiState
import com.finanza.next.ui.screens.ContasScreen
import com.finanza.next.ui.screens.HomeScreen
import com.finanza.next.ui.screens.PaymentMethodUi
import com.finanza.next.ui.screens.MonthTrendUi
import com.finanza.next.ui.screens.FeatureCenterScreen
import com.finanza.next.features.FeatureActions
import com.finanza.next.features.FeatureCenterUiState
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience

data class AppUiState(
    val selectedTab: AppTab,
    val userName: String,
    val greeting: String,
    val period: String,
    val balance: String,
    val accountsTotal: String,
    val income: String,
    val spent: String,
    val paid: String,
    val remaining: String,
    val billProgress: Float,
    val transactions: List<TransactionUi>,
    val accounts: List<AccountUi>,
    val bills: List<BillUi>,
    val categories: List<CategoryUi>,
    val monthlyTrends: List<MonthTrendUi>,
    val config: ConfigUiState,
    val paymentMethods: List<PaymentMethodUi>,
    val features: FeatureCenterUiState
)

data class AppActions(
    val selectTab: (AppTab) -> Unit,
    val refresh: (() -> Unit) -> Unit,
    val openTransaction: (Long) -> Unit,
    val openAccount: (String) -> Unit,
    val openBill: (Long) -> Unit,
    val newAccount: () -> Unit,
    val transferAccounts: () -> Unit,
    val saveExpense: (String, String, String, PaymentMethodUi) -> Boolean,
    val config: ConfigActions,
    val features: FeatureActions
)

@Composable
fun AppScaffold(state: AppUiState, actions: AppActions, initialShowFeatures: Boolean = false, initialFeatureModule: String? = null) {
    var showAddSheet by remember { mutableStateOf(false) }
    var showFeatures by remember { mutableStateOf(initialShowFeatures) }
    var navVisible by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(state.selectedTab) }
    val tabs = AppTab.entries
    val scope = rememberCoroutineScope()
    val tokens = LocalAppExperienceTokens.current
    val finanzaDark = LocalAppExperience.current == AppExperience.FINANZA && MaterialTheme.colorScheme.background.luminance() < 0.5f
    val finanzaBackground = MaterialTheme.colorScheme.background
    val finanzaPrimary = MaterialTheme.colorScheme.primary
    val finanzaTeal = MaterialTheme.colorScheme.tertiary
    val pagerState = rememberPagerState(
        initialPage = tabs.indexOf(selectedTab).coerceAtLeast(0),
        pageCount = { tabs.size }
    )
    val scrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulated = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y == 0f) return Offset.Zero
                if ((accumulated > 0f && available.y < 0f) || (accumulated < 0f && available.y > 0f)) {
                    accumulated = 0f
                }
                accumulated += available.y
                if (accumulated <= -28f) {
                    navVisible = false
                    accumulated = 0f
                } else if (accumulated >= 20f) {
                    navVisible = true
                    accumulated = 0f
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(state.selectedTab) {
        if (state.selectedTab != selectedTab) {
            navVisible = true
            selectedTab = state.selectedTab
            val target = tabs.indexOf(state.selectedTab)
            if (target >= 0 && target != pagerState.currentPage) pagerState.scrollToPage(target)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        val tab = tabs.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        if (tab != selectedTab) selectedTab = tab
        actions.selectTab(tab)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (finanzaDark) Color.Transparent else MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            Modifier.fillMaxSize()
                .then(
                    if (finanzaDark) Modifier.drawBehind {
                        drawRect(finanzaBackground)
                        drawCircle(
                            color = finanzaPrimary.copy(alpha = 0.08f),
                            radius = size.width * 0.78f,
                            center = Offset(size.width * 0.50f, -size.height * 0.10f)
                        )
                        drawCircle(
                            color = finanzaTeal.copy(alpha = 0.055f),
                            radius = size.width * 0.56f,
                            center = Offset(size.width * 1.05f, size.height * 0.60f)
                        )
                        drawCircle(
                            color = Color(0xFFA78BFA).copy(alpha = 0.045f),
                            radius = size.width * 0.46f,
                            center = Offset(-size.width * 0.05f, size.height * 0.88f)
                        )
                    } else Modifier
                )
                .statusBarsPadding()
                .nestedScroll(scrollConnection)
        ) {
        if (showFeatures) {
            FeatureCenterScreen(state.features, actions.features, initialFeatureModule) { showFeatures = false }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = tokens.navHeight + 20.dp)
                    .testTag("mainPager"),
                key = { tabs[it] }
            ) { page ->
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        if (!refreshing) {
                            refreshing = true
                            actions.refresh { refreshing = false }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    when (tabs[page]) {
                        AppTab.HOME -> HomeScreen(
                    state.userName, state.greeting, state.period, state.balance, state.income, state.spent,
                    state.transactions, state.accounts, state.bills, state.categories, state.features,
                    { showAddSheet = true }, { actions.selectTab(AppTab.ANALISE) },
                    actions.openTransaction, actions.openBill, { showFeatures = true },
                    { actions.selectTab(AppTab.CONTAS) }, { actions.selectTab(AppTab.ANALISE) },
                    { actions.selectTab(AppTab.CONFIG) }
                )
                        AppTab.CONTAS -> ContasScreen(
                    state.period, state.accountsTotal, state.billProgress, state.paid, state.remaining,
                    state.accounts, state.bills, actions.newAccount, actions.transferAccounts, actions.openAccount, actions.openBill
                )
                        AppTab.ANALISE -> AnalysisScreen(state.income, state.spent, state.categories, state.monthlyTrends, state.transactions, actions.openTransaction)
                        AppTab.CONFIG -> ConfigScreen(state.config, actions.config)
                    }
                }
            }
        }

            AnimatedVisibility(
                visible = navVisible && !showFeatures,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = tokens.navHorizontalPadding, vertical = 10.dp),
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                BottomNavBar(
                    selected = selectedTab,
                    onSelect = { tab ->
                        navVisible = true
                        if (tab != selectedTab) {
                            selectedTab = tab
                            scope.launch { pagerState.scrollToPage(tabs.indexOf(tab)) }
                        }
                        actions.selectTab(tab)
                    }
                )
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            methods = state.paymentMethods,
            onComplete = { amount, description, category, method ->
                if (actions.saveExpense(amount, description, category, method)) showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}
