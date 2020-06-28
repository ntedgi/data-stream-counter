package core.counters.impl.lossy

import java.io.File

fun main(args: Array<String>) {
    val c = LossyCounting<String>(100_000, 2)
    val lines = File("/home/naort/projects/data/cs_datasets/allht.txt").readLines()
    repeat(1) {
        lines.parallelStream().forEach { ht ->
            c.add(ht, 1)
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
