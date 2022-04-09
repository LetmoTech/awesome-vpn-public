package awesomevpn.domain

import BitcoinBigDecimal
import awesomevpn.db.user.CryptoCurrency
import awesomevpn.db.user.CryptoEventSupplier
import awesomevpn.domain.crypto.CryptoGateway
import org.springframework.stereotype.Component
import java.util.*

@Component
class Constants {
    val rates = Collections.synchronizedMap(EnumMap<CryptoCurrency, BitcoinBigDecimal>(EnumMap(CryptoCurrency::class.java)))
    val cryptoGateways = Collections.synchronizedMap(hashMapOf<CryptoCurrency, CryptoGateway>())
    var cryptoEventSupplier: CryptoEventSupplier? = null

}