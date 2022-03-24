package org.kamikadzy.awesomevpn.utils

import java.util.*

@Suppress("UNCHECKED_CAST")
fun <T : Exception> kassert(b: Boolean, exception: T = Exception() as T) {
    if (!b) {
        throw exception
    }
}

fun <T> Optional<T>.unwrap(): T? = orElse(null)
