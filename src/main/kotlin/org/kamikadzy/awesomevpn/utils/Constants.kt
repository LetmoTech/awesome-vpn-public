package org.kamikadzy.awesomevpn.utils

import org.kamikadzy.awesomevpn.db.user.CryptoCurrencies
import org.kamikadzy.awesomevpn.domain.crypto.BitcoinAPI
import org.kamikadzy.awesomevpn.domain.crypto.CryptoGateway
import org.springframework.stereotype.Component

@Component
class Constants(
    private final val bitcoinAPI: BitcoinAPI
){
    val cryptoGateways: Map<CryptoCurrencies, CryptoGateway> = mapOf(
        CryptoCurrencies.BTC to bitcoinAPI
    )
}