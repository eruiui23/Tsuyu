package com.example.tsuyu

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class JapaneseTokenizer(context: Context, vocabFileName: String = "vocab.txt") {

    private val idToToken: Map<Long, String>

    init {
        val map = mutableMapOf<Long, String>()
        context.assets.open(vocabFileName).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).useLines { lines ->
                lines.forEachIndexed { index, line ->
                    map[index.toLong()] = line.trim()
                }
            }
        }
        idToToken = map
    }

    /**
     * Decodes a list of token IDs into a normalized Japanese string.
     * Ignores structural BERT tokens and strips WordPiece "##" prefixes.
     */
    fun decode(tokenIds: List<Long>): String {
        val sb = StringBuilder()
        val specialTokens = setOf("[PAD]", "[CLS]", "[SEP]", "[UNK]", "[MASK]")

        for (id in tokenIds) {
            val token = idToToken[id] ?: continue
            if (token in specialTokens) continue

            if (token.startsWith("##")) {
                sb.append(token.substring(2))
            } else {
                sb.append(token)
            }
        }

        return sb.toString()
            .replace("\n", "")
            .replace("\r", "")
            .trim()
    }

    companion object {
        const val PAD_TOKEN_ID = 0L
        const val UNK_TOKEN_ID = 1L
        const val CLS_TOKEN_ID = 2L
        const val SEP_TOKEN_ID = 3L
    }
}
