package org.core.impl.lossy

import org.core.CMap
import org.core.StreamCounter
import org.utils.LogProvider
import org.utils.linfo
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue


class LossyCounter<T>(
    private val iterationSize: Int,
    private val minAmountPerIteration: Int,
    concurrency: Int = Runtime.getRuntime().availableProcessors() * 8
) : StreamCounter<T>, LogProvider() {
    private val gcCounter = AtomicInteger(0)
    private val currentStep = AtomicLong(0)
    private val counters = Array(concurrency) { CMap<T>() }
    override fun add(v: T, amount: Long) {
        val step = currentStep.incrementAndGet()
        if (step % iterationSize == 0L) {
            gcCounter.incrementAndGet()
            performGC((step / iterationSize).toInt() * minAmountPerIteration)
        }
        val mapIndex = v.hashCode().absoluteValue % counters.size
        val map = counters[mapIndex]
        synchronized(map) {
            map.addTo(v, amount)
        }
    }

    override fun complete(): CMap<T> {
        performGC((currentStep.get() / iterationSize).toInt() * minAmountPerIteration)
        val result = CMap<T>(counters.sumBy { it.size })
        counters.forEach { result.putAll(it); it.clear() }
        currentStep.set(0)

        return result
    }

    private fun performGC(min: Int) {
        linfo { "Thread - ${Thread.currentThread().id} started GC" }
        for (counter in counters) {
            val temp = CMap<T>(counter.size)
            synchronized(counter) {
                for ((k, v) in counter.object2LongEntrySet().fastIterator()) {
                    if (v >= min) temp[k] = v
                }
                counter.clear()
                counter.putAll(temp)
            }
        }
    }
}
