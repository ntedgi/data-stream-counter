package core.counters.impl.sticky.parallel

import core.counters.CMap
import core.counters.StreamCounter

class StreamLossyCountering<T> : StreamCounter<T> {
    override fun add(v: T, amount: Long) {
        TODO("Not yet implemented")
    }
    override fun complete(): CMap<T> {
        TODO("Not yet implemented")
    }
}
