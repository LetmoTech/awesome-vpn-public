package org.kamikadzy.awesomevpn.domain.api

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.kamikadzy.awesomevpn.utils.OkHttpUtils
import org.springframework.stereotype.Component

class BlockCypherAPIException(message: String): Exception(message)

@Component
class BlockCypherAPI {
    suspend fun getSuggestedFee(cryptoCurrency: String = "btc"): Long {
        val request = Request.Builder()
            .url("https://api.blockcypher.com/v1/$cryptoCurrency/main")
            .build()

        val response = OkHttpUtils.makeAsyncRequest(
            request = request
        ) ?: throw BlockCypherAPIException("Bad code")

        return JSONObject(response).getLong("high_fee_per_kb")
    }
}