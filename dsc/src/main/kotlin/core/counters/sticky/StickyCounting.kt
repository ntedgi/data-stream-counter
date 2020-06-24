package core.counters.sticky

import kotlin.random.Random

interface FrequencyCount<T> {
    fun process(dataWindow: List<T>): FrequencyCount<T>
    fun computeOutput(): Array<Pair<T, Int>>
}

object SamplingRateRetriever {
    /**
     * Returns the rate that corresponds to this item based on the number of items that have been processed so far
     *
     * @param itemSeqId A sequential id (past items + 1)
     * @param t
     * @return
     */
    fun deriveSamplingRate(itemSeqId: Long, t: Double): Int {
        var currRate = 1
        var sum = 0.0
        while ((currRate * t + sum) < itemSeqId) {
            sum += (currRate * t)
            currRate *= 2
        }
        return currRate
    }
}

/**
 *
 * @param frequency Frequency above which we want to print out frequent items
 * @param error     output = f*N - e*N, where N is the total number of elements
 * @tparam T The type of item
 */
class StickySamplingModel<T>(
    val frequency: Double, val error: Double, val probabilityOfFailure: Double
) : FrequencyCount<T> {
    /**
     * The first t elements are sampled at rate r=1, the next 2t are sampled at rate r=2, the next 4t at r=4 and so on
     */
    val t = (1.0 / error) * Math.log(1.0 / (frequency * probabilityOfFailure))
    val INITIAL_SAMPLING_RATE = 1
    private val map = HashMap<T, Int>()
    var rng = Random(137)
    var samplingRate = INITIAL_SAMPLING_RATE
    private var totalProcessedElements = 0L
    fun withRng(randomNumberGenerator: Random): StickySamplingModel<T> {
        this.rng = Random
        return this
    }

    override fun process(dataStream: List<T>): StickySamplingModel<T> {
        dataStream.forEach { item ->
            totalProcessedElements += 1
            val currSamplingRate = SamplingRateRetriever.deriveSamplingRate(totalProcessedElements, t)
            updateItemWithSampling(item, currSamplingRate)
            if (samplingRateHasChanged(samplingRate, currSamplingRate)) {
                decreaseAllEntriesByCoinToss(currSamplingRate)
            }
            samplingRate = currSamplingRate
        }
        return this
    }

    private fun updateItemWithSampling(item: T, samplingRate: Int) {
        if (map.contains(item)) {
            val count = map[item]!!
            map[item] = count + 1
        } else if (canSelectItWithSamplingRate(samplingRate)) {
            map[item] = 1
        }
    }

    private fun canSelectItWithSamplingRate(samplingRate: Int): Boolean = rng.nextDouble() < (1.0 / samplingRate)
    private fun decreaseAllEntriesByCoinToss(samplingRate: Int) {
        map.forEach { item ->
            var currCount = item.value
            while (currCount > 0 && unsuccessfulCoinToss()) {
                currCount -= 1
            }
            if (currCount > 0) map[item.key] = currCount
            else map.remove(item.key)
        }
    }

    private fun unsuccessfulCoinToss(): Boolean = rng.nextDouble() > 0.5
    private fun samplingRateHasChanged(prevRate: Int, currRate: Int): Boolean = currRate > prevRate
    override fun computeOutput(): Array<Pair<T, Int>> {
        return map.filter { itemWithFreq ->
            itemWithFreq.value.toDouble() >= (frequency * totalProcessedElements - error * totalProcessedElements)
        }.toList().sortedBy { it.second }.toTypedArray()
    }
}
