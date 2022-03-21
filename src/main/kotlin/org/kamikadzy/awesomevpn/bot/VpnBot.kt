package org.kamikadzy.awesomevpn.bot

import org.kamikadzy.awesomevpn.db.user.User
import org.kamikadzy.awesomevpn.db.admin.Admin
import org.kamikadzy.awesomevpn.db.user.UserService
import org.kamikadzy.awesomevpn.db.admin.AdminService
import org.kamikadzy.awesomevpn.db.user.DealSource
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
): TelegramLongPollingBot(), Bot {
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
            e.printStackTrace()
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
        val userName = if (update.hasCallbackQuery()) update.callbackQuery.from.userName else update.message.from.userName
        val userId = if (update.hasCallbackQuery()) update.callbackQuery.from.id else update.message.from.id
        val userChatId = if (update.hasCallbackQuery()) update.callbackQuery.message.chatId else update.message.chatId.toLong()

        var user = userService.getUserById(userId)
        updateNewAdmin(user)
        val admin = adminService.getAdminById(userId)

        if(admin != null && admin.asUser == false) {
            processAdminUpdate(update, admin)
        } else if (user == null) {
            user = userService.saveUser(
                    User(
                            name = userName,
                            chatId = userChatId,
                            tgId = userId
                    )
            )
            sendMessage("Создаем вам криптокошельки.\n" +
                    "Это происходит при регистрации нового пользователя и может занять некоторое время.\n", user.chatId, false)
            newUserTokens(user)
            sendMessage("Создание криптокошельков завершено.\n" +
                    "Добро пожаловать!", user.chatId, false)
            processUserUpdate(update, user)
        } else {
            user.name = userName
            userService.saveUser(user)
            if(user.ban) return
            processUserUpdate(update, user)
        }
    }

    //Фиксируем id нового админа
    private fun updateNewAdmin(user: User?) {
        if(user == null || user.name == null) return
        val mbAdmin = adminService.getAdminByName(user.name!!)
        if(mbAdmin != null && mbAdmin.tgId == (-1).toLong() && mbAdmin.chatId == (-1).toLong()) {
            // Сообщаем о регистрации нового админа существующим админам
            val admins = adminService.getAllAdmins()
            for (i in 0 until admins.size) {
                if(admins[i].chatId != (-1).toLong()) {
                    sendMessage("Registered new admin: " + mbAdmin.name, admins[i].chatId, false)
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
    fun processAdminUpdate(update: Update, admin: Admin) {
        if (update.hasMessage()) {
            val text = update.message
            val splittedMessage = update.message.text.split(" ")
            when (splittedMessage[0]) {
                "/start" -> {
                    sendMessage("Доброго времени суток, сударь ${admin.name}!", admin.chatId, false, listOf(
                            "Список команд" to "commands"
                    ))
                }
                "/asUser" -> {
                    admin.asUser = true
                    adminService.saveAdmin(admin)
                    return
                }
                "/view" -> {
                    if(splittedMessage.size == 1) {
                        sendMessage("Usage: view 'admins|users'", admin.chatId, false)
                        return
                    }
                    if(splittedMessage[1] in listOf("admin", "admins", "adm", "ad")) {
                        val admins = adminService.getAllAdmins()
                        var adminsOut: String = "Admins:\n"
                        for (i in 0 until admins.size) {
                            adminsOut += "`" + admins[i].name + "` `" + admins[i].tgId + "`\n"
                        }
                        sendMessage(adminsOut, admin.chatId, false)
                    } else if(splittedMessage[1] in listOf("user", "users", "usr", "us")) {
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
                    if(splittedMessage.size == 1) {
                        sendMessage("Usage: add 'NAME_ADMIN'\nTry again", admin.chatId, false)
                        return
                    }
                    if(adminService.getAdminByName(splittedMessage[1]) == null) {
                        adminService.saveAdmin(Admin(name = splittedMessage[1], -1, -1))
                        sendMessage("Admin `${splittedMessage[1]}` added.", admin.chatId, false)
                    } else {
                        sendMessage("Admin with name `${splittedMessage[1]}` already added.", admin.chatId, false)
                    }
                }
                "/ban" -> {
                    if(splittedMessage.size == 1) {
                        sendMessage("Usage: ban 'ID_USER'", admin.chatId, false)
                        return
                    }
                    val banUser = userService.getUserById(splittedMessage[1].toLong())
                    if(banUser != null) {
                        banUser.ban = true
                        sendMessage("User `${banUser.name}` with id `${banUser.tgId}` has been baned.", admin.chatId, false)
                        userService.saveUser(banUser)
                    } else {
                        sendMessage("User with id `${splittedMessage[1]}` not found.", admin.chatId, false)
                    }
                }
                "/unban" -> {
                    if(splittedMessage.size == 1) {
                        sendMessage("Usage: unban 'ID_USER'", admin.chatId, false)
                        return
                    }
                    val unbanUser = userService.getUserById(splittedMessage[1].toLong())
                    if(unbanUser != null) {
                        unbanUser.ban = false
                        sendMessage("User `${unbanUser.name}` with id `${unbanUser.tgId}` has been unbaned.", admin.chatId, false)
                        userService.saveUser(unbanUser)
                    } else {
                        sendMessage("User with id `${splittedMessage[1]}` not found.", admin.chatId, false)
                    }
                }
                "/remove" -> {
                    if(splittedMessage.size == 1) {
                        sendMessage("Usage: remove 'NAME_ADMIN'\nTry again", admin.chatId, false)
                        return
                    }
                    if(splittedMessage[1] == "kuratz") {
                        sendMessage("Are you serious?", admin.chatId, false)
                        return
                    }
                    val remAdmin = adminService.getAdminByName(splittedMessage[1])
                    if (remAdmin != null) {
                        sendMessage("Removed admin: `${remAdmin.name}`", admin.chatId, false)
                        if(remAdmin.chatId != (-1).toLong())
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
            when(callbackData) {
                "commands" -> {
                    sendMessage("/view 'admins|users' - просмотр ников и tgId админов или юзеров'\n" +
                            "/add 'NAME_ADMIN' - добавить админа по имени'\n" +
                            "/remove 'NAME_ADMIN' - удалить админа по имени'\n" +
                            "/ban 'ID_USER' - забанить юзера по tgId\n" +
                            "/unban 'ID_USER' - разбанить юзера по tgId\n" +
                            "/asUser - войти в систему как пользователь\n" +
                            "/asAdminCum - войти в систему как админ, если в системе уже как пользователь\n",
                    admin.chatId, false, listOf(
                            "Назад" to "start"
                    ))
                }
                "start" -> {
                    sendMessage("Доброго времени суток, сударь ${admin.name}!", admin.chatId, false, listOf(
                            "Список команд" to "commands"
                    ))
                }
                else -> sendAchtung(admin.chatId)
            }
            println(callbackData)
        } else {
            sendAchtung(admin.chatId)
        }
    }

    /*
     * Команды обычных
     * пользователей
     */
    fun processUserUpdate(update: Update, user: User) {
        if (update.hasMessage()) {
            val text = update.message
            val splittedMessage = update.message.text.split(" ")

            when (splittedMessage[0]) {
                "/start" -> {
                    val rr = sendMessage("Здравствуйте" + if(user.name != null) ", " + user.name + "." else "!" + "\n"
                            , user.chatId, false, listOf(
                            "Баланс" to "start!balance",
                            "Мои криптокошельки" to "start!mycryptowallets"
                    ))
                    user.lastMessageType = 1
                    if(rr != null) user.lastMessageId = rr.toLong()
                    userService.saveUser(user)
                }
                "/asAdminCum" -> {
                    val checkAdmin = adminService.getAdminById(user.tgId)
                    if(checkAdmin != null) {
                        checkAdmin.asUser = false
                        adminService.saveAdmin(checkAdmin)
                    }
                    return
                }
                else -> sendAchtung(user.chatId)
            }
        } else if (update.hasCallbackQuery()) {
            answerCallbackQuery(update.callbackQuery.id)
            val callbackData = update.callbackQuery.data
            val splittedCallBack = callbackData.split("!")
            if(splittedCallBack.size == 1){
                sendAchtung(user.chatId)
                return
            }
            when(splittedCallBack[1]) {
                "balance" -> {
                    editMessageText("Ваш баланс составляет: `${user.balance}`₽", user.lastMessageId, user.chatId, false, listOf(
                            "Назад" to "balance!start",
                    ))
                }
                "mycryptowallets" -> {

                    editMessageText("Ваши криптокошельки:\n\n" +
                            "BTC:   `${user.tokens[DealSource.BTC]}`\n\n" +
                            "ETH:   `${user.tokens[DealSource.ETH]}`\n\n" +
                            "USDT:   `${user.tokens[DealSource.USDT]}`\n\n" +
                            "TRON:   `${user.tokens[DealSource.TRON]}`\n\n" +
                            "MONERO:   `${user.tokens[DealSource.MONERO]}`\n"
                            , user.lastMessageId, user.chatId, false, listOf(
                            "Назад" to "mycryptowallets!start",
                    ))
                }
                "start" -> {
                    editMessageText("Здравствуйте" + if(user.name != null) ", " + user.name + "." else "!" + "\n"
                            , user.lastMessageId, user.chatId, false, listOf(
                            "Баланс" to "start!balance",
                            "Мои криптокошельки" to "start!mycryptowallets"
                    ))
                }

                else -> sendAchtung(user.chatId)
            }
            println(callbackData)
        } else {
            sendAchtung(user.chatId)
        }

    }
    override fun <T: java.io.Serializable, Method : BotApiMethod<T>> execute(method: Method): T = super.execute(method)
}