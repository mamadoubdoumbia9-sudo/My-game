package com.whalert.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * OFFICIAL reporting channels only. This app has NO private WhatsApp API and NEVER
 * automates sending reports. It can only:
 *   1. deep-link into WhatsApp with the reported number so the USER performs the
 *      in-app "Signaler" action themselves (official mechanism), and
 *   2. open WhatsApp's official support/report web form, or
 *   3. hand a prepared dossier to the user's own email client (explicit consent).
 * Every action here requires an explicit tap by the user.
 */
object OfficialChannels {

    const val WHATSAPP_PKG = "com.whatsapp"
    const val WHATSAPP_BUSINESS_PKG = "com.whatsapp.w4b"

    /** wa.me link — opens WhatsApp chat with the number, in the user's own session. */
    fun waMeUrl(e164: String): String = "https://wa.me/" + e164.removePrefix("+")

    fun isInAppChannelAvailable(context: Context): Boolean {
        val pm = context.packageManager
        return listOf(WHATSAPP_PKG, WHATSAPP_BUSINESS_PKG).any {
            pm.getLaunchIntentForPackage(it) != null
        }
    }

    /** Opens WhatsApp conversation; the USER then uses WhatsApp's own "Block/Report" menu. */
    fun openWhatsAppConversation(context: Context, e164: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waMeUrl(e164)))
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /** Official WhatsApp support form (public page operated by WhatsApp/Meta). */
    const val SUPPORT_FORM_URL = "https://support.whatsapp.com/hc/requests/new"

    fun openSupportForm(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_FORM_URL))
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }

    /**
     * Hands the generated dossier to the device's email client (user's own mailbox,
     * user's own send action). Recipient is shown; nothing is sent automatically.
     * WhatsApp's official guidance: reporting happens inside WhatsApp; external email
     * does not accelerate review — this is presented honestly in the UI.
     */
    fun composeEmail(context: Context, to: String, subject: String, body: String, attachmentUris: List<Uri>): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // only apps that handle mail
            putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return runCatching {
            if (attachmentUris.isNotEmpty()) {
                intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachmentUris))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Transmettre le dossier par e-mail")); true
        }.getOrDefault(false)
    }
}
