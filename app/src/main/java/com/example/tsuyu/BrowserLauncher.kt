package com.example.tsuyu

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object BrowserLauncher {
    fun openInFirefox(context: Context, text: String) {
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val uri = Uri.parse("https://jisho.org/search/$encodedText")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("org.mozilla.firefox")
            // CRITICAL: Required when calling startActivity() from outside of an Activity context (like a Service)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
        }

        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Firefox not found, using default browser.", Toast.LENGTH_SHORT).show()
            
            // Fallback: Remove the package restriction to let the system resolver handle it
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            try {
                context.startActivity(fallbackIntent)
            } catch (fallbackEx: ActivityNotFoundException) {
                Toast.makeText(context, "No web browser installed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
