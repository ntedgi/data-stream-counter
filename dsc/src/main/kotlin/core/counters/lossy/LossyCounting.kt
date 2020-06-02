package core.counters.lossy

import it.unimi.dsi.fastutil.objects.Object2LongMap
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap
import utils.LogProvider
import utils.linfo
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.absoluteValue

private typealias CMap<T> = Object2LongOpenHashMap<T>

class LossyCounting<T>(
    private val iterationSize: Int,
    private val minAmountPerIteration: Int,
    concurrency: Int = Runtime.getRuntime().availableProcessors() * 8
) : LogProvider() {
    val gcCounter = AtomicInteger(0)
    private val currentStep = AtomicLong(0)
    private val counters = Array(concurrency) { CMap<T>() }
    fun add(v: T, amount: Long = 1L) {
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

    fun complete(): Object2LongMap<T> {
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

fun main(args: Array<String>) {
    val c = LossyCounting<String>(100_000, 2)
    val lines = File("/home/naort/projects/data/cs_datasets/allht.txt").readLines()
    repeat(1) {
        lines.parallelStream().forEach { ht ->
            c.add(ht)
        }
    }
    File("/home/naort/projects/data/cs_datasets/out.txt").printWriter().use { pw ->
        val result = c.complete()
        result.object2LongEntrySet().sortedByDescending { it.longValue }.forEach {
            pw.println("${it.key}:${it.longValue}")
        }
    }
    println(c.gcCounter.getAndIncrement())
    println("done")
}
