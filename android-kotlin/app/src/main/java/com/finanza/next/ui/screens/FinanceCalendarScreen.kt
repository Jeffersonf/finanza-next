package com.finanza.next.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.finanza.next.ui.components.categoryIcon
import com.finanza.next.ui.theme.AppExperience
import com.finanza.next.ui.theme.LocalAppExperience
import com.finanza.next.ui.theme.LocalAppExperienceTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class FinanceCalendarSource { TRANSACTION, DUE }

data class FinanceCalendarEventUi(
    val id: Long,
    val title: String,
    val amount: String,
    val date: LocalDate,
    val category: String,
    val color: Color,
    val income: Boolean,
    val source: FinanceCalendarSource
)

@Composable
fun FinanceCalendarScreen(
    events: List<FinanceCalendarEventUi>,
    onEvent: (FinanceCalendarEventUi) -> Unit,
    onQuickAdd: () -> Unit,
    onClose: () -> Unit
) {
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    val today = LocalDate.now()
    val web = LocalAppExperience.current == AppExperience.WEB
    val modern = LocalAppExperience.current == AppExperience.NEXT
    val tokens = LocalAppExperienceTokens.current
    val visibleMonth = remember(anchor) { YearMonth.from(anchor) }
    val visibleDays = remember(visibleMonth) {
        val firstDay = visibleMonth.atDay(1)
        val lastDay = visibleMonth.atEndOfMonth()
        val gridStart = firstDay.minusDays((firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        val gridEnd = lastDay.plusDays((DayOfWeek.SUNDAY.value - lastDay.dayOfWeek.value).toLong())
        generateSequence(gridStart) { day -> day.plusDays(1) }
            .takeWhile { day -> !day.isAfter(gridEnd) }
            .toList()
    }
    val visibleEvents = events.filter { it.date.year == visibleMonth.year && it.date.month == visibleMonth.month }
    val expenseTotal = visibleEvents.filter { !it.income && it.source == FinanceCalendarSource.TRANSACTION }
        .sumOf { calendarAmount(it.amount) }
    val incomeTotal = visibleEvents.filter { it.income }.sumOf { calendarAmount(it.amount) }
    val pendingDue = visibleEvents.count { it.source == FinanceCalendarSource.DUE }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
        item {
            if (modern) {
                ModernCalendarHeader(
                    anchor = anchor,
                    onClose = onClose,
                    onPrevious = { anchor = anchor.minusMonths(1) },
                    onNext = { anchor = anchor.plusMonths(1) }
                )
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Agenda", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Movimentos e compromissos no mesmo calendário",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                        )
                    }
                    CalendarMonthSelector(anchor, { anchor = anchor.minusMonths(1) }, { anchor = anchor.plusMonths(1) }, web)
                }
            }
        }
        item {
            CalendarPulse(incomeTotal, expenseTotal, pendingDue, web, modern)
        }
        item {
            FinanceMonthGrid(visibleDays, visibleMonth, events, today, onEvent)
        }
        if (visibleEvents.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(tokens.cardRadius),
                    color = if (web) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Nenhum lançamento ou vencimento neste mês.", modifier = Modifier.padding(start = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(86.dp),
                contentAlignment = Alignment.Center
            ) {
                CalendarToolbar(
                    onPrevious = { anchor = anchor.minusMonths(1) },
                    onNext = { anchor = anchor.plusMonths(1) },
                    onAdd = onQuickAdd
                )
            }
        }
    }
}

@Composable
private fun ModernCalendarHeader(
    anchor: LocalDate,
    onClose: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
            }
            Column(Modifier.padding(start = 5.dp)) {
                Text("Agenda", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Gastos e compromissos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        CalendarMonthSelector(anchor, onPrevious, onNext, web = false, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun CalendarMonthSelector(
    anchor: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    web: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(if (web) 12.dp else 18.dp),
        color = if (web) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mês anterior")
            }
            Text(
                anchor.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() } + " ${anchor.year}",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            IconButton(onClick = onNext, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Próximo mês")
            }
        }
    }
}

@Composable
private fun CalendarPulse(income: Double, expense: Double, pendingDue: Int, web: Boolean, modern: Boolean) {
    val background = when {
        web -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        modern -> Color(0xFF0B4327)
        else -> Color(0xFF155E35)
    }
    val ink = if (web) MaterialTheme.colorScheme.onSurface else Color(0xFFF0FFF4)
    val muted = ink.copy(alpha = 0.74f)
    Surface(shape = RoundedCornerShape(if (web) 16.dp else 22.dp), color = background) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(ink.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.EventNote, contentDescription = null, tint = ink, modifier = Modifier.size(21.dp)) }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(
                    if (pendingDue == 0) "Tudo em dia" else "$pendingDue vencimento${if (pendingDue == 1) "" else "s"} nesta janela",
                    style = MaterialTheme.typography.titleSmall,
                    color = ink
                )
                Text(
                    "${calendarMoney(expense)} em gastos e ${calendarMoney(income)} em entradas neste mês.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarPulseValue(label: String, value: String, color: Color, ink: Color, muted: Color, modifier: Modifier) {
    Column(modifier) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = muted, modifier = Modifier.padding(top = 8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = ink, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun FinanceMonthGrid(
    dates: List<LocalDate>,
    visibleMonth: YearMonth,
    allEvents: List<FinanceCalendarEventUi>,
    today: LocalDate,
    onEvent: (FinanceCalendarEventUi) -> Unit
) {
    val grouped = allEvents.groupBy { it.date }
    val modern = LocalAppExperience.current == AppExperience.NEXT
    val line = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (modern) 0.16f else 0.68f)

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom").forEach { day ->
                Text(day, modifier = Modifier.weight(1f).padding(bottom = 7.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDay(
                        date = date,
                        belongsToMonth = YearMonth.from(date) == visibleMonth,
                        isToday = date == today,
                        events = grouped[date].orEmpty(),
                        line = line,
                        onEvent = onEvent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate,
    belongsToMonth: Boolean,
    isToday: Boolean,
    events: List<FinanceCalendarEventUi>,
    line: Color,
    onEvent: (FinanceCalendarEventUi) -> Unit,
    modifier: Modifier
) {
    val modern = LocalAppExperience.current == AppExperience.NEXT
    val dailyNet = events.sumOf { event -> if (event.income) calendarAmount(event.amount) else -calendarAmount(event.amount) }
    Column(
        modifier
            .height(if (modern) 126.dp else 168.dp)
            .padding(end = 1.dp, bottom = 1.dp)
            .background(line)
            .padding(1.dp)
            .background(MaterialTheme.colorScheme.background.copy(alpha = if (belongsToMonth) 1f else 0.48f))
            .padding(horizontal = 3.dp, vertical = 5.dp)
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = if (belongsToMonth) 1f else 0.34f)
            )
        }
        if (modern && events.isNotEmpty()) {
            Text(
                "${if (dailyNet >= 0) "+" else "-"}${calendarMoney(abs(dailyNet))}",
                style = MaterialTheme.typography.labelSmall,
                color = if (dailyNet >= 0) Color(0xFF74E5A6) else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 3.dp, bottom = 1.dp)
            )
        }
        events.take(3).forEach { event ->
            CalendarEventPill(event, onEvent, modern)
        }
        if (events.size > 3) Text("+${events.size - 3}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
    }
}

@Composable
private fun CalendarEventPill(
    event: FinanceCalendarEventUi,
    onEvent: (FinanceCalendarEventUi) -> Unit,
    modern: Boolean
) {
    val foreground = if (event.color.luminance() > 0.6f) Color(0xFF111111) else Color.White
    val title = calendarEventTitle(event.title)
    if (modern) {
        Column(
            Modifier.fillMaxWidth().padding(top = 3.dp).testTag("calendarEvent-${event.id}").clip(RoundedCornerShape(7.dp))
                .background(event.color.copy(alpha = 0.9f)).clickable { onEvent(event) }
                .padding(horizontal = 5.dp, vertical = 5.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = foreground, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (event.income) "+" else "-"}R$ ${calendarCompactAmount(event.amount)}",
                style = MaterialTheme.typography.labelSmall,
                color = foreground.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 3.dp).testTag("calendarEvent-${event.id}").clip(RoundedCornerShape(7.dp))
            .background(event.color.copy(alpha = 0.88f)).clickable { onEvent(event) }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(categoryIcon(event.category), contentDescription = null, tint = foreground, modifier = Modifier.size(10.dp))
        Column(Modifier.padding(start = 3.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${if (event.income) "+" else "-"}R$ ${calendarCompactAmount(event.amount)}",
                style = MaterialTheme.typography.labelSmall,
                color = foreground.copy(alpha = 0.88f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalendarToolbar(modifier: Modifier = Modifier, onPrevious: () -> Unit, onNext: () -> Unit, onAdd: () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 58.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.96f),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier.height(64.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mes anterior", tint = MaterialTheme.colorScheme.inverseOnSurface)
            }
            IconButton(
                onClick = onAdd,
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Adicionar lançamento", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(29.dp))
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Próxima semana", tint = MaterialTheme.colorScheme.inverseOnSurface)
            }
        }
    }
}

private fun calendarEventTitle(raw: String): String = raw
    .replace(Regex("(?i)^\\d{1,2}\\s+(?:JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)\\s+"), "")
    .replace(Regex("^[•*\\s]+(?:\\d{4}\\s+)?"), "")
    .replace(Regex("^[\\s*\\u2022]+(?:\\d{4}\\s+)?"), "")
    .trim()

private fun calendarCompactAmount(text: String): String = FinanzaCalendarFormatting.compactAmount(text)

private fun calendarAmount(text: String): Double = FinanzaCalendarFormatting.amount(text)

private fun calendarMoney(value: Double): String = "R$ ${FinanzaCalendarFormatting.number(value)}"

internal object FinanzaCalendarFormatting {
    fun compactAmount(text: String): String = number(amount(text))

    fun amount(text: String): Double = text
        .replace("R$", "", ignoreCase = true)
        .replace("\\s".toRegex(), "")
        .replace(".", "")
        .replace(',', '.')
        .toDoubleOrNull()
        ?.let(::abs)
        ?: 0.0

    fun number(value: Double): String = DecimalFormat(
        "#,##0.00",
        DecimalFormatSymbols(Locale("pt", "BR"))
    ).format(value)
}
