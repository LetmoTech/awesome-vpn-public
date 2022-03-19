package org.kamikadzy.awesomevpn.utils

import java.util.*

object CodeGenerator {
    fun generateRandomCode(): String {
        return UUID.randomUUID().toString()
    }
}