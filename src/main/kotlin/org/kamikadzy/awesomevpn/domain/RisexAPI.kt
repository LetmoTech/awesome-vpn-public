package org.kamikadzy.awesomevpn.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.kamikadzy.awesomevpn.db.user.UserService
import org.kamikadzy.awesomevpn.utils.retry
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.gildor.coroutines.okhttp.await
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*
import javax.annotation.PostConstruct
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Legacy code
 */

@Component
class RisexWSAPI (
    //val userService: UserService
        ) {
    var gotUpdate: (suspend (JSONObject) -> Unit)? = null
    val okHttpClient = OkHttpClient.Builder().build()

    private val webSocket = okHttpClient.newWebSocket(Request.Builder().apply {
        url("ws://localhost:8091")
    }.build(), object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)

            GlobalScope.launch(Dispatchers.Default) {
                try {
                    println(text)
                    gotUpdate?.invoke(JSONObject(text))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            t.printStackTrace()
        }
    })

    fun addUsers(users: List<Pair<Long, String>>) {
        webSocket.send(
            JSONObject(
                mapOf(
                    "type" to "add_users",
                    "users" to users.map {
                        mapOf(
                            "id" to it.first,
                            "token" to it.second
                        )
                    }
                )
            ).toString()
        )
    }

    fun updateUser(previousToken: String, nextToken: String) {
        webSocket.send(
            JSONObject(
                mapOf(
                    "type" to "update_user",
                    "prev_token" to previousToken,
                    "next_token" to nextToken
                )
            ).toString()
        )
    }

    fun deleteUser(token: String) {
        webSocket.send(
            JSONObject(
                mapOf(
                    "type" to "delete_user",
                    "token" to token
                )
            ).toString()
        )
    }
}

class RisexAPI(
    val token: String
) {
    val BASE_URL = "https://api.risex.net/api/v1"

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.getDefault())
    private val dealDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss XXX", Locale.getDefault())

    private val httpClient = OkHttpProvider().httpClient.value
    private var advertisementId: Long = -1L
    get() {
        if (field == -1L) {
            field = getAdvertisementID()
        }

        return field
    }

    companion object {
        const val MIN_BALANCE = 10000

        fun dealLink(id: Long) = "https://trade.risex.net/deal/$id"
        fun tokenLink() = "https://api.risex.net/api/v1/auth/sign-in"
    }

    inner class OkHttpProvider {
        fun tokenLink() = "$BASE_URL/auth/sign-in"
        fun dealLink(id: Long) = "https://trade.risex.net/deal/$id"

        val httpClient = lazy {
            OkHttpClient.Builder()
                .addNetworkInterceptor(TokenInterceptor())
                .connectTimeout(Duration.ofMinutes(1))
                .build()
        }

        inner class TokenInterceptor() : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val newReq = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                return chain.proceed(newReq)
            }
        }
    }

    suspend fun getDealById(dealId: Long): JSONObject? {
        val request = Request.Builder()
            .url("$BASE_URL/deal/$dealId") //TODO: Currency check
            .get()
            .build()
        val call = httpClient.newCall(request).await()

        val string = withContext(Dispatchers.Default) { call.body!!.string() }
        call.body?.close()
        call.close()

        return if (call.code == 200) JSONObject(string) else null
    }

    fun getCryptoBalance(): Double {
        val request = Request.Builder()
            .url("$BASE_URL/profile/balance/btc") //TODO: Currency check
            .get()
            .build()
        val call = httpClient.newCall(request).execute()

        val string = call.body!!.string()


        //println(string)
        val data = JSONObject(string).getJSONObject("data").getDouble("amount")

        call.body?.close()
        call.close()

        val amount = data

        return amount
    }

    fun getBalance(cryptoCurrency: String = "btc", fiatCurrency: String = "rub"): Double {
        val request = Request.Builder()
            .url("$BASE_URL/profile/balance/$cryptoCurrency") //TODO: Currency check
            .get()
            .build()
        val call = httpClient.newCall(request).execute()

        val string = call.body!!.string()


        //println(string)
        val data = JSONObject(string).getJSONObject("data").getJSONObject("fiat")

        call.body?.close()
        call.close()

        val amount = data.getDouble("amountUsd")
        val rates = data.getJSONArray("rates")
        var fiatRate = 0.0

        for (i in 0 until rates.count()) {
            val rate = rates.getJSONObject(i)
            if (rate.getString("code") == fiatCurrency) {
                fiatRate = rate.getDouble("rate")

                break
            }
        }

        return amount * fiatRate
    }

    suspend fun ping() {
        val request = Request.Builder()
            .url("$BASE_URL/profile/balance/btc") //TODO: Currency check
            .get()
            .build()
        val call = httpClient.newCall(request).await()

        call.body?.close()
        call.close()
    }

    fun closeDeal(dealId: Long, neededCloseByAPI: Boolean = true) {
        GlobalScope.launch {
            println("CLOSING $dealId")
            if (neededCloseByAPI) {
                retry(times = 20) {
                    val requestFin = Request.Builder()
                        .url("$BASE_URL/deal/$dealId/finish")
                        .put(FormBody.Builder().build())
                        .build()

                    val callFinish = httpClient.newCall(requestFin).execute()
                    callFinish.body?.close()
                    callFinish.close()
                    //   if (callFinish.code != 200
                    if (callFinish.code !in 200..300) {
                        //sendProps(KeyValueStorage.get(KeyValueStorage.PROPS_KEY)!!)
                        throw Exception("Finish bad code ${callFinish.code}")
                    }
                }
            }
        }
    }

    /*fun disputeDeals(
        vararg deals: Deal,
        updateBalance: Boolean = true,
        smsIds: String
    ) {
        GlobalScope.launch(Dispatchers.Default) {
            val successfullyDisputed = hashSetOf<Long>()

            retry(times = 20) {
                for (deal in deals) {
                    if (!successfullyDisputed.contains(deal.stockExchangeId)) {
                        val dealInfo = getDealById(deal.stockExchangeId)!!

                        val statusHistory = dealInfo.getJSONObject("data").getJSONArray("status_history")
                        val status =
                            dealInfo.getJSONObject("data").getJSONObject("status").getString("title").trim()

                        var wasPaid = false

                        for (i in 0 until statusHistory.count()) {
                            if (statusHistory.getJSONObject(i).getString("title") == "Paid") {
                                wasPaid = true
                                break
                            }
                        }

                        val statusCheck =
                            status != "In dispute" && status != "Cancelled" && status != "Cancellation" && status != "Finishing" && status != "Finished" && status != "Autocancelled"
                        val sendToServer = statusCheck && wasPaid

                        val endDate = Date()
                        endDate.time = System.currentTimeMillis() + 10800000

                        if (sendToServer) {
                            val request = Request.Builder()
                                .url("$BASE_URL/deal/${deal.stockExchangeId}/dispute")
                                .put(FormBody.Builder().build())
                                .build()

                            val call = httpClient.newCall(request).execute()
                            println(call.body?.string())

                            call.body?.close()
                            call.close()
                            if (call.code != 200) {
                                throw Exception("Bad code ${call.code} at deal ${deal.stockExchangeId} closing! New attempt")
                            }
                        }

                        successfullyDisputed.add(deal.stockExchangeId) //Means deal successfully disputed
                    }
                }
            }
        }
    }*/

    fun addRequisite(requisite: String): Int {
        val request2 = Request.Builder()
            .url("$BASE_URL/offer/$advertisementId/requisite/")
            .post("""{"type": "card", "requisite":"$requisite", "auto_select": true}""".toRequestBody("application/json;charset=UTF-8".toMediaTypeOrNull()))
            .build()

        println("$BASE_URL/offer/$advertisementId/requisite/")
        val call2 = httpClient.newCall(request2).execute()
        val string = call2.body!!.string()
        println("$BASE_URL/offer/$advertisementId/requisite/")
        println(string)
        call2.body?.close()

        if (call2.code !in 200..300) {
            throw Exception("Bad code send props!")
        }

        call2.close()
        return JSONObject(string).getJSONObject("data").getInt("id")
    }

    private fun getAdvertisement(): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL/offer")
            .get()
            .build()
        val call = httpClient.newCall(request).execute()

        val string = call.body!!.string()
        val data = JSONObject(string).getJSONArray("data")
        call.body?.close()
        call.close()

        return data.getJSONObject(0)
    }

    private fun getAdvertisementByID(id: Long): JSONObject {
        val request = Request.Builder()
            .url("$BASE_URL/offer/$id")
            .get()
            .build()
        val call = httpClient.newCall(request).execute()

        val string = call.body!!.string()
        val data = JSONObject(string).getJSONObject("data")
        call.body?.close()
        call.close()

        return data
    }

    private fun getAdvertisementID(): Long {
        return getAdvertisement().getLong("id")
    }

    fun getAllRequisites(): List<Pair<Long, String>> {
        if (advertisementId == -1L) {
            advertisementId = getAdvertisementID()
        }

        val request = Request.Builder()
            .url("$BASE_URL/offer/$advertisementId/requisite/")
            .get()
            .build()

        val call2 = httpClient.newCall(request).execute()
        val string = call2.body!!.string()
        val json = JSONObject(string).getJSONArray("data")

        val requisites = mutableListOf<Pair<Long, String>>()

        for (i in 0 until json.length()) {
            val jsonRequisite = json.getJSONObject(i)
            requisites.add(jsonRequisite.getLong("id") to jsonRequisite.getString("requisite"))
        }
        call2.body?.close()

        if (call2.code !in 200..300) {
            throw Exception("Bad code send props!")
        }

        call2.close()

        return requisites
    }

    fun removeRequisite(id: Long) {
        val request = Request.Builder()
            .url("$BASE_URL/requisite/$id/")
            .delete()
            .build()

        val call2 = httpClient.newCall(request).execute()
        call2.body?.close()

        if (call2.code !in 200..300) {
            throw Exception("Bad code send props!")
        }

        call2.close()
    }

    fun updateLimitOnRequisite(id: Long, limit: Long) {
        val request = Request.Builder()
            .url("$BASE_URL/requisite/$id")
            .put("""{"max": $limit}""".toRequestBody("application/json;charset=UTF-8".toMediaTypeOrNull()))
            .build()

        val call = httpClient.newCall(request).execute()
        call.body?.close()
    }

    fun changeRequisiteState(requisite: String, id: Long, enabled: Boolean): Int {
        val request = Request.Builder()
            .url("$BASE_URL/requisite/$id")
            .put("""{"auto_select": $enabled, "requisite": "$requisite"}""".toRequestBody("application/json;charset=UTF-8".toMediaTypeOrNull()))
            .build()

        val call = httpClient.newCall(request).execute()
        val string = call.body?.string()
        val reqId = JSONObject(string).getJSONObject("data").getInt("id")

        call.body?.close()

        return reqId
    }

    fun changeAdvertisementState(enabled: Boolean) {
        val request = Request.Builder()
            .url("$BASE_URL/offer/$advertisementId")
            .put("""{"id": $advertisementId, "is_active": $enabled}""".toRequestBody("application/json;charset=UTF-8".toMediaTypeOrNull()))
            .build()

        val call = httpClient.newCall(request).execute()
        call.body?.close()
    }

    fun setAdvertisementMax(max: Double) {
        val previousPrice = getAdvertisementByID(advertisementId).getDouble("price")

        val request = Request.Builder()
            .url("$BASE_URL/offer/$advertisementId")
            .put("""{"id": $advertisementId, "price": $previousPrice, "min": 500, "max": $max}""".toRequestBody("application/json;charset=UTF-8".toMediaTypeOrNull()))
            .build()

        val call = httpClient.newCall(request).execute()
        call.body?.close()
    }

    suspend fun getAllClaims(): JSONObject? {
        val request = Request.Builder()
            .url("$BASE_URL/claim/incoming?closed=0")
            .get()
            .build()

        val response = httpClient.newCall(request).await()

        val string = withContext(Dispatchers.Default) { response.body?.string() } ?: "{}"
        val code = response.code

        response.body?.close()
        response.close()

        return if (code in 200..300) JSONObject(string) else null
    }

    fun applyClaim(id: Long) {
        GlobalScope.launch {
            retry(times = 20) {
                val requestFin = Request.Builder()
                    .url("$BASE_URL/claim/$id/apply")
                    .put(FormBody.Builder().build())
                    .build()

                val callFinish = httpClient.newCall(requestFin).execute()
                callFinish.body?.close()
                callFinish.close()

                if (callFinish.code !in 200..300) {
                    throw Exception("Apply claim bad code ${callFinish.code}")
                }
            }
        }
    }

    fun rejectClaim(id: Long) {
        GlobalScope.launch {
            retry(times = 20) {
                val requestFin = Request.Builder()
                    .url("$BASE_URL/claim/$id/reject")
                    .put(FormBody.Builder().build())
                    .build()

                val callFinish = httpClient.newCall(requestFin).execute()
                callFinish.body?.close()
                callFinish.close()

                if (callFinish.code !in 200..300) {

                    throw Exception("Reject claim bad code ${callFinish.code}")
                }
            }
        }
    }

    fun getLogin(): String {
        val request = Request.Builder()
            .url("$BASE_URL/profile/") //TODO: Currency check
            .get()
            .build()
        val call = httpClient.newCall(request).execute()

        val string = call.body!!.string()


        //println(string)
        val data = JSONObject(string).getJSONObject("data").getString("login")

        call.body?.close()
        call.close()

        return data
    }
}