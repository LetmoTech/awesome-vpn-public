package org.kamikadzy.awesomevpn.bot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.kamikadzy.awesomevpn.utils.nonMarkdownShielded
import org.kamikadzy.awesomevpn.utils.telegramShielded
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto
import org.telegram.telegrambots.meta.api.methods.send.SendSticker
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.io.File

interface Bot {
    fun <T : java.io.Serializable, Method : BotApiMethod<T>> execute(method: Method): T

    fun execute(sendSticker: SendSticker): Message
    fun execute(sendDocument: SendDocument): Message
    fun execute(sendPhoto: SendPhoto): Message

    suspend fun sendSticker(sticker: String, chatId: Long) {
        println("STICKER")
        val sendSticker = SendSticker()
        val str = org.telegram.telegrambots.meta.api.objects.InputFile()
        str.setMedia(sticker)
        sendSticker.chatId = chatId.toString()
        sendSticker.sticker = str
        try {
            execute(sendSticker)
        } catch (e: Exception) {
            e.printStackTrace()
            //Не слать пользователю ахтунг
            //sendAchtung(chatId)
        }
    }

    suspend fun editMessageText(
        text: String,
        messageId: Long,
        chatId: Long,
        shielded: Boolean,
        buttons: List<Pair<String, String>>? = null
    ) {
        println("EDITING")
        if (text.length > 4096) {
            val splitted = text.split("\n")

            var i = 0
            var reducedText = ""

            while (i in splitted.indices) {
                while (i in splitted.indices && (reducedText + splitted[i] + "\n").length <= 4096) {
                    reducedText += splitted[i] + "\n"
                    i++
                }

                editMessageText(reducedText, messageId, chatId, shielded)

                reducedText = ""
            }

            return
        }


        val editMessageText = EditMessageText()
        editMessageText.messageId = messageId.toInt()
        editMessageText.chatId = chatId.toString()
        editMessageText.text = if (shielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()
        editMessageText.parseMode = "MarkdownV2"

        if (buttons != null) {
            val inlineKeyboardMarkup = InlineKeyboardMarkup()
            inlineKeyboardMarkup.keyboard = makeKeyboard(buttons)
            editMessageText.replyMarkup = inlineKeyboardMarkup
        }

        try {
            execute(editMessageText)
        } catch (e: Exception) {
            println(e.message)
            //Не выводить ошибки при редактировании сообщения
            //e.printStackTrace()
            //Не слать пользователю ахтунг
            //sendAchtung(chatId)
        }
    }


    fun sendAchtung(chatId: Long) {
        GlobalScope.launch(Dispatchers.Default) {
            val sendMessage = SendMessage()
            sendMessage.chatId = chatId.toString()
            sendMessage.text = VpnBot.ACHTUNG_MESSAGE

            execute(sendMessage)
        }
    }

    suspend fun sendMessage(
        text: String,
        chatId: Long,
        shielded: Boolean = false,
        buttons: List<Pair<String, String>>? = null
    ): Int? {
        println("MESSAGE")
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

            return -1
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

        //GlobalScope.launch (Dispatchers.Default) {
        return GlobalScope.async {
            val id = try {
                execute(sendMessage).messageId
            } catch (e: Exception) {
                e.printStackTrace()
                sendAchtung(chatId)

                -1
            }

            return@async id
        }.await()
        //}
    }

    private fun makeKeyboard(buttonsInfo: List<Pair<String, String>>): List<List<InlineKeyboardButton>> {
        val keyboard = arrayListOf<List<InlineKeyboardButton>>()

        var twoList = arrayListOf<InlineKeyboardButton>()
        for ((name, code) in buttonsInfo) {
            val button = InlineKeyboardButton()
            button.text = name

            if (code.length >= 8 && code.substring(0, 8) == "https://") {
                button.url = code
            } else {
                button.callbackData = code
            }

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

    suspend fun sendPhoto(photo: String, chatId: Long) {
        // Закидывайте все отправляемые фото и документы в папку "photoAndDocs"
        println("PHOTO")
        val sendPhoto = SendPhoto()
        val t = InputFile()
        t.setMedia(File("photoAndDocs\\$photo"))
        sendPhoto.photo = t
        sendPhoto.chatId = chatId.toString()
        try {
            execute(sendPhoto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    suspend fun sendPhoto(photo: File, chatId: Long) {
        // Закидывайте все отправляемые фото и документы в папку "photoAndDocs"
        println("PHOTO")
        val sendPhoto = SendPhoto()
        val t = InputFile()
        t.setMedia(photo)
        sendPhoto.caption
        sendPhoto.photo = t
        sendPhoto.chatId = chatId.toString()
        try {
            execute(sendPhoto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendDocument(document: File, text: String, chatId: Long, markdownShielded: Boolean = true) {
        // Закидывайте все отправляемые фото и документы в папку "photoAndDocs"
        println("DOCUMENT")
        val sendDocument = SendDocument()
        sendDocument.chatId = chatId.toString()
        sendDocument.document = InputFile(document, document.name)
        sendDocument.caption =
            if (markdownShielded) text.telegramShielded().nonMarkdownShielded() else text.telegramShielded()

        sendDocument.parseMode = "MarkdownV2"

        execute(sendDocument)
    }

    suspend fun answerCallbackQuery(id: String) {
        val answerCallbackQuery = AnswerCallbackQuery()
        answerCallbackQuery.callbackQueryId = id

        execute(answerCallbackQuery)
    }
}