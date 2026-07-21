package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.SettingsGroup
import com.finanza.next.ui.components.SettingsRow
import com.finanza.next.ui.components.SettingsSectionTitle
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

data class ConfigUiState(
    val userName: String,
    val accountStatus: String,
    val budget: String,
    val theme: String,
    val accounts: Int,
    val currency: String,
    val notifications: Boolean,
    val privacy: Boolean,
    val lastSync: String,
    val pendingSync: Int,
    val syncError: String,
    val role: String,
    val twoFactor: Boolean
)

data class ConfigActions(
    val editProfile: () -> Unit,
    val openAccount: () -> Unit,
    val editBudget: () -> Unit,
    val changeTheme: () -> Unit,
    val toggleNotifications: (Boolean) -> Unit,
    val togglePrivacy: (Boolean) -> Unit,
    val sync: () -> Unit,
    val backup: () -> Unit,
    val diagnostics: () -> Unit,
    val security: () -> Unit,
    val admin: () -> Unit,
    val pinShortcut: () -> Unit,
    val clearData: () -> Unit
)

@Composable
fun ConfigScreen(state: ConfigUiState, actions: ConfigActions) {
    val tokens = LocalAppExperienceTokens.current
    val experience = LocalAppExperience.current
    val finanza = experience.id == "finanza"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 108.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 12.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(tokens.cardRadius + 2.dp)).background(if (finanza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                .border(1.dp, if (finanza) MaterialTheme.colorScheme.outlineVariant else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(tokens.cardRadius + 2.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                Text(state.userName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.padding(start = 12.dp)) {
                Text(state.userName, style = MaterialTheme.typography.bodyLarge)
                Text(state.accountStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f))
            }
        }

        SettingsSectionTitle("Conta")
        SettingsGroup {
            SettingsRow("Seu perfil", state.userName, actions.editProfile)
            SettingsRow("Conta Finanza", state.accountStatus, actions.openAccount, showDivider = false)
        }
        SettingsSectionTitle("Planejamento")
        SettingsGroup {
            SettingsRow("Orçamento mensal", state.budget, actions.editBudget)
            SettingsRow("Contas", "${state.accounts} ativas", showDivider = false)
        }
        SettingsSectionTitle("Aparencia")
        SettingsGroup {
            SettingsRow("Tema", state.theme, actions.changeTheme)
            SettingsRow("Ocultar valores", switch = state.privacy, onSwitchChange = actions.togglePrivacy, showDivider = false)
        }
        SettingsSectionTitle("Captura rapida")
        SettingsGroup {
            SettingsRow("Notificação", switch = state.notifications, onSwitchChange = actions.toggleNotifications)
            SettingsRow("Fixar atalho", onClick = actions.pinShortcut, showDivider = false)
        }
        SettingsSectionTitle("Dados")
        SettingsGroup {
            SettingsRow("Sincronizar agora", state.lastSync, actions.sync)
            if (state.pendingSync > 0) SettingsRow("Pendencias locais", state.pendingSync.toString(), actions.sync)
            if (state.syncError.isNotBlank()) SettingsRow("Ultimo erro", state.syncError)
            SettingsRow("Compartilhar backup", onClick = actions.backup)
            SettingsRow("Diagnostico", onClick = actions.diagnostics)
            SettingsRow("Apagar dados locais", onClick = actions.clearData, showDivider = false)
        }
        SettingsSectionTitle("Seguranca")
        SettingsGroup {
            SettingsRow("Autenticação em duas etapas", if (state.twoFactor) "Ativa" else "Desativada", actions.security)
            if (state.role == "admin") {
                SettingsRow("Administracao", state.role, actions.admin, showDivider = false)
            } else {
                SettingsRow("Perfil de acesso", state.role.ifBlank { "local" }, showDivider = false)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("${experience.label} Android 1.2.2", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
