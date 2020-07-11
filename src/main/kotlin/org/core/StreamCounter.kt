package org.core

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap

typealias CMap<T> = Object2LongOpenHashMap<T>

interface StreamCounter<T> {
    fun add(v: T, amount: Long = 1L)
    fun complete(): CMap<T>
}
