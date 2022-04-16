package awesomevpn.domain

import BitcoinBigDecimal
import awesomevpn.bot.MessageSender
import awesomevpn.db.user.CryptoCurrency
import awesomevpn.db.user.CryptoEventSupplier
import awesomevpn.domain.crypto.CryptoGateway
import awesomevpn.domain.netmaker.VPNInteractor
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*
import java.util.prefs.Preferences

@Component
class Constants {
    val rates =
        Collections.synchronizedMap(EnumMap<CryptoCurrency, BitcoinBigDecimal>(EnumMap(CryptoCurrency::class.java)))
    val cryptoGateways = Collections.synchronizedMap(hashMapOf<CryptoCurrency, CryptoGateway>())
    var cryptoEventSupplier: CryptoEventSupplier? = null

    var cost: BigDecimal = Preferences.userRoot().get("costs", "500.0").toBigDecimal()
        set(value) {
            Preferences.userRoot().put("costs", value.toString())
            field = value
        }

    var messageSender: MessageSender? = null
    var vpnInteractor: VPNInteractor? = null
}
