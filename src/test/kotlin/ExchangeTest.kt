package org.example.bitvavo.jvm

import kotlin.test.Test
import kotlin.test.asserter
import kotlin.test.fail

class ExchangeTest {

    @Test
    fun testBasicScenario() {
        val exchange = Exchange()
        exchange.invokeOnTrade { fail("A trade should not happen") }
        val buyOrders = listOf(
            BuyOrder("1", 99, 1000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200)
        )
        exchange.placeBuyOrders(buyOrders)
    }

    // Buy 1000 @ 99 is matched first (as it is the oldest order at the highest price level)
    // Buy 500 @ 99 is matched second (as it is at the highest price level and arrived after the
    // Buy 1000 @ 99 order)
    @Test
    fun testBasicScenario2() {
        val exchange = Exchange()
        exchange.invokeOnTrade { println(it) }
        val buyOrders = listOf(
            BuyOrder("1", 99, 1000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200)
        )
        exchange.placeBuyOrders(buyOrders)
        var sellOrder = SellOrder("1234", 101, 2000)
        exchange.placeSellOrder(sellOrder)
        sellOrder = SellOrder("12345", 95, 2000)
        exchange.placeSellOrder(sellOrder)
    }

}

// --- batches ---

fun Exchange.placeBuyOrders(orders: List<BuyOrder>) {
    for (order in orders) placeBuyOrder(order)
}