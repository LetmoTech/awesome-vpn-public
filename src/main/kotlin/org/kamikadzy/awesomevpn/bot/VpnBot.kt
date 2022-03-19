package org.kamikadzy.awesomevpn.bot

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import javax.annotation.PostConstruct

@Component
class VpnBot(
    @Value("\${telegram.token}")
    val token: String
): TelegramLongPollingBot(), Bot {
    companion object {
        const val ACHTUNG_MESSAGE = "Ошибка!"
    }
    override fun getBotUsername() = "Awesome VPN Bot"

    override fun getBotToken() = token
    
    @Synchronized
    override fun onUpdateReceived(update: Update) {
        try {
            val userName = if (update.hasCallbackQuery()) update.callbackQuery.from.userName else update.message.from.userName
            val userId = if (update.hasCallbackQuery()) update.callbackQuery.from.id else update.message.from.id
            val userChatId = if (update.hasCallbackQuery()) update.callbackQuery.message.chatId.toLong() else update.message.chatId.toLong()

            if (userName in listOf("snitron", "minetik288", "kuratz")) {
                processAdminUpdate(update, userId, userChatId)
            } else {
                processUserUpdate(update)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun processAdminUpdate(update: Update, userId: Long, userChatId: Long) {
        if (update.hasMessage()) {
            val message = update.message
            val splittedMessage = update.message.text.split(" ")

            when (splittedMessage[0]) {
                "/start" -> {
                    sendMessage("Привет, странник!", userChatId, false)
                }

                "/cum" -> {
                    sendMessage("Fucking slave!!!!", userChatId, false)
                }

                else -> sendAchtung(userChatId)
            }
        } else {
            sendAchtung(userChatId)
        }
    }

    fun processUserUpdate(update: Update) {

    }

    override fun <T: java.io.Serializable, Method : BotApiMethod<T>> execute(method: Method): T = super.execute(method)
}