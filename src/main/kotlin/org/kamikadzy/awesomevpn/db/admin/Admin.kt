package org.kamikadzy.awesomevpn.db.admin

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import org.kamikadzy.awesomevpn.db.user.User
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import ru.agronum.ewcidshipping.model.BaseEntity
import java.math.BigDecimal
import javax.persistence.Entity
import javax.persistence.Table

@Repository
interface AdminRepository: CrudRepository<Admin, Long> {
    fun findByTgId(tgId: Long): Admin?
    fun findByName(name: String): Admin?
}

@DynamicInsert
@DynamicUpdate
@Entity
@Table(name = "admins")
data class Admin(
        val name: String,
        var chatId: Long,
        var tgId: Long,
        var asUser: Boolean = false,
        val balance: BigDecimal = BigDecimal.ZERO,
        val vpnId: String = ""
): BaseEntity<Long>() {
}