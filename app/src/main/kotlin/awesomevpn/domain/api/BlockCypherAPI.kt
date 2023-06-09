package awesomevpn.domain.api

import OkHttpUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

class BlockCypherAPIException(message: String) : Exception(message)

@Component
class BlockCypherAPI {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getSuggestedFee(cryptoCurrency: String = "btc"): Long {
        val request = Request.Builder()
            .url("https://api.blockcypher.com/v1/$cryptoCurrency/main")
            .build()

        val response = OkHttpUtils.makeAsyncRequest(
            request = request,
            client = httpClient
        ) ?: throw BlockCypherAPIException("Bad code")

        return JSONObject(response).getLong("high_fee_per_kb")
    }
}