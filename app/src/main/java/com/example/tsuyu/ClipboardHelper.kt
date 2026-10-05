package com.example.tsuyu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object ClipboardHelper {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Tsuyu OCR", text)
        clipboard.setPrimaryClip(clip)
        
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }
}
