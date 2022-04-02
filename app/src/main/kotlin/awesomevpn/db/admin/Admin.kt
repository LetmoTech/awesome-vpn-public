package awesomevpn.db.admin

import org.hibernate.annotations.DynamicInsert
import org.hibernate.annotations.DynamicUpdate
import awesomevpn.db.BaseEntity
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import javax.persistence.Entity
import javax.persistence.Table

@Repository
interface AdminRepository : CrudRepository<Admin, Long> {
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
    var asUser: Boolean = false
) : BaseEntity<Long>()