package org.kamikadzy.awesomevpn.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.kamikadzy.awesomevpn.utils.nonMarkdownShielded
import org.kamikadzy.awesomevpn.utils.telegramShielded
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.File

interface Bot {
    fun  <T: java.io.Serializable, Method: BotApiMethod<T>> execute(method: Method): T

    fun execute(sendDocument: SendDocument): Message

    fun sendAchtung(chatId: Long) {
        GlobalScope.launch (Dispatchers.Default) {
            val sendMessage = SendMessage()
            sendMessage.chatId = chatId.toString()
            sendMessage.text = VpnBot.ACHTUNG_MESSAGE

            execute(sendMessage)
        }
    }

    fun sendMessage(text: String, chatId: Long, shielded: Boolean, buttons: List<Pair<String, String>>? = null) {
        println("SENDING")
        if (text.length > 4096) {
            val splitted = text.split("\n")

            var i = 0
            var reducedText = ""

            while (i in splitted.indices) {
                while (i in splitted.indices && (reducedText + splitted[i] + "\n").length <= 4096) {
                    reducedText += splitted[i] + "\n"
                    i++
                }

                sendMessage(reducedText, chatId, shielded)

                reducedText = ""
            }

            return
        }


        val sendMessage = SendMessage()
        sendMessage.chatId = chatId.toString()
        sendMessage.text = if (shielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()
        sendMessage.parseMode = "MarkdownV2"

        if (buttons != null) {
            val inlineKeyboardMarkup = InlineKeyboardMarkup()
            inlineKeyboardMarkup.keyboard = makeKeyboard(buttons)
            sendMessage.replyMarkup = inlineKeyboardMarkup
        }

        GlobalScope.launch (Dispatchers.Default) {
            try {
                execute(sendMessage)
            } catch (e: Exception) {
                e.printStackTrace()

                sendAchtung(chatId)
            }
        }
    }

    private fun makeKeyboard(buttonsInfo: List<Pair<String, String>>): List<List<InlineKeyboardButton>> {
        val keyboard = arrayListOf<List<InlineKeyboardButton>>()

        var twoList = arrayListOf<InlineKeyboardButton>()
        for ((name, code) in buttonsInfo) {
            val button = InlineKeyboardButton()
            button.text = name
            button.callbackData = code

            twoList.add(button)

            if (twoList.size == 2) {
                keyboard.add(twoList)
                twoList = arrayListOf()
            }
        }

        if (twoList.isNotEmpty()) {
            keyboard.add(twoList)
        }

        return keyboard
    }

    fun sendDocument(document: File, text: String, chatId: Long, markdownShielded: Boolean = true) {
        val sendDocument = SendDocument()
        sendDocument.chatId = chatId.toString()
        sendDocument.document = InputFile(document, document.name)
        sendDocument.caption = if(markdownShielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()

        sendDocument.parseMode = "MarkdownV2"

        GlobalScope.launch (Dispatchers.Default) {
            execute(sendDocument)
        }
    }

    fun answerCallbackQuery(id: String) {
        val answerCallbackQuery = AnswerCallbackQuery()
        answerCallbackQuery.callbackQueryId = id

        GlobalScope.launch (Dispatchers.Default) {
            execute(answerCallbackQuery)
        }
    }
}