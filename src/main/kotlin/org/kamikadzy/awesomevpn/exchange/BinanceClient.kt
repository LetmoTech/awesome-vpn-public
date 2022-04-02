package org.kamikadzy.awesomevpn.exchange

import com.binance.api.client.BinanceApiClientFactory
import com.binance.api.client.domain.market.CandlestickInterval
import com.binance.api.client.exception.BinanceApiException
import org.kamikadzy.awesomevpn.db.user.DealSource
import org.springframework.stereotype.Component
import java.sql.Timestamp


@Component
class BinanceClient {

    protected val factory = BinanceApiClientFactory.newInstance("", "")
    protected val client = factory.newRestClient()

    suspend fun main() {
        //getPrice(DealSource.BTC,"RUB")
        getInvoicePrice(DealSource.BTC,"RUB",1648802427000)
    }

    suspend fun getLastPrice(asset: DealSource, base: String ) {
        try {
            val tickerStatistics = client.get24HrPriceStatistics(asset.toString() + base.uppercase())
            println(tickerStatistics.lastPrice)
        } catch (e: BinanceApiException) {
            println(e.error.code)
            println(e.error.msg)
        }

    }
    suspend fun getInvoicePrice(asset: DealSource, base: String, timestamp: Long){
        try {
            val symbol = asset.toString() + base.uppercase()
            val candlesticks = client.getCandlestickBars(symbol,CandlestickInterval.ONE_MINUTE,1,timestamp,timestamp+60000)
            println(candlesticks[0].high)
        } catch (e: BinanceApiException) {
            println(e.error.code)
            println(e.error.msg)
        }

    }

}