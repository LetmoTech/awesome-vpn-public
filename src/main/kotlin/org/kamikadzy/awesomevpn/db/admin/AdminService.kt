package org.kamikadzy.awesomevpn.db.admin

import org.springframework.stereotype.Service

@Service
class AdminService(
    private val adminRepository: AdminRepository
) {
    fun saveAdmin(Admin: Admin) = adminRepository.save(Admin)
    fun removeAdmin(admin: Admin) = adminRepository.delete(admin)

    fun getAllAdmins(): ArrayList<Admin> {
        return adminRepository.findAll() as ArrayList<Admin>
    }

    fun getAdminById(id: Long): Admin? {
        return adminRepository.findByTgId(id)
    }

    fun getAdminByName(name: String): Admin? {
        return adminRepository.findByName(name)
    }

}