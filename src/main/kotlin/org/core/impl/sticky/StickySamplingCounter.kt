package org.core.impl.sticky

import org.core.CMap
import org.core.StreamCounter
import org.utils.LogProvider
import org.utils.linfo
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue
import kotlin.math.ln
import kotlin.random.Random

class StickySamplingCounter<T>(
    frequency: Double,
    error: Double,
    probabilityOfFailure: Double,
    concurrency: Int = Runtime.getRuntime().availableProcessors() * 8
) : StreamCounter<T>, LogProvider() {
    private val t: Double = (1.0 / error) * ln(1.0 / (frequency * probabilityOfFailure))
    private val initialSamplingRate = 1
    private val counters = Array(concurrency) { CMap<T>() }
    private var rng = Random
    private var samplingRate = initialSamplingRate
    private var totalProcessedElements = AtomicLong(0)
    override fun add(v: T, amount: Long) {
        val currentItem = totalProcessedElements.incrementAndGet()
        var currentSamplingRate = samplingRate

        if (currentItem > t) synchronized(samplingRate) {
            currentSamplingRate = deriveSamplingRate(currentItem, t)
        }
        val mapIndex = v.hashCode().absoluteValue % counters.size
        val map = counters[mapIndex]

        synchronized(map) {
            when {
                map.contains(v) -> {
                    map.addTo(v, amount)
                }
                canSelectItWithSamplingRate(samplingRate) -> {
                    map[v] = 1
                }
                else -> {
                }
            }
        }

        if (samplingRateHasChanged(samplingRate, currentSamplingRate)) {
            samplingRate = currentSamplingRate
            performGC()
        }
    }

    private fun deriveSamplingRate(itemSeqId: Long, t: Double): Int {
        var currentSamplingRate = 1
        var sum = 0.0
        while (currentSamplingRate * t + sum < itemSeqId) {
            sum += currentSamplingRate * t
            currentSamplingRate *= 2
        }
        return currentSamplingRate
    }

    override fun complete(): CMap<T> {
        val result = CMap<T>(counters.sumBy { it.size })
        counters.forEach { result.putAll(it); it.clear() }
        totalProcessedElements.set(0)
        return result
    }

    private fun canSelectItWithSamplingRate(samplingRate: Int): Boolean = rng.nextDouble() < (1.0 / samplingRate)
    private fun performGC() {
        linfo { "Thread - ${Thread.currentThread().id} started GC" }
        for (counter in counters) {
            val temp = CMap<T>(counter.size)
            synchronized(counter) {
                for ((k, v) in counter.object2LongEntrySet().fastIterator()) {
                    var currCount = v
                    while (currCount > 0 && unsuccessfulCoinToss()) currCount -= 1
                    if (v >= 0) temp[k] = currCount
                }
                counter.clear()
                counter.putAll(temp)
            }
        }
    }

    private fun unsuccessfulCoinToss(): Boolean = rng.nextDouble() > 0.5
    private fun samplingRateHasChanged(prevRate: Int, currRate: Int): Boolean = currRate > prevRate
}