package com.example.myapplication.data

import com.example.myapplication.model.AdChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AiSearchResponse(
    val type: String,
    val explanation: String = "",
    val matchedAdIds: List<String> = emptyList(),
    val suggestedRefinements: List<String> = emptyList(),
    val question: String = "",
    val toolArgumentsSummary: String = ""
)

object AiSearchRepository {
    // Android Emulator uses 10.0.2.2 for the host machine. For a real device, use your computer's LAN IP.
    private const val DEFAULT_AI_SEARCH_BASE_URL = "http://100.102.234.68:8000"

    suspend fun aiSearch(
        query: String,
        currentChannel: AdChannel?,
        limit: Int = 12
    ): Result<AiSearchResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("$DEFAULT_AI_SEARCH_BASE_URL/api/ai-search")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val requestJson = JSONObject()
                .put("query", query)
                .put("currentChannel", currentChannel?.name)
                .put("limit", limit)
                .toString()

            connection.outputStream.use { output ->
                output.write(requestJson.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseText = readResponseText(connection, responseCode)
            connection.disconnect()

            if (responseCode !in 200..299) {
                error("AI search failed with HTTP $responseCode: $responseText")
            }

            JSONObject(responseText).toAiSearchResponse()
        }
    }

    private fun readResponseText(
        connection: HttpURLConnection,
        responseCode: Int
    ): String {
        val stream = if (responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun JSONObject.toAiSearchResponse(): AiSearchResponse {
        val type = optString("type")
        return AiSearchResponse(
            type = type,
            explanation = optString("explanation"),
            matchedAdIds = optJSONArray("matchedAdIds").toStringList(),
            suggestedRefinements = when (type) {
                "clarify" -> optJSONArray("suggestedOptions").toStringList()
                else -> optJSONArray("suggestedRefinements").toStringList()
            },
            question = optString("question"),
            toolArgumentsSummary = optJSONObject("toolArguments").toSummaryText()
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }
            .filter { it.isNotBlank() }
    }

    private fun JSONObject?.toSummaryText(): String {
        if (this == null) return ""
        val parts = buildList {
            optString("intent").takeIf { it.isNotBlank() }?.let { add("意图 $it") }
            optString("preferredChannel").takeIf { it.isNotBlank() && it != "null" }?.let { add("频道 $it") }
            optJSONArray("tags").toStringList().takeIf { it.isNotEmpty() }?.let { add("标签 ${it.joinToString("、")}") }
            optJSONArray("keywords").toStringList().takeIf { it.isNotEmpty() }?.let { add("关键词 ${it.joinToString("、")}") }
        }
        return parts.joinToString(" · ")
    }
}
