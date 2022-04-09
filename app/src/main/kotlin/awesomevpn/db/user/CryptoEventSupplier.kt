package awesomevpn.db.user

import awesomevpn.db.cryptoinvoice.CryptoInvoice

interface CryptoEventSupplier {
    fun getIdsAndAddresses(cryptoCurrency: CryptoCurrency): List<Pair<Long, String>>

    fun saveBalance(cryptoInvoice: CryptoInvoice)
}

class CryptoEventException(message: String): Exception(message)