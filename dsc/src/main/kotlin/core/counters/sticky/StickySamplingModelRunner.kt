package core.counters.sticky

enum class Item { Red, Blue, Yellow, Brown, Green }
object StickySamplingModelRunner {
    fun create(count: Int, type: Item): List<Item> {
        return Array(count) { type }.toList()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val frequency = 0.03
        val error = 0.1 * frequency
        val probabilityOfFailure = 0.1 * error
        val itemBatches = listOf(
            listOf(
                create(19, Item.Red),
                create(11, Item.Blue),
                create(10, Item.Yellow),
                create(10, Item.Brown),
                create(0, Item.Green)
            ), listOf(create(30, Item.Red), create(10, Item.Blue), create(10, Item.Yellow)), listOf(
                create(30, Item.Red),
                create(10, Item.Blue),
                create(0, Item.Yellow),
                create(5, Item.Brown),
                create(5, Item.Green)
            ), listOf(create(40, Item.Red), create(10, Item.Blue)), listOf(
                create(40, Item.Red), create(10, Item.Blue)
            )
        )
        val model = StickySamplingModel<Item>(frequency, error, probabilityOfFailure)
        println("Frequency: $frequency, Error: $error Probability of failure: $probabilityOfFailure")
        val x = itemBatches.flatten().flatten()

        val group = x.groupBy { it }
        group.forEach { t, u ->
            println("($t, ${u.size})")
        }
        println("after counting")
        x.chunked(20).forEach { i ->
            model.process(i)
        }
        model.computeOutput().forEach{pair -> println(pair)}

    }
}


