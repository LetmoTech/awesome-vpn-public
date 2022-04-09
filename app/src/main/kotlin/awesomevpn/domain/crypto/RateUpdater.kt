package awesomevpn.domain.crypto

import BitcoinBigDecimal
import awesomevpn.db.user.CryptoCurrency
import awesomevpn.domain.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Component
import ru.gildor.coroutines.okhttp.await
import toBitcoinBigDecimal
import java.util.concurrent.TimeUnit

@Component
class RateUpdater(
    private var constants: Constants
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    private suspend fun getBinanceRate(asset: String = "BTC"): BitcoinBigDecimal? {
        val request = Request.Builder()
            .url("https://api.binance.com/api/v3/ticker/price?symbol=${asset}RUB")
            .get()
            .build()

        val call = httpClient.newCall(request).await()

        if (!call.isSuccessful || call.code !in 200..300) {
            call.body?.close()
            call.close()
            return null
        }

        return try {
            val json = JSONObject(call.body?.string())

            call.body?.close()
            call.close()

            json.getString("price").toBitcoinBigDecimal()
        } catch (e: Exception) {
            print("Something went wrong with Binance. Error: $e")

            null
        }
    }

    init {
        GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                for (asset in CryptoCurrency.values()) {
                    try {
                        val rate = getBinanceRate(asset.name)
                        synchronized(constants.rates) {
                            constants.rates[asset] = rate
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                delay(30000)
            }
        }
    }
}