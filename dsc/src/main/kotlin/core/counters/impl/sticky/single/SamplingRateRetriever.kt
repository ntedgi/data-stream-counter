package core.counters.impl.sticky.single

object SamplingRateRetriever {
    /**
     * Returns the rate that corresponds to this item based on the number of items that have been processed so far
     *
     * @param itemSeqId A sequential id (past items + 1)
     * @param t
     * @return
     */
    fun deriveSamplingRate(itemSeqId: Long, t: Double): Int {
        var currentSampligRate = 1
        var sum = 0.0
        while ((currentSampligRate * t + sum) < itemSeqId) {
            sum += (currentSampligRate * t)
            currentSampligRate *= 2
        }
        return currentSampligRate
    }
}