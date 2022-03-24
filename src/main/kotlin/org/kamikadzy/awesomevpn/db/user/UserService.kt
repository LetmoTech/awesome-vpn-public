package org.kamikadzy.awesomevpn.db.user

import org.springframework.stereotype.Service

@Service
class UserService (
        val userRepository: UserRepository
        ) {
    fun saveUser(user: User) = userRepository.save(user)

    fun getAllUsers(): ArrayList<User> {
        return userRepository.findAll() as ArrayList<User>
    }

    fun getUserById(id: Long): User? {
        return userRepository.findByTgId(id)
    }

    fun setRegistratedById(id: Long, registrated: Boolean) {
        val user = userRepository.findByTgId(id)

        user?.registrated = true
        if (user != null) {
            userRepository.save(user)
        }
    }
}