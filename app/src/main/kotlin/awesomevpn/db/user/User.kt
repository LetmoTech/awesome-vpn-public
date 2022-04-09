package awesomevpn.db.user

import awesomevpn.db.BaseAuditEntity
import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import awesomevpn.db.BaseEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Repository
interface UserRepository : CrudRepository<User, Long> {
    fun findByTgId(tgId: Long): User?

    @Modifying
    @Query(nativeQuery = true, value = "UPDATE public.users SET balance = balance + :delta WHERE id = :id")
    fun changeBalance(@Param("id") id: Long, @Param("delta") delta: BigDecimal)
}

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "users")
data class User(
    var name: String?,
    val chatId: Long,
    val tgId: Long,
    var lastMessageId: Long = -1L,
    var isBan: Boolean = false,
    var isRegistered: Boolean = false,
    var isActive: Boolean = false,
    @Column(precision = 40, scale = 2)
    val balance: BigDecimal = BigDecimal.ZERO,
    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "crypto_wallets_key", length = 100000)
    @Column(name = "crypto_wallets_val", length = 100000)
    var cryptoWallets: MutableMap<CryptoCurrency, String> = EnumMap(CryptoCurrency::class.java)
) : BaseAuditEntity<Long>() {
    init {
        for (cur in CryptoCurrency.values()) {
            if (cryptoWallets[cur] == null || cryptoWallets[cur]?.isBlank() == true) {

            }
        }
    }
}

enum class CryptoCurrency {
    BTC, ETH, USDT, TRON, MONERO
}
