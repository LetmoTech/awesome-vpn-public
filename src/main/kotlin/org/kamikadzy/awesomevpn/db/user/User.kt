package org.kamikadzy.awesomevpn.db.user

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.agronum.ewcidshipping.model.BaseEntity
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Table

@Repository
interface UserRepository: CrudRepository<User, Long> {
    fun findByTgId(tgId: Long): User?
}

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "users")
data class User (
        val name: String,
        val chatId: Long,
        val tgId: Long,
        var lastMessageId: Long = (-1).toLong(),
        var lastMessageType: Short = -1,
        var ban: Boolean = false,
        val balance: BigDecimal = BigDecimal.ZERO,
        val vpnId: String = ""
        ): BaseEntity<Long>()