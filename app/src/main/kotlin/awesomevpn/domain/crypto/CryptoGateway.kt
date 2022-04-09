package awesomevpn.domain.crypto

interface CryptoGateway {
    fun getNewAddress(): String
    fun getMasterBalance(): Long
    suspend fun sendFundsOnAddress(address: String, amount: Double?, fee: Long?): String
}

open class CryptoGatewayException(message: String) : Exception(message)