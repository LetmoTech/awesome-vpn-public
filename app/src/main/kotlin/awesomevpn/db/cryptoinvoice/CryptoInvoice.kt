package awesomevpn.db.cryptoinvoice

import BitcoinBigDecimal
import awesomevpn.db.BaseAuditEntity
import awesomevpn.db.user.CryptoCurrency
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import javax.persistence.Column
import javax.persistence.Entity

@Repository
interface CryptoInvoiceRepository : CrudRepository<CryptoInvoice, Long> {
    fun findByTxIdAndUserId(txId: String, userId: Long): CryptoInvoice?
}

@DynamicInsert
@DynamicUpdate
@Entity
data class CryptoInvoice(
    val userId: Long,
    val rate: BitcoinBigDecimal,
    @Column(precision = 40, scale = 8)
    val amount: BitcoinBigDecimal,
    val txId: String,
    val cryptoCurrency: CryptoCurrency,
    var status: CryptoInvoiceStatus
) : BaseAuditEntity<Long>() {
    fun calculateInRub(): BigDecimal = (rate * amount).setScale(2, RoundingMode.CEILING)
}

enum class CryptoInvoiceStatus {
    PENDING, CANCELLED, FINISHED
}