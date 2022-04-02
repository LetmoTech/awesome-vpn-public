package awesomevpn.domain.crypto

interface CryptoGateway {
    fun getNewAddress(): String
}