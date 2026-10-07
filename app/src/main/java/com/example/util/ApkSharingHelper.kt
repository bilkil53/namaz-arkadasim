package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

object ApkSharingHelper {

    const val DEFAULT_DOWNLOAD_URL = "https://github.com/bilkil53/aile-ile-secdeye/releases/latest/download/Ailece_Secde.apk"
    const val DEFAULT_GITHUB_DOWNLOAD_URL = "https://github.com/bilkil53/aile-ile-secdeye/releases/latest/download/Ailece_Secde.apk"
    const val DEFAULT_GITHUB_REPO_URL = "https://github.com/bilkil53/aile-ile-secdeye"

    /**
     * Builds the complete invitation message including direct APK download link and invite code.
     */
    fun buildInvitationMessage(inviteCode: String, customUrl: String = ""): String {
        val downloadUrl = if (customUrl.isNotBlank() && !customUrl.contains("tinyurl")) customUrl.trim() else DEFAULT_DOWNLOAD_URL
        return buildString {
            append("Selamün aleyküm! Aile ile Secdeye uygulamamızda birlikte namazlarımızı, kazalarımızı ve aile ibadet havuzumuzu takip edelim. 🤲\n\n")
            append("📲 Uygulamayı Buradan İndir (Resmi GitHub Güvenli İndirme Linki):\n")
            append(downloadUrl)
            append("\n\n")
            append("🤝 Aile Havuzu Davet Kodumuz: ")
            append(inviteCode)
            append("\n\n(Uygulamayı yükledikten sonra 'Ailece' sekmesinde 'Kodu Onayla & Katıl' butonuna bu kodu yazarak hemen havuzumuza dahil olabilirsiniz.)")
        }
    }

    /**
     * Directly opens the Android System Share Chooser (WhatsApp, Telegram, SMS, etc.)
     * with the invitation message containing download link and invite code.
     */
    fun shareInvitation(context: Context, inviteCode: String, customUrl: String = "") {
        val message = buildInvitationMessage(inviteCode, customUrl)
        shareViaSystemChooser(context, message)
    }

    /**
     * Shares the text message directly via WhatsApp (or WhatsApp Business / System Chooser fallback).
     */
    fun shareViaWhatsApp(context: Context, message: String, sendApkFile: Boolean = false) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }

            intent.setPackage("com.whatsapp")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                intent.setPackage("com.whatsapp.w4b") // WhatsApp Business
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } else {
                    shareViaSystemChooser(context, message)
                }
            }
        } catch (_: Exception) {
            shareViaSystemChooser(context, message)
        }
    }

    /**
     * Shares the text message via Email/Gmail.
     */
    fun shareViaEmail(context: Context, message: String, sendApkFile: Boolean = false) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, "Namaz Arkadaşım Daveti ve İndirme Bağlantısı")
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(intent, "Mail ile Paylaş"))
        } catch (_: Exception) {
            shareViaSystemChooser(context, message)
        }
    }

    /**
     * Shares the text message via standard Android chooser (SMS, Telegram, Bluetooth, etc.).
     */
    fun shareViaSystemChooser(context: Context, message: String, sendApkFile: Boolean = false) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_SUBJECT, "Namaz Arkadaşım Daveti")
            }
            context.startActivity(Intent.createChooser(intent, "Namaz Arkadaşı Daveti Paylaş"))
        } catch (e: Exception) {
            Toast.makeText(context, "Paylaşım açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
