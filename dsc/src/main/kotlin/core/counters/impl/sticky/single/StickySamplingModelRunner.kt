package core.counters.impl.sticky.single

enum class Item { Red, Blue, Yellow, Brown, Green }
object StickySamplingModelRunner {
    private fun create(count: Int, type: Item): List<Item> {
        return Array(count) { type }.toList()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val frequency = 0.01
        val error = 0.19 * frequency
        val probabilityOfFailure = 0.1 * error
        val itemBatches = listOf(
            listOf(
                create(
                    11290, Item.Red
                ), create(
                    11110, Item.Blue
                ), create(
                    1101, Item.Yellow
                ), create(
                    10445, Item.Brown
                ), create(
                    345, Item.Green
                )
            ), listOf(
                create(
                    344550, Item.Red
                ), create(
                    410, Item.Blue
                ), create(
                    10, Item.Yellow
                )
            ), listOf(
                create(
                    3203, Item.Red
                ), create(
                    1203, Item.Blue
                ), create(
                    145144, Item.Yellow
                ), create(
                    1515, Item.Brown
                ), create(
                    312135, Item.Green
                )
            ), listOf(
                create(
                    4440, Item.Red
                ), create(
                    4130, Item.Blue
                )
            ), listOf(
                create(
                    410, Item.Red
                ), create(
                    11230, Item.Blue
                )
            )
        )



        repeat(10) {
            val x = itemBatches.flatten().flatten().shuffled()

            val model =
                StickySamplingModel<Item>(
                    frequency, error, probabilityOfFailure
                )
            println("Frequency: $frequency, Error: $error Probability of failure: $probabilityOfFailure")
            x.chunked(20).forEach { i ->
                model.process(i)
            }
            model.computeOutput().forEach { pair -> println(pair) }
        }
    }
}


