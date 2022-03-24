package org.kamikadzy.awesomevpn.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.max

fun BitcoinBigDecimal.formatGroups(): String {
    val formatter = NumberFormat.getInstance(Locale.US) as DecimalFormat
    val symbols = formatter.decimalFormatSymbols

    symbols.groupingSeparator = ' '
    formatter.decimalFormatSymbols = symbols
    return formatter.format(this)
}

fun Int.formatCode(): String {
    val codeLen = max(this.toString().length, 6)
    return "%0${codeLen}d".format(this)
}

fun String.telegramShielded(): String {
    return this.replace(".", """\.""")
        .replace(",", """\,""")
        .replace("!", """\!""")
        .replace("-", """\-""")
        .replace("_", """\_""")
        .replace("|", """\|""")
        .replace("+", """\+""")
        .replace("#", """\#""")
        .replace("=", """\=""")
}

fun String.nonMarkdownShielded(): String = this
    .replace("(", """\(""")
    .replace(")", """\)""")

private val HEX_CHARS = "0123456789ABCDEF"

fun String.hexStringToByteArray(): ByteArray {
    val result = ByteArray(length / 2)

    for (i in 0 until length step 2) {
        val firstIndex = HEX_CHARS.indexOf(this[i])
        val secondIndex = HEX_CHARS.indexOf(this[i + 1])

        val octet = firstIndex.shl(4).or(secondIndex)
        result.set(i.shr(1), octet.toByte())
    }

    return result
}

fun String.toBitcoinBigDecimal(): BitcoinBigDecimal {
    return BitcoinBigDecimal(this.toBigDecimal())
}