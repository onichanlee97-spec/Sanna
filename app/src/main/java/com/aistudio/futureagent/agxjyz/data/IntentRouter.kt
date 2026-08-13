package com.aistudio.futureagent.agxjyz.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object IntentRouter {

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendWhatsAppMessage(context: Context, phoneNumber: String, message: String): String {
        return try {
            val formattedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uriString = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg"
            
            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uriString)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isAppInstalled(context, "com.whatsapp")) {
                whatsappIntent.setPackage("com.whatsapp")
                context.startActivity(whatsappIntent)
                "Successfully dispatched direct WhatsApp message intent to $phoneNumber."
            } else {
                context.startActivity(whatsappIntent)
                "WhatsApp app not installed. Fallback dispatched view intent to web."
            }
        } catch (e: Exception) {
            "Error routing WhatsApp intent: ${e.localizedMessage}"
        }
    }

    fun openTelegram(context: Context, username: String, message: String? = null): String {
        return try {
            val cleanUsername = username.replace("@", "").trim()
            val uriString = if (!message.isNullOrBlank()) {
                val encodedMsg = URLEncoder.encode(message, "UTF-8")
                "https://t.me/$cleanUsername?text=$encodedMsg"
            } else {
                "https://t.me/$cleanUsername"
            }
            
            val telegramIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uriString)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isAppInstalled(context, "org.telegram.messenger")) {
                telegramIntent.setPackage("org.telegram.messenger")
                context.startActivity(telegramIntent)
                "Successfully dispatched direct Telegram chat intent to @$cleanUsername."
            } else {
                context.startActivity(telegramIntent)
                "Telegram app not installed. Fallback dispatched view intent to web."
            }
        } catch (e: Exception) {
            "Error routing Telegram intent: ${e.localizedMessage}"
        }
    }

    fun playSpotify(context: Context, searchQuery: String): String {
        return try {
            val encodedQuery = URLEncoder.encode(searchQuery, "UTF-8")
            val uriString = "spotify:search:$encodedQuery"
            val webUriString = "https://open.spotify.com/search/$encodedQuery"
            
            val spotifyIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(uriString)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isAppInstalled(context, "com.spotify.music")) {
                context.startActivity(spotifyIntent)
                "Successfully dispatched direct Spotify music search intent for '$searchQuery'."
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUriString)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                "Spotify app not installed. Fallback dispatched view intent to Spotify web search."
            }
        } catch (e: Exception) {
            "Error routing Spotify intent: ${e.localizedMessage}"
        }
    }
}
