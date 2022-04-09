package awesomevpn.db.user

import awesomevpn.db.cryptoinvoice.CryptoInvoice
import awesomevpn.domain.Constants
import org.springframework.stereotype.Service
import unwrap
import javax.annotation.PostConstruct

@Service
class UserService(
    private val userRepository: UserRepository,
    private val constants: Constants
) : CryptoEventSupplier {

    @PostConstruct
    fun postInit() {
        constants.cryptoEventSupplier = this
    }

    fun saveUser(user: User): User = userRepository.save(user)

    fun removeUser(user: User) = userRepository.delete(user)

    fun createUser(tgId: Long, chatId: Long, name: String?): User {
        val newUser = User(
            tgId = tgId,
            chatId = chatId,
            name = name
        )

        for ((cur, gateway) in constants.cryptoGateways) {
            newUser.cryptoWallets[cur] = gateway.getNewAddress()
        }

        return userRepository.save(newUser)
    }

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

    override fun getIdsAndAddresses(cryptoCurrency: CryptoCurrency): List<Pair<Long, String>> = userRepository.findAll()
        .map { it.id!! to it.cryptoWallets[cryptoCurrency]!! }

    override fun saveBalance(cryptoInvoice: CryptoInvoice) {
        //TODO: notify balance add

        val user = userRepository.findById(cryptoInvoice.userId).unwrap()
            ?: throw CryptoEventException("No user found for $cryptoInvoice")

        userRepository.changeBalance(user.id!!, cryptoInvoice.calculateInRub())
    }
}