package core.counters

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

class FrequencyCounts<T> {
    val data: Object2IntOpenHashMap<T> = Object2IntOpenHashMap<T>()
    init {
        data.defaultReturnValue(0)
    }
    fun inc(element: T) {
        data.addTo(element, 1)
    }

    fun dec(element: T) {
        val count = data.getInt(element)
        if (count - 1 == 0) {
            data.removeInt(element)
        } else data.replace(element, count - 1)
    }
}

interface Counter<T> {
    fun count(window: List<T>): Counter<T>
    fun computeFrequency(): FrequencyCounts<T>
}