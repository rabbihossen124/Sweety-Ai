package com.sweety.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiClient(
    private var baseUrl: String = "https://sweety-ai-three.vercel.app"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun setBaseUrl(newUrl: String) {
        baseUrl = if (newUrl.endsWith("/")) newUrl.dropLast(1) else newUrl
    }

    fun getBaseUrl(): String = baseUrl

    suspend fun sendChatMessage(
        message: String,
        sessionId: String = "session_default",
        userId: String = "user_1"
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val jsonBody = JSONObject().apply {
            put("message", message)
            put("sessionId", sessionId)
            put("userId", userId)
        }

        val request = Request.Builder()
            .url("$baseUrl/api/assistant/chat")
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(body).optString("error", "Server returned ${response.code}")
                    } catch (e: Exception) {
                        "Server returned ${response.code}"
                    }
                    return@withContext AssistantResponse(
                        success = false,
                        text = "সার্ভার এরর: $errorMsg",
                        continueListening = false,
                        error = errorMsg
                    )
                }

                val json = JSONObject(body)
                val text = json.optString("text", "")
                val continueListening = json.optBoolean("continueListening", true)
                val returnedSessionId = json.optString("sessionId", sessionId)

                val actionsList = mutableListOf<ActionPayload>()
                val actionsJson = json.optJSONArray("actions") ?: JSONArray()
                for (i in 0 until actionsJson.length()) {
                    val actObj = actionsJson.getJSONObject(i)
                    val actionName = actObj.optString("action", "")
                    val paramsObj = actObj.optJSONObject("params")
                    val paramsMap = mutableMapOf<String, Any?>()
                    if (paramsObj != null) {
                        val keys = paramsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            paramsMap[k] = paramsObj.get(k)
                        }
                    }
                    if (actionName.isNotEmpty()) {
                        actionsList.add(ActionPayload(actionName, paramsMap))
                    }
                }

                AssistantResponse(
                    success = true,
                    text = text,
                    actions = actionsList,
                    sessionId = returnedSessionId,
                    continueListening = continueListening
                )
            }
        } catch (e: Exception) {
            AssistantResponse(
                success = false,
                text = "নেটওয়ার্ক কানেকশন পাওয়া যায়নি। ব্যাকএন্ড সার্ভার কি চলছে?",
                continueListening = false,
                error = e.localizedMessage
            )
        }
    }
}

