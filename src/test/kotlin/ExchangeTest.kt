package org.example.bitvavo.jvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ExchangeTest {

    @Test
    fun testNoMatchScenario() {
        val exchange = Exchange()
        exchange.invokeOnTrade { fail("A trade should not happen") }
        val buyOrders = listOf(
            BuyOrder("1", 99, 1000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200)
        )
        exchange.placeBuyOrders(buyOrders)
    }

    // - Buy 1000 @ 99 is matched first (as it is the oldest order at the highest price level)
    // - Buy 500 @ 99 is matched second (as it is at the highest price level and arrived after the
    //   Buy 1000 @ 99 order)
    // - Buy 500 @ 98 is matched third (as it is at a lower price. This partially fills the
    //   resting order of 1200, leaving 700 in the order book)
    @Test
    fun testMatchingSellEvent() {
        val exchange = Exchange()
        val buyOrders = listOf(
            BuyOrder("1", 99, 1000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200)
        )
        val sellOrder2 = SellOrder("12345", 95, 2000)
        val sellOrder2AfterFirstSell = SellOrder("12345", 95, 1000)
        val sellOrder2AfterSecondSell = SellOrder("12345", 95, 500)
        val expectedTrades = listOf(
            Trade(sellOrder2, buyOrders[0]),
            Trade(sellOrder2AfterFirstSell, buyOrders[1]),
            Trade(sellOrder2AfterSecondSell, buyOrders[2]),
        )
        val actualTrades = mutableListOf<Trade>()
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeBuyOrders(buyOrders)
        val sellOrder1 = SellOrder("1234", 101, 2000)
        exchange.placeSellOrder(sellOrder1)
        exchange.placeSellOrder(sellOrder2)
        assertTradesMatch(expectedTrades, actualTrades)
    }
}

private fun assertTradesMatch(expected: List<Trade>, actual: List<Trade>) {
    if (actual.size > expected.size) {
        fail("Actual trades are more than the expected-size:${expected.size}")
    }
    for (index in expected.indices) {
        assertEquals(expected[index], actual[index])
    }
}

// --- batches ---

fun Exchange.placeBuyOrders(orders: List<BuyOrder>) {
    for (order in orders) placeBuyOrder(order)
}