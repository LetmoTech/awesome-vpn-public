package org.kamikadzy.awesomevpn.db.admin

import org.kamikadzy.awesomevpn.db.admin.Admin
import org.springframework.stereotype.Service

@Service
class AdminService (
        val AdminRepository: AdminRepository
        ) {
    fun saveAdmin(Admin: Admin) = AdminRepository.save(Admin)
    fun removeAdmin(admin: Admin) = AdminRepository.delete(admin)

    fun getAllAdmins(): ArrayList<Admin> {
        return AdminRepository.findAll() as ArrayList<Admin>
    }

    fun getAdminById(id: Long): Admin? {
        return AdminRepository.findByTgId(id)
    }
    fun getAdminByName(name: String): Admin? {
        return AdminRepository.findByName(name)
    }

}