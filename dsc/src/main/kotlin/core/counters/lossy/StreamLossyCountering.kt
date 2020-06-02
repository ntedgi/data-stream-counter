package core.counters.lossy

import it.unimi.dsi.fastutil.objects.Object2LongMap
import utils.LogProvider
import java.io.File
import java.util.stream.Stream

class StreamLossyCountering<T>(
    iterationSize: Int, minAmountPerIteration: Int
) : LogProvider() {
    private val counter = LossyCounting<T>(iterationSize, minAmountPerIteration)
    fun count(stream: Stream<T>): Object2LongMap<T> {
        stream.forEach { counter.add(it) }
        return counter.complete()
    }
}

fun main(args: Array<String>) {
    val c = StreamLossyCountering<String>(100_000, 2)
    val lines = File("/home/naort/projects/data/cs_datasets/allht.txt").bufferedReader().lines()
    c.count(lines)

}
