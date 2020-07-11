package org.core.impl.sticky

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.core.impl.FakeDataGenerator
import org.core.impl.Item
import org.junit.jupiter.api.Test

internal class StickySamplingCounterTest {
    @Test
    fun `counter finish without throwing exception less then 3% error`() {
        val frequency = 0.01
        val error = 0.19 * frequency
        val probabilityOfFailure = 0.1 * error
        val counter = StickySamplingCounter<Item>(frequency, error, probabilityOfFailure, 10)
        val streamConfig = mapOf(
            Item.Blue to 100_000L,
            Item.Red to 230_000L,
            Item.Brown to 80_000L,
            Item.Green to 150_000L,
            Item.Yellow to 340_000L
        )
        val data = FakeDataGenerator.createFakeStream(streamConfig)
        data.parallel().forEach {
            counter.add(it)
        }
        val result = counter.complete()
        result.forEach { (t, u) ->
            assertThat(u).isCloseTo(streamConfig[t], Percentage.withPercentage(0.03)).withFailMessage(
                "less then error original count of $t is ${streamConfig[t]} counter counts $u instances  "
            )
        }
    }
}

