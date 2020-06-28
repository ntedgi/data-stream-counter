package core.counters.impl.sticky.parallel

import core.counters.CMap
import core.counters.StreamCounter
import core.counters.impl.sticky.single.deriveSamplingRate
import utils.LogProvider
import utils.linfo
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue
import kotlin.math.ln
import kotlin.random.Random

class StreamStickySamplingModel<T>(
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
// Frequency: 0.01, Error: 0.0019 Probability of failure: 1.9E-4
// (Brown, 11950)
// (Blue, 28073)
// (Yellow, 146250)
// (Green, 312476)
// (Red, 363885)
// Frequency: 0.01, Error: 0.0019 Probability of failure: 1.9E-4
// (Brown, 11956)
// (Blue, 28074)
// (Yellow, 146248)
// (Green, 312479)
// (Red, 363892)