package com.whalert.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.whalert.app.core.AccountState
import com.whalert.app.core.CaseStatus
import com.whalert.app.core.ModerationDecision

/** Design tokens — 8dp grid, consistent radii/typography (Pixel-perfect spec in README). */
object Space { val xs = 4.dp; val s = 8.dp; val m = 16.dp; val l = 24.dp; val xl = 32.dp }
object Radius { val card = 20.dp; val button = 14.dp; val field = 14.dp }

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Radius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) { Column(Modifier.padding(Space.m), content = content) }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.97f, label = "btnScale")
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.scale(scale).height(52.dp),
        shape = RoundedCornerShape(Radius.button),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.5.dp)
        else Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InfoBanner(text: String, tone: BannerTone = BannerTone.NEUTRAL) {
    val bg = when (tone) {
        BannerTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer
        BannerTone.WARN -> MaterialTheme.colorScheme.errorContainer
        BannerTone.OK -> MaterialTheme.colorScheme.tertiaryContainer
    }
    Surface(shape = RoundedCornerShape(Radius.field), color = bg, modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(Space.m), style = MaterialTheme.typography.bodyMedium)
    }
}

enum class BannerTone { NEUTRAL, WARN, OK }

@Composable
fun StatusChip(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(label, Modifier.padding(horizontal = Space.m, vertical = Space.xs),
            style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

fun accountColor(s: AccountState) = when (s) {
    AccountState.CONNECTED -> Color(0xFF2E7D32)
    AccountState.NOT_CONNECTED -> Color(0xFF757575)
    AccountState.SESSION_EXPIRED -> Color(0xFFEF6C00)
    AccountState.CONNECTION_ERROR -> Color(0xFFC62828)
}

fun caseStatusColor(s: CaseStatus) = when (s) {
    CaseStatus.DRAFT -> Color(0xFF9E9E9E)
    CaseStatus.PREPARED -> Color(0xFF1976D2)
    CaseStatus.SUBMITTED -> Color(0xFFEF6C00)
    CaseStatus.CONFIRMED -> Color(0xFF2E7D32)
    CaseStatus.FOLLOW_UP -> Color(0xFFF9A825)
    CaseStatus.CLOSED -> Color(0xFF455A64)
}

@Composable
fun ModerationNotice(decision: ModerationDecision) {
    val text = when (decision) {
        ModerationDecision.UNKNOWN, ModerationDecision.NO_OFFICIAL_CONFIRMATION ->
            "WhatsApp ne fournit pas de confirmation publique permettant à cette application de vérifier directement si ce compte a été suspendu."
        ModerationDecision.OFFICIAL_INFO_AVAILABLE ->
            "Information officielle disponible — consultez la source indiquée dans le dossier."
        ModerationDecision.ACTION_REPORTED_BY_WHATSAPP ->
            "Action signalée par WhatsApp via une source officielle vérifiable."
    }
    InfoBanner(text, BannerTone.NEUTRAL)
}
