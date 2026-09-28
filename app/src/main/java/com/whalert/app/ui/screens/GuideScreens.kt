package com.whalert.app.ui.screens

import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.whalert.app.ui.*

@Composable
private fun ScreenScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.l)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(Space.m))
        content()
    }
}

/** ÉCRAN 6 — Guide de sécurité. */
@Composable
fun GuideScreen(onBack: () -> Unit) {
    ScreenScaffold("Guide de sécurité", onBack) {
        GuideSection("Reconnaître une arnaque", """
            Fausses offres d'emploi exigeant un paiement initial, faux gains de loterie, urgences artificielles,
            liens raccourcis, pression temporelle excessive, demandes de codes de vérification reçus par SMS.
        """.trimIndent())
        GuideSection("Reconnaître un compte frauduleux", """
            Photo de profil volée, numéro proche d'un contact réel (usurpation), promesses financières irrégulières,
            refus d'appel vocal, demandes d'argent ou d'informations personnelles.
        """.trimIndent())
        GuideSection("Conserver les preuves", """
            Capturez les conversations avec date visible, notez l'heure exacte des messages, ne supprimez pas la
            conversation avant le signalement, conservez les liens reçus sans les ouvrir depuis vos comptes sensibles.
        """.trimIndent())
        GuideSection("Ne pas menacer l'autre personne", """
            Restez factuel. N'insultez pas, ne menacez pas, n'essayez pas de « piéger » ou de harceler en retour :
            cela nuirait à votre dossier et pourrait vous exposer juridiquement.
        """.trimIndent())
        GuideSection("Ne pas envoyer de faux signalements", """
            Un faux signalement est une abuse des plateformes et peut constituer une infraction. Signalez uniquement
            ce que vous avez réellement subi ou constaté.
        """.trimIndent())
        GuideSection("Utiliser les mécanismes officiels", """
            Dans WhatsApp : ouvrez la conversation → menu → « Signaler ». Vous pouvez aussi bloquer. Pour les
            infractions graves, contactez les autorités locales compétentes (cybercriminalité).
        """.trimIndent())
        GuideSection("Protéger vos informations", """
            Activez la vérification en deux étapes dans WhatsApp, limitez qui voit votre photo/statut, ne partagez
            jamais un code SMS reçu, méfiez-vous des comptes demandant des captures d'écran de codes.
        """.trimIndent())
    }
}

@Composable
private fun GuideSection(title: String, body: String) {
    SectionCard(modifier = Modifier.padding(bottom = Space.s)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(Space.xs))
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Page « Ce que cette application peut et ne peut pas faire ». */
@Composable
fun TransparencyScreen(onBack: () -> Unit) {
    ScreenScaffold("Transparence", onBack) {
        SectionCard {
            Text("L'application PEUT :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            listOf(
                "préparer un dossier de signalement structuré",
                "organiser vos preuves localement et chiffrées",
                "générer un texte de signalement factuel",
                "demander votre confirmation explicite avant toute transmission",
                "transmettre via un canal réellement disponible (menu officiel WhatsApp, formulaire d'assistance, votre client e-mail)",
                "conserver l'historique local de vos dossiers",
                "afficher les confirmations techniques réellement reçues"
            ).forEach { BulletRow("✓ $it") }
        }
        Spacer(Modifier.height(Space.m))
        SectionCard {
            Text("L'application NE PEUT PAS :", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            listOf(
                "garantir un bannissement ou une suspension",
                "connaître les décisions internes de WhatsApp sans source officielle",
                "envoyer de faux signalements",
                "multiplier artificiellement les signalements d'un même incident",
                "contourner les protections de WhatsApp",
                "forcer WhatsApp à suspendre un compte",
                "vérifier publiquement si un numéro possède un compte WhatsApp"
            ).forEach { BulletRow("✗ $it") }
        }
        Spacer(Modifier.height(Space.m))
        InfoBanner("WhatsApp ne fournit pas de confirmation publique permettant à cette application de vérifier directement si ce compte a été suspendu. Privilégiez toujours les procédures officielles de WhatsApp.")
    }
}

@Composable private fun BulletRow(t: String) {
    Text(t, Modifier.padding(vertical = 2.dp), style = MaterialTheme.typography.bodyMedium)
}
