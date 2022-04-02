import awesomevpn.AwesomeVpnApplication
import awesomevpn.db.user.User
import awesomevpn.db.user.UserService
import awesomevpn.domain.netmaker.NetmakerAPI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.io.File

@SpringBootTest(classes = [AwesomeVpnApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NetmakerAPITests @Autowired constructor(
    private val netmakerAPI: NetmakerAPI,
    private val userService: UserService
) {
    private var user = userService.saveUser(
        User(
            chatId = 22143214235,
            name = "test",
            tgId = 3425345345345345
        )
    )

    private fun baseTest(test: suspend (User) -> Any?, asserts: (User) -> Unit): Any? {
        var result: Any? = null

        try {
            runBlocking {
                result = test.invoke(user)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            asserts.invoke(
                userService.getUserById(
                    user
                        .id!!
                )!!
            )
        }


        return result
    }

    @Test
    fun testCreatingUser() {
        baseTest({
            netmakerAPI.createUser(it.tgId)

            while (!userService.getUserById(it.id!!)!!.isRegistered) {
                delay(10)
            }
        }, {
            assert(it.isRegistered)
        })
    }

    @Test
    fun testDownloadingConfig() {
        val file = baseTest({
            netmakerAPI.getUserConf(it.tgId)
        }, {}) as? File

        assert(file?.exists() ?: false)
    }

    @Test
    fun testCreatingQR() {
        val file = baseTest({
            netmakerAPI.getQrCode(it.tgId)
        }, {}) as? File

        assert(file?.exists() ?: false)
    }

    @Test
    fun testEnablingDisablingUser() {
        baseTest({
            netmakerAPI.enableUser(it.tgId)
        }, {
            assert(it.isActive)
        })

        baseTest({
            netmakerAPI.disableUser(it.tgId)
        }, {
            assert(!it.isActive)
        })
    }

    @AfterAll
    fun removeTestUser() {
        try {
            runBlocking {
                netmakerAPI.deleteUser(user.tgId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        userService.removeUser(user)
    }
}