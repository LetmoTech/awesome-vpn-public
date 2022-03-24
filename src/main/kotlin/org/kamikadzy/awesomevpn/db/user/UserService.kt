package org.kamikadzy.awesomevpn.db.user

import org.kamikadzy.awesomevpn.utils.unwrap
import org.springframework.stereotype.Service

@Service
class UserService(
    val userRepository: UserRepository
) {
    fun saveUser(user: User) = userRepository.save(user)

    fun removeUser(user: User) = userRepository.delete(user)

    fun getAllUsers(): ArrayList<User> {
        return userRepository.findAll() as ArrayList<User>
    }

    fun getUserByTgId(tgId: Long): User? {
        return userRepository.findByTgId(tgId)
    }

    fun getUserById(id: Long): User? {
        return userRepository.findById(id).unwrap()
    }

    fun setRegisteredById(tgId: Long, isRegistered: Boolean) {
        val user = userRepository.findByTgId(tgId)

        user?.isRegistered = isRegistered
        if (user != null) {
            userRepository.save(user)
        }
    }

    fun setActiveById(tgId: Long, isActive: Boolean) {
        val user = userRepository.findByTgId(tgId)

        user?.isActive = isActive

        if (user != null) {
            userRepository.save(user)
        }
    }
}