package org.kamikadzy.awesomevpn.domain.netmaker

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.kamikadzy.awesomevpn.utils.OkHttpUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration

class NetmakerAPIException(s: String) : Exception(s)

@Component
class NetmakerAPI (
    @Value("\${netmaker.token}")
    val token: String,
    @Value("\${netmaker.api-url}")
    val apiUrl: String,
    @Value("\${netmaker.api-create-user-url}")
    val apiCreateUserUrl: String
        ) {
    private val httpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(TokenInterceptor())
            .connectTimeout(Duration.ofMinutes(1))
            .build()

    private inner class TokenInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val newReq = chain.request()
                .newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            return chain.proceed(newReq)
        }
    }

    suspend fun getUserNamesList(): List<String> {
        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .build()

        val response = OkHttpUtils.makeAsyncRequest(
            client = httpClient,
            request = request
        ) ?: throw NetmakerAPIException("Bad code")

        val jsonArray = JSONArray(response)
        val userNames = arrayListOf<String>()

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)

            userNames.add(jsonObject.getString("clientid"))
        }

        return userNames
    }

    suspend fun changeUserValue(prevName: String, newName: String, enabled: Boolean = false) {
        val updateNameRequest = Request.Builder()
            .url("${apiUrl}/vpn/${prevName}")
            .post("{\"clientid\": \"$newName\", \"enabled\": $enabled}".toRequestBody(OkHttpUtils.JSON_TYPE))
            .build()

        OkHttpUtils.makeAsyncRequest(httpClient, updateNameRequest) ?: throw NetmakerAPIException("Bad code")
    }

    suspend fun createUser(id: Long) {
        val usersBefore = getUserNamesList().toSet()

        val createRequest = Request.Builder()
            .url(apiCreateUserUrl)
            .post("".toRequestBody())
            .build()

        OkHttpUtils.makeAsyncRequest(httpClient, createRequest) ?: throw NetmakerAPIException("Bad code")

        val usersAfter = getUserNamesList().toSet()
        val userName = (usersAfter - usersAfter).first()
    }
}