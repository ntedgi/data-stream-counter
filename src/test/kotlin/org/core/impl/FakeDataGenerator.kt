package org.core.impl

import java.util.stream.Stream

enum class Item { Red, Blue, Yellow, Brown, Green }
object FakeDataGenerator {
    private fun create(count: Long, type: Item): List<Item> {
        return Array(count.toInt()) { type }.toList()
    }

    fun createFakeStream(config: Map<Item, Long>): Stream<Item> {
        val list = ArrayList<Item>()
        config.forEach { (t, u) ->
            list.addAll(create(u, t))
        }
        list.shuffle()
        return list.parallelStream()

    }
}
