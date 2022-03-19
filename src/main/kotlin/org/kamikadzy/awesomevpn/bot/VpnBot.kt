package org.kamikadzy.awesomevpn.bot

import org.kamikadzy.awesomevpn.db.user.User
import org.kamikadzy.awesomevpn.db.user.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class VpnBot(
    @Value("\${telegram.token}")
    val token: String,
    val userService: UserService
): TelegramLongPollingBot(), Bot {
    companion object {
        const val ACHTUNG_MESSAGE = "Ошибка!"
    }

    override fun getBotUsername(): String {
        return "Awesome VPN Bot"
    }

    override fun getBotToken() = token
    
    @Synchronized
    override fun onUpdateReceived(update: Update) {
        try {
            val userName = update.message.from.userName
            val userId = update.message.from.id
            val userChatId = update.message.chatId.toLong()

            var user = userService.getUserById(userId)

            if (user == null) {
                user = userService.saveUser(
                        User(
                                name = userName,
                                chatId = userChatId,
                                tgId = userId
                        )
                )
            }

            if (listOf("snitron", "minetik288", "kuratz").contains(userName)) {
                processAdminUpdate(update, user)
            } else {
                processUserUpdate(update)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processAdminUpdate(update: Update, user: User) {
        if (update.hasMessage()) {
            val text = update.message
            val splittedMessage = update.message.text.split(" ")

            when (splittedMessage[0]) {
                "/start" -> {
                    sendMessage("Привет, странник!", user.chatId, false)
                }

                "/cum" -> {
                    sendMessage("Fucking slave!!!!", user.chatId, false)
                }

                "/hello" -> sendMessage("Привет, ${user.name}!", user.chatId, false)

                else -> sendAchtung(user.chatId)
            }
        } else {
            sendAchtung(user.chatId)
        }
    }

    fun processUserUpdate(update: Update) {

    }

    override fun <T: java.io.Serializable, Method : BotApiMethod<T>> execute(method: Method): T = super.execute(method)
}