package org.kamikadzy.awesomevpn.utils

fun kassert(b: Boolean) {
    if (!b) { throw Exception() }
}