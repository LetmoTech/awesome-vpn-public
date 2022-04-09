package awesomevpn.db.cryptoinvoice

import org.springframework.stereotype.Service

@Service
class CryptoInvoiceService (
    val cryptoInvoiceRepository: CryptoInvoiceRepository
        ) {
    fun save(cryptoInvoice: CryptoInvoice): CryptoInvoice = cryptoInvoiceRepository.save(cryptoInvoice)
    fun findByTxIdAndUserId(txId: String, userId: Long): CryptoInvoice? = cryptoInvoiceRepository.findByTxIdAndUserId(txId, userId)
}