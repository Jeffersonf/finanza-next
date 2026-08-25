@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finanza.next.FinanzaInvoiceReview
import com.finanza.next.InvoiceMatch
import com.finanza.next.InvoiceMatchKind
import com.finanza.next.InvoiceReviewLine
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun InvoiceReviewSheet(
    lines: List<InvoiceReviewLine>,
    onDismiss: () -> Unit,
    onRemovePlannedInstallment: (InvoiceMatch) -> Unit,
    onConfirm: (List<InvoiceReviewLine>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = remember(lines) {
        mutableStateMapOf<Long, Boolean>().apply {
            lines.forEach { line -> put(line.transaction.id, line.matches.isEmpty()) }
        }
    }
    var editableLines by remember(lines) { mutableStateOf(lines) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var removalCandidate by remember { mutableStateOf<InvoiceMatch?>(null) }
    val normalizedQuery = FinanzaInvoiceReview.merchantKey(query)
    val visible = editableLines.filter { line ->
        val matchesFilter = when (filter) {
            InvoiceFilter.ALL -> true
            InvoiceFilter.NEW -> line.matches.isEmpty()
            InvoiceFilter.REVIEW -> line.matches.isNotEmpty()
        }
        matchesFilter && (normalizedQuery.isBlank() || FinanzaInvoiceReview.merchantKey(line.transaction.description).contains(normalizedQuery))
    }
    val selectedLines = editableLines.filter { selected[it.transaction.id] == true && it.matches.none { match -> match.kind == InvoiceMatchKind.EXACT } }
    val exact = editableLines.count { line -> line.matches.any { it.kind == InvoiceMatchKind.EXACT } }
    val installments = editableLines.count { line -> line.matches.any { it.kind == InvoiceMatchKind.LIKELY_INSTALLMENT } }
    val maxHeight = with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.height.toDp() * 0.78f
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight).imePadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Revisar fatura", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Nada será salvo sem sua confirmação.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, contentDescription = "Fechar") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReviewCount("Lidos", lines.size.toString(), Modifier.weight(1f))
                    ReviewCount("Repetidos", exact.toString(), Modifier.weight(1f))
                    ReviewCount("Parcelas", installments.toString(), Modifier.weight(1f))
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    label = { Text("Buscar compra ou parcela") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InvoiceFilter.entries.forEach { choice ->
                        FilterChip(
                            selected = filter == choice,
                            onClick = { filter = choice },
                            label = { Text(choice.label) }
                        )
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { editableLines.forEach { line -> if (line.matches.none { it.kind == InvoiceMatchKind.EXACT }) selected[line.transaction.id] = true } }) { Text("Marcar todos") }
                    TextButton(onClick = { editableLines.forEach { line -> selected[line.transaction.id] = false } }) { Text("Desmarcar todos") }
                }
            }
            if (visible.isEmpty()) {
                item {
                    Text(
                        "Nenhum lançamento encontrado com este filtro.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 22.dp)
                    )
                }
            }
            items(visible, key = { it.transaction.id }) { line ->
                InvoiceReviewRow(
                    line = line,
                    checked = selected[line.transaction.id] == true,
                    onCheckedChange = { checked -> selected[line.transaction.id] = checked },
                    onEdit = { updated -> editableLines = editableLines.map { current -> if (current.transaction.id == updated.transaction.id) updated else current } },
                    onRemovePlannedInstallment = { removalCandidate = it }
                )
            }
            item {
                Button(
                    onClick = { onConfirm(selectedLines) },
                    enabled = selectedLines.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                ) {
                    Text("Adicionar ${selectedLines.size} lançamento${if (selectedLines.size == 1) "" else "s"}")
                }
            }
        }
    }
    removalCandidate?.let { match ->
        AlertDialog(
            onDismissRequest = { removalCandidate = null },
            title = { Text("Remover parcela planejada?") },
            text = {
                Text("${match.title} (${match.installmentLabel.ifBlank { "parcela futura" }}) será removida do planejamento. Esta ação não adiciona a linha da fatura automaticamente.")
            },
            dismissButton = {
                Button(onClick = { removalCandidate = null }) { Text("Manter") }
            },
            confirmButton = {
                Button(onClick = {
                    onRemovePlannedInstallment(match)
                    removalCandidate = null
                }) { Text("Remover") }
            }
        )
    }
}

private enum class InvoiceFilter(val label: String) { ALL("Todos"), NEW("Novos"), REVIEW("Revisar") }

@Composable
private fun ReviewCount(label: String, value: String, modifier: Modifier) {
    val tokens = LocalAppExperienceTokens.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(tokens.cardRadius.coerceAtMost(14.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InvoiceReviewRow(
    line: InvoiceReviewLine,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: (InvoiceReviewLine) -> Unit,
    onRemovePlannedInstallment: (InvoiceMatch) -> Unit
) {
    val tokens = LocalAppExperienceTokens.current
    val exact = line.matches.firstOrNull { it.kind == InvoiceMatchKind.EXACT }
    val refundAdjustment = line.matches.firstOrNull { it.kind == InvoiceMatchKind.REFUND_ADJUSTMENT }
    val installment = line.matches.firstOrNull { it.kind == InvoiceMatchKind.LIKELY_INSTALLMENT }
    val blocked = exact != null
    val money = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.cardRadius)).clickable(enabled = !blocked) { onCheckedChange(!checked) },
        shape = RoundedCornerShape(tokens.cardRadius),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = if (blocked) null else onCheckedChange, enabled = !blocked)
                Icon(
                    Icons.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = if (blocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(line.transaction.description, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    val detail = buildString {
                        append(line.transaction.date)
                        if (line.transaction.category.isNotBlank()) append(" · ${line.transaction.category}")
                        if (line.installmentLabel.isNotBlank()) append(" · Parcela ${line.installmentLabel}")
                    }
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Text(money.format(line.transaction.amount), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            when {
                exact != null -> InvoiceMatchNotice("Já existe em ${exact.date}: ${exact.title}. Este item foi bloqueado.", true)
                refundAdjustment != null -> InvoiceMatchNotice(
                    "Estorno identificado: ${refundAdjustment.title} será ajustado para ${money.format(line.transaction.amount)}. Marque para confirmar a atualização.",
                    false
                )
                installment != null -> InvoiceMatchNotice(
                    "Possível série já planejada: ${installment.title}${installment.installmentLabel.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}. A fatura indica ${line.installmentLabel}; confirme antes de remover ou importar.",
                    false
                )
            }
            Row(Modifier.fillMaxWidth().padding(start = 44.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = line.transaction.type == "income",
                    onClick = { onEdit(line.copy(transaction = line.transaction.copy(type = if (line.transaction.type == "income") "expense" else "income"))) },
                    label = { Text(if (line.transaction.type == "income") "Entrada" else "Despesa") }
                )
                OutlinedTextField(
                    value = line.transaction.category,
                    onValueChange = { onEdit(line.copy(transaction = line.transaction.copy(category = it))) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Categoria") }
                )
            }
            OutlinedTextField(
                value = line.transaction.description,
                onValueChange = { onEdit(line.copy(transaction = line.transaction.copy(description = it))) },
                modifier = Modifier.fillMaxWidth().padding(start = 44.dp, top = 8.dp),
                singleLine = true,
                label = { Text("Descrição") }
            )
            Row(Modifier.fillMaxWidth().padding(start = 44.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = line.transaction.date,
                    onValueChange = { onEdit(line.copy(transaction = line.transaction.copy(date = it))) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Data") }
                )
                OutlinedTextField(
                    value = line.transaction.amount.toString(),
                    onValueChange = { raw -> raw.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0.0 }?.let { onEdit(line.copy(transaction = line.transaction.copy(amount = it))) } },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            if (installment != null) {
                Button(
                    onClick = { onRemovePlannedInstallment(installment) },
                    modifier = Modifier.padding(start = 44.dp, top = 7.dp)
                ) {
                    Text("Remover parcela planejada")
                }
            }
        }
    }
}

@Composable
private fun InvoiceMatchNotice(text: String, exact: Boolean) {
    val color = if (exact) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(start = 44.dp, top = 7.dp)
    )
}
