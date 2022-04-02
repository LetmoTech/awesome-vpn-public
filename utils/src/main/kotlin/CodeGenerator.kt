import java.util.*

object CodeGenerator {
    fun generateRandomCode(): String {
        return UUID.randomUUID().toString()
    }
}