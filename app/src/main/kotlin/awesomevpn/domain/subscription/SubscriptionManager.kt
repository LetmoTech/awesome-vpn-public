package awesomevpn.domain.subscription

import awesomevpn.db.user.UserService
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import startSuspended

@Component
class SubscriptionManager (
    val userService: UserService
        ) {
    @Async
    @Scheduled(cron = "0 0 */11 * * *")
    fun checkSubscription() = userService.getAllUsers().forEach {
        startSuspended { userService.checkSubscriptions(it) }
    }
}