package org.kamikadzy.awesomevpn.domain.crypto

interface CryptoGateway {
    fun getNewAddress(): String
}