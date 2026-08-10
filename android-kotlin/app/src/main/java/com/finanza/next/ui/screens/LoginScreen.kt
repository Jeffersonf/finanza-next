package com.finanza.next.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens

@Composable
fun LoginScreen(
    initialUrl: String,
    initialUsername: String,
    busy: Boolean,
    error: String?,
    onLogin: (url: String, username: String, password: String, otp: String) -> Unit,
    onContinueOffline: (displayName: String) -> Unit
) {
    var apiUrl by remember { mutableStateOf(initialUrl) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    if (LocalAppExperience.current == AppExperience.WEB) {
        FinanzaWebLogin(
            apiUrl = apiUrl,
            username = username,
            password = password,
            otp = otp,
            busy = busy,
            error = error,
            onApiUrlChange = { apiUrl = it },
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onOtpChange = { otp = it.filter(Char::isDigit).take(6) },
            onLogin = { onLogin(apiUrl.trim(), username.trim(), password, otp.trim()) },
            onContinueOffline = { onContinueOffline(username.ifBlank { "Voc\u00ea" }) }
        )
        return
    }
    val finanza = LocalAppExperience.current.usesFinanzaVisuals
    val cardColor = if (finanza) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val pageColor = MaterialTheme.colorScheme.background

    Box(Modifier.fillMaxSize().background(pageColor).imePadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(if (finanza) LocalAppExperienceTokens.current.cardRadius else 28.dp),
                color = cardColor,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Surface(
                        modifier = Modifier.size(50.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null)
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Text("Finext", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "Entre com a mesma conta usada no Finanza web.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = { apiUrl = it },
                        label = { Text("URL da API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Usuário") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                        label = { Text("Código de autenticação (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    )
                    if (!error.isNullOrBlank()) {
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    Button(
                        onClick = { onLogin(apiUrl.trim(), username.trim(), password, otp.trim()) },
                        enabled = !busy && apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null)
                        Text(if (busy) "Conectando..." else "Entrar e sincronizar", modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedButton(
                        onClick = { onContinueOffline(username.ifBlank { "Você" }) },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Text("Usar somente neste aparelho", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        "O modo local mantém seus dados neste aparelho. Você pode conectar a conta depois em Ajustes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanzaWebLogin(
    apiUrl: String,
    username: String,
    password: String,
    otp: String,
    busy: Boolean,
    error: String?,
    onApiUrlChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onOtpChange: (String) -> Unit,
    onLogin: () -> Unit,
    onContinueOffline: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "FINANZA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Seu dinheiro,\norganizado.",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Entre para sincronizar seus dados do Finanza web neste aparelho.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 9.dp, bottom = 24.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.66f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Acessar conta", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Use as mesmas credenciais do web.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp, bottom = 14.dp)
                    )
                    OutlinedTextField(
                        value = apiUrl,
                        onValueChange = onApiUrlChange,
                        label = { Text("URL da API") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        label = { Text("Usu\u00e1rio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                    )
                    OutlinedTextField(
                        value = otp,
                        onValueChange = onOtpChange,
                        label = { Text("C\u00f3digo de autentica\u00e7\u00e3o (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 9.dp)
                    )
                    if (!error.isNullOrBlank()) {
                        Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 11.dp))
                    }
                    Button(
                        onClick = onLogin,
                        enabled = !busy && apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Icon(Icons.Rounded.CloudSync, contentDescription = null)
                        Text(if (busy) "Conectando..." else "Entrar e sincronizar", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            OutlinedButton(
                onClick = onContinueOffline,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null)
                Text("Usar somente neste aparelho", modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "O modo local mant\u00e9m seus dados neste aparelho. A conex\u00e3o pode ser feita depois em Ajustes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
