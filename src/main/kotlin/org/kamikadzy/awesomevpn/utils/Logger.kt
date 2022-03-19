package org.kamikadzy.awesomevpn.utils

import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


object Logger {
    private val file = File("log.txt")
    private val hgBaseLog = File("hgbaselog.txt")
    private val panelsLog = File("panelslog.txt")
    private val dealLogFile = File("deals.txt")
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")

    init {
        if (!file.exists()) {
            file.createNewFile()
        }

        if (!dealLogFile.exists()) {
            dealLogFile.createNewFile()
        }

        if (!hgBaseLog.exists()) {
            hgBaseLog.createNewFile()
        }

        if (!panelsLog.exists()) {
            panelsLog.createNewFile()
        }
    }

    fun addEntity(entity: String) {
        try {
            file.appendText(
                "${getDate()}\t${entity}\n"
            )
        } catch (_: Exception) {}
    }

    fun addDealEvent(entity: String) {
        try {
            file.appendText(
                "${getDate()}\t${entity}\n"
            )
        } catch (_: Exception) {}
    }

    fun addHgBaseEvent(entity: String) {
        try {
            hgBaseLog.appendText(
                "${getDate()}\t${entity}\n"
            )
            panelsLog.appendText(
                "${getDate()}\t${entity}\n"
            )
        } catch (_: Exception) {}
    }

    fun addPanelsEvent(entity: String) {
        try {
            panelsLog.appendText(
                "${getDate()}\t${entity}\n"
            )
        } catch (_: Exception) {}
    }

    private fun getDate(): String {
        val date = Date.from(Instant.now())
        date.time += 1 * 60 * 60 * 1000

        return dateFormatter.format(date)
    }
}