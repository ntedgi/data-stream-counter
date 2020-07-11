package org.core.impl.lossy

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.core.impl.FakeDataGenerator
import org.core.impl.Item
import org.junit.jupiter.api.Test

internal class LossyCounterTest{
    @Test
    fun `counter finish without throwing exception less then 2 percent error`() {
        val counter = LossyCounter<Item>(100000,1)
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
            assertThat(u).isCloseTo(streamConfig[t], Percentage.withPercentage(0.02)).withFailMessage(
                "less then error original count of $t is ${streamConfig[t]} counter counts $u instances  "
            )
        }
    }
}

