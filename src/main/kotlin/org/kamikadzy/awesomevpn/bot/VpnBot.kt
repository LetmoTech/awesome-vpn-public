package org.kamikadzy.awesomevpn.bot

import org.kamikadzy.awesomevpn.db.admin.Admin
import org.kamikadzy.awesomevpn.db.admin.AdminService
import org.kamikadzy.awesomevpn.db.user.DealSource
import org.kamikadzy.awesomevpn.db.user.User
import org.kamikadzy.awesomevpn.db.user.UserService
import org.kamikadzy.awesomevpn.utils.startSuspended
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update

@Component
class VpnBot(
    @Value("\${telegram.token}")
    val token: String,
    val userService: UserService,
    val adminService: AdminService
) : TelegramLongPollingBot(), Bot {
    companion object {
        const val ACHTUNG_MESSAGE = "Ошибка!"

        const val CHOOSE_BITCOIN_TEXT = "BTC"
        const val CHOOSE_BITCOIN = "CPM!BTC"
    }

    override fun getBotUsername(): String {
        return "Awesome VPN Bot"
    }

    override fun getBotToken() = token

    /*
     *
     * Запуск бота с функции ниже
     *
     */
    @Synchronized
    override fun onUpdateReceived(update: Update) {
        try {
            processUpdate(update)
        } catch (e: Exception) {
            println("Пропуск хода.")
        }
    }

    // Создание криптокошельков нового пользователя
    private fun newUserTokens(user: User) {
        user.tokens[DealSource.BTC] = "Находится в разработке."
        user.tokens[DealSource.ETH] = "Находится в разработке."
        user.tokens[DealSource.USDT] = "Находится в разработке."
        user.tokens[DealSource.TRON] = "Находится в разработке."
        user.tokens[DealSource.MONERO] = "Находится в разработке."
        userService.saveUser(user)
    }

    // Распределение юзеров/админов
    private fun processUpdate(update: Update) {
        val userName =
            if (update.hasCallbackQuery()) update.callbackQuery.from.userName else update.message.from.userName
        val userId = if (update.hasCallbackQuery()) update.callbackQuery.from.id else update.message.from.id
        val userChatId =
            if (update.hasCallbackQuery()) update.callbackQuery.message.chatId else update.message.chatId.toLong()

        var user = userService.getUserByTgId(userId)
        updateNewAdmin(user)
        val admin = adminService.getAdminById(userId)

        if (admin != null && !admin.asUser) {
            startSuspended { processAdminUpdate(update, admin) }
        } else if (user == null) {
            user = userService.saveUser(
                User(
                    name = userName,
                    chatId = userChatId,
                    tgId = userId
                )
            )
            startSuspended {
                sendMessage(
                    "Создаем Вам криптокошельки.\n" +
                            "Это происходит при регистрации нового пользователя и может занять некоторое время.\n",
                    user.chatId,
                    false
                )
                newUserTokens(user)
                sendMessage(
                    "Создание криптокошельков завершено.\n" +
                            "Добро пожаловать!", user.chatId, false
                )
                processUserUpdate(update, user)
            }
        } else {
            user.name = userName
            userService.saveUser(user)
            if (user.isBan) return
            startSuspended { processUserUpdate(update, user) }
        }
    }

    //Фиксируем id нового админа
    private fun updateNewAdmin(user: User?) {
        if (user == null || user.name == null) return // эту строку не трогать, я сам не уверен как она работает...
        val mbAdmin = adminService.getAdminByName(user.name!!)
        if (mbAdmin != null && mbAdmin.tgId == (-1).toLong() && mbAdmin.chatId == (-1).toLong()) {
            // Сообщаем о регистрации нового админа существующим админам
            val admins = adminService.getAllAdmins()
            for (i in 0 until admins.size) {
                if (admins[i].chatId != (-1).toLong()) {
                    startSuspended { sendMessage("Registered new admin: " + mbAdmin.name, admins[i].chatId, false) }
                }
            }
            // Сохраняем нового админа в базе
            mbAdmin.tgId = user.tgId
            mbAdmin.chatId = user.chatId
            adminService.saveAdmin(mbAdmin)
        }
    }

    /*
     * Команды
     * админов
     */
    suspend fun processAdminUpdate(update: Update, admin: Admin) {
        if (update.hasMessage()) {
            val text = update.message
            val splittedMessage = update.message.text.split(" ")
            when (splittedMessage[0]) {
                "/start" -> {
                    sendSticker("CAACAgIAAxkBAAEEPlFiOmXSUUeV3-o1Na7NngNZ3KeRhwACxwAD-0HCDoWNjoqE3wv6IwQ", admin.chatId)
                    sendMessage(
                        "Рады тебя видеть, ${admin.name}!", admin.chatId, false, listOf(
                            "Список команд" to "commands"
                        )
                    )
                }
                "/asUser" -> {
                    admin.asUser = true
                    adminService.saveAdmin(admin)
                    sendSticker(
                        "CAACAgIAAxkBAAEEPk1iOmRR9bwTKMY71SaVKnQuUocqVQACEBUAAjFLyEtoCinYPeRu6CME",
                        admin.chatId
                    )
                    return
                }
                "/view" -> {
                    if (splittedMessage.size != 2) {
                        sendMessage("Usage: view 'admins|users'", admin.chatId, false)
                        return
                    }
                    if (splittedMessage[1] in listOf("admin", "admins", "adm", "ad")) {
                        val admins = adminService.getAllAdmins()
                        var adminsOut: String = "Admins:\n"
                        for (i in 0 until admins.size) {
                            adminsOut += "`" + admins[i].name + "` `" + admins[i].tgId + "`\n"
                        }
                        sendMessage(adminsOut, admin.chatId, false)
                    } else if (splittedMessage[1] in listOf("user", "users", "usr", "us")) {
                        val users = userService.getAllUsers()
                        var usersOut: String = "Users:\n"
                        for (i in 0 until users.size) {
                            usersOut += "`" + users[i].name + "` `" + users[i].tgId + "`\n"
                        }
                        sendMessage(usersOut, admin.chatId, false)
                    } else {
                        sendMessage("Usage: view 'admins|users'", admin.chatId, false)
                    }
                }
                "/add" -> {
                    if (splittedMessage.size != 2) {
                        sendMessage("Usage: add 'NAME_ADMIN'\nTry again", admin.chatId, false)
                        return
                    }
                    if (adminService.getAdminByName(splittedMessage[1]) == null) {
                        adminService.saveAdmin(Admin(name = splittedMessage[1], -1, -1))
                        sendMessage("Admin `${splittedMessage[1]}` added.", admin.chatId, false)
                    } else {
                        sendMessage("Admin with name `${splittedMessage[1]}` already added.", admin.chatId, false)
                    }
                }
                "/ban" -> {
                    if (splittedMessage.size != 2) {
                        sendMessage("Usage: ban 'ID_USER'", admin.chatId, false)
                        return
                    }
                    val banUser = userService.getUserByTgId(splittedMessage[1].toLong())
                    if (banUser != null) {
                        banUser.isBan = true
                        sendMessage(
                            "User `${banUser.name}` with id `${banUser.tgId}` has been baned.",
                            admin.chatId,
                            false
                        )
                        userService.saveUser(banUser)
                    } else {
                        sendMessage("User with id `${splittedMessage[1]}` not found.", admin.chatId, false)
                    }
                }
                "/unban" -> {
                    if (splittedMessage.size != 2) {
                        sendMessage("Usage: unban 'ID_USER'", admin.chatId, false)
                        return
                    }
                    val unbanUser = userService.getUserByTgId(splittedMessage[1].toLong())
                    if (unbanUser != null) {
                        unbanUser.isBan = false
                        sendMessage(
                            "User `${unbanUser.name}` with id `${unbanUser.tgId}` has been unbaned.",
                            admin.chatId,
                            false
                        )
                        userService.saveUser(unbanUser)
                    } else {
                        sendMessage("User with id `${splittedMessage[1]}` not found.", admin.chatId, false)
                    }
                }
                "/remove" -> {
                    if (splittedMessage.size != 2) {
                        sendMessage("Usage: remove 'NAME_ADMIN'\nTry again", admin.chatId, false)
                        return
                    }
                    if (splittedMessage[1] == "kuratz") {
                        sendMessage("Are you serious?", admin.chatId, false)
                        return
                    }
                    val remAdmin = adminService.getAdminByName(splittedMessage[1])
                    if (remAdmin != null) {
                        sendMessage("Removed admin: `${remAdmin.name}`", admin.chatId, false)
                        if (remAdmin.chatId != (-1).toLong())
                            sendMessage("You've been removed from admins.", remAdmin.chatId, false)
                        adminService.removeAdmin(remAdmin)
                    } else {
                        sendMessage("Admin with name `${splittedMessage[1]}` not found.", admin.chatId, false)
                    }
                }
                else -> sendAchtung(admin.chatId)
            }
        } else if (update.hasCallbackQuery()) {
            answerCallbackQuery(update.callbackQuery.id)
            val callbackData = update.callbackQuery.data
            when (callbackData) {
                "commands" -> {
                    sendMessage(
                        "/view 'admins|users' - просмотр ников и tgId админов или юзеров'\n" +
                                "/add 'NAME_ADMIN' - добавить админа по имени'\n" +
                                "/remove 'NAME_ADMIN' - удалить админа по имени'\n" +
                                "/ban 'ID_USER' - забанить юзера по tgId\n" +
                                "/unban 'ID_USER' - разбанить юзера по tgId\n" +
                                "/asUser - войти в систему как пользователь\n" +
                                "/asAdminCum - войти в систему как админ, если в системе уже как пользователь\n",
                        admin.chatId, false, listOf(
                            "Назад" to "start"
                        )
                    )
                }
                "start" -> {
                    sendMessage(
                        "Доброго времени суток, сударь ${admin.name}!", admin.chatId, false, listOf(
                            "Список команд" to "commands"
                        )
                    )
                }
                else -> sendAchtung(admin.chatId)
            }
            println(callbackData)
        }
    }

    /*
     * Команды обычных
     * пользователей
     */
    suspend fun processUserUpdate(update: Update, user: User) {
        if (update.hasMessage()) {
            val text = update.message
            val splittedMessage = update.message.text.split(" ")

            when (splittedMessage[0]) {
                "/start" -> {
                    val rr = sendMessage(
                        "Здравствуйте" + if (user.name != null) ", " + user.name + "." else "!" + "\n",
                        user.chatId,
                        false,
                        listOf(
                            "Баланс" to "start!balance",
                            "Мои криптокошельки" to "start!mycryptowallets",
                            "Пополнить баланс" to "start!paybalance",
                            "Как подключить VPN" to "start!tutorial!1"
                        )
                    )
                    user.lastMessageType = "start"
                    if (rr != null) user.lastMessageId = rr.toLong()
                    userService.saveUser(user)
                }
                "/asAdminCum" -> {
                    val checkAdmin = adminService.getAdminById(user.tgId)
                    if (checkAdmin != null) {
                        checkAdmin.asUser = false
                        adminService.saveAdmin(checkAdmin)
                        sendSticker(
                            "CAACAgIAAxkBAAEEPk9iOmRWF-V3YtvLNPdD7pi7bqV47AACWRIAAiyS0Ev9JSCrjYWUiCME",
                            user.chatId
                        )
                    }
                    return
                }
                else -> sendAchtung(user.chatId)
            }
        } else if (update.hasCallbackQuery()) {
            answerCallbackQuery(update.callbackQuery.id)
            val callbackData = update.callbackQuery.data
            val splittedCallBack = callbackData.split("!")
            if (splittedCallBack.size == 1) {
                sendAchtung(user.chatId)
                return
            }
            when (splittedCallBack[1]) {
                "paybalance" -> {
                    editMessageText(
                        "Вы можете пополнить баланс с карты, нажав на кнопку ниже.",
                        user.lastMessageId, user.chatId, false, listOf(
                            "Пополнить баланс" to "https://yoomoney.ru/",
                            "Назад" to "paybalance!start"
                        )
                    )
                }
                "balance" -> {
                    editMessageText(
                        "Ваш баланс составляет: `${user.balance}`₽", user.lastMessageId, user.chatId, false, listOf(
                            "Назад" to "balance!start",
                        )
                    )
                }
                "mycryptowallets" -> {
                    editMessageText(
                        "Ваши криптокошельки:\n\n" +
                                "BTC:   `${user.tokens[DealSource.BTC]}`\n\n" +
                                "ETH:   `${user.tokens[DealSource.ETH]}`\n\n" +
                                "USDT:   `${user.tokens[DealSource.USDT]}`\n\n" +
                                "TRON:   `${user.tokens[DealSource.TRON]}`\n\n" +
                                "MONERO:   `${user.tokens[DealSource.MONERO]}`\n",
                        user.lastMessageId,
                        user.chatId,
                        false,
                        listOf(
                            "Назад" to "mycryptowallets!start",
                        )
                    )
                }
                "tutorial" -> {
                    val page = splittedCallBack[2].toInt()
                    val endTutorial = 3
                    when (page) {
                        endTutorial -> {
                            editMessageText(
                                "${page}/${endTutorial}\n" +
                                        "Для активации Вашего VPN" +
                                        " нажмите на флажок в приложении и сверните приложение " +
                                        "(главное не закрывать его полностью).\n\n" +
                                        "Вот и всё! Ваш личный VPN настроен, " +
                                        "Вы можете пользоваться любыми приложениями и не волноваться о Вашей безопасности.",
                                user.lastMessageId, user.chatId, true, listOf(
                                    "Назад" to "tutorial!tutorial!${page - 1}",
                                    "В меню" to "tutorial!start"
                                )
                            )
                        }
                        1 -> {
                            editMessageText(
                                "${page}/${endTutorial}\n" +
                                        "Скачайте приложение \'WireGuard\'.\n" +
                                        "Его можно скачать, перейдя по кнопкам ниже.\n" +
                                        "App Store - для iPhone.\n" +
                                        "Play Маркет - для Android.\n" +
                                        "И для других платформ(Windows, macOS, Linux).",
                                user.lastMessageId, user.chatId, true, listOf(
                                    "Вперед" to "tutorial!tutorial!${page + 1}",
                                    "В меню" to "tutorial!start",
                                    "Play Маркет" to "https://play.google.com/store/apps/details?id=com.wireguard.android",
                                    "App Store" to "https://apps.apple.com/us/app/wireguard/id1441195209",
                                    "Для других платформ" to "https://www.wireguard.com/install/"
                                )
                            )
                        }
                        2 -> {
                            editMessageText(
                                "${page}/${endTutorial}\n" +
                                        "При регистрации вы получили уникальные zip файл и QR-код,\n" +
                                        "они необходимы Вам для настройки VPN.\n" +
                                        "Настройка происходит единожды.\n\n" +
                                        "Следуйте шагам, показанным на фотографиях выше:\n" +
                                        "1. Войдите в приложение WireGuard.\n" +
                                        "2. Нажмите на плюсик.\n" +
                                        "3. Выберите удобный Вам способ настройки\n" +
                                        "   ('С помощью файла или архива' - используйте zip файл, выданный Вам ранее)\n" +
                                        "   ('C помощью QR-кода' - наведите камеру на QR-код, выданный Вам ранее).\n" +
                                        "4. Назовите VPN как Вам угодно.\n" +
                                        "5. Если в приложении появилась строка с выбором VPN, значит настройка прошла успешно!\n" +
                                        "Переходите на следующий шаг настройки.",
                                user.lastMessageId, user.chatId, true, listOf(
                                    "Назад" to "tutorial!tutorial!${page - 1}",
                                    "Вперед" to "tutorial!tutorial!${page + 1}",
                                    "В меню" to "tutorial!start"
                                )
                            )
                        }
                        else -> {
                            editMessageText(
                                "${page}/${endTutorial}\n",
                                user.lastMessageId, user.chatId, false, listOf(
                                    "Назад" to "tutorial!tutorial!${page - 1}",
                                    "Вперед" to "tutorial!tutorial!${page + 1}",
                                    "В меню" to "tutorial!start"
                                )
                            )
                        }
                    }
                }
                "start" -> {
                    editMessageText(
                        "Здравствуйте" + if (user.name != null) ", " + user.name + "." else "!" + "\n",
                        user.lastMessageId,
                        user.chatId,
                        false,
                        listOf(
                            "Баланс" to "start!balance",
                            "Мои криптокошельки" to "start!mycryptowallets",
                            "Пополнить баланс" to "start!paybalance",
                            "Как подключить VPN" to "start!tutorial!1"
                        )
                    )
                }
            }
            println(callbackData)
        }

    }

    override fun <T : java.io.Serializable, Method : BotApiMethod<T>> execute(method: Method): T = super.execute(method)
}