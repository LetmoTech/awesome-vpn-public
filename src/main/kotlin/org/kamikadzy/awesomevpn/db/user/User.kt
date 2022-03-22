package org.kamikadzy.awesomevpn.db.user

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.agronum.ewcidshipping.model.BaseEntity
import java.math.BigDecimal
import java.util.*
import javax.persistence.*

@Repository
interface UserRepository: CrudRepository<User, Long> {
    fun findByTgId(tgId: Long): User?
}

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "users")
data class User (
        var name: String?,
        val chatId: Long,
        val tgId: Long,
        var lastMessageId: Long = (-1).toLong(),
        var lastMessageType: String? = "start",
        var ban: Boolean = false,
        val balance: BigDecimal = BigDecimal.ZERO,
        val vpnId: String = "",
        @ElementCollection(fetch = FetchType.EAGER)
        @MapKeyColumn(name = "cards_with_daaz_tokens_key", length = 100000)
        @Column(name = "cards_with_daaz_tokens_val", length = 100000)
        var tokens: MutableMap<DealSource, String> = EnumMap(DealSource::class.java)
        ): BaseEntity<Long>()

enum class DealSource {
    BTC, ETH, USDT, TRON, MONERO
}
