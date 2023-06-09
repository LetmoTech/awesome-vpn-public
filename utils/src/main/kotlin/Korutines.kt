import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

suspend fun <T> retry(
    times: Int = Int.MAX_VALUE,
    initialDelay: Long = 5000, // 0.1 second
    maxDelay: Long = 5000,    // 1 second
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        delay(currentDelay)
        //currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return block() // last attempt
}

fun startSuspended(unit: suspend () -> Any?) = GlobalScope.launch { unit.invoke() }