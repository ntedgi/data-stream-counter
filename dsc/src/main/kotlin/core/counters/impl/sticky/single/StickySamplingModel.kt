package core.counters.impl.sticky.single

import kotlin.math.ln
import kotlin.random.Random

interface FrequencyCount<T> {
    fun process(dataStream: List<T>): FrequencyCount<T>
    fun computeOutput(): Array<Pair<T, Int>>
}

/**
 *
 * @param frequency Frequency above which we want to print out frequent items
 * @param error     output = f*N - e*N, where N is the total number of elements
 * @tparam T The type of item
 */
class StickySamplingModel<T>(
    private val frequency: Double, private val error: Double, probabilityOfFailure: Double
) : FrequencyCount<T> {
    /**
     * The first t elements are sampled at rate r=1, the next 2t are sampled at rate r=2, the next 4t at r=4 and so on
     */
    private val t: Double = (1.0 / error) * ln(1.0 / (frequency * probabilityOfFailure))
    private val initialSamplingRate = 1
    private val map = HashMap<T, Int>()
    private var rng = Random
    private var samplingRate = initialSamplingRate
    private var totalProcessedElements = 0L
    override fun process(dataStream: List<T>): StickySamplingModel<T> {
        dataStream.forEach { item ->
            totalProcessedElements += 1


            val currSamplingRate = deriveSamplingRate(
                totalProcessedElements, t
            )

            updateItemWithSampling(item, currSamplingRate)

            if (samplingRateHasChanged(samplingRate, currSamplingRate)) {
                decreaseAllEntriesByCoinToss()
            }

            samplingRate = currSamplingRate
        }
        return this
    }

    private fun updateItemWithSampling(item: T, samplingRate: Int) {
        if (map.contains(item)) map[item] = map[item]!! + 1
        else if (canSelectItWithSamplingRate(samplingRate)) map[item] = 1
    }

    private fun canSelectItWithSamplingRate(samplingRate: Int): Boolean = rng.nextDouble() < (1.0 / samplingRate)
    private fun decreaseAllEntriesByCoinToss() = map.forEach { item ->
        var currCount = item.value
        while (currCount > 0 && unsuccessfulCoinToss()) currCount -= 1
        if (currCount > 0) map[item.key] = currCount
        else map.remove(item.key)
    }

    private fun unsuccessfulCoinToss(): Boolean = rng.nextDouble() > 0.5
    private fun samplingRateHasChanged(prevRate: Int, currRate: Int): Boolean = currRate > prevRate
    override fun computeOutput(): Array<Pair<T, Int>> {
        return map.filter { itemWithFreq ->
            itemWithFreq.value.toDouble() >= (frequency * totalProcessedElements - error * totalProcessedElements)
        }.toList().sortedBy { it.second }.toTypedArray()
    }
}

/**
 * Returns the rate that corresponds to this item based on the number of items that have been processed so far
 *
 * @param itemSeqId A sequential id (past items + 1)
 * @param t
 * @return
 */
fun deriveSamplingRate(itemSeqId: Long, t: Double): Int {
    var currentSamplingRate = 1
    var sum = 0.0
    while (currentSamplingRate * t + sum < itemSeqId) {
        sum += currentSamplingRate * t
        currentSamplingRate *= 2
    }
    return currentSamplingRate
}