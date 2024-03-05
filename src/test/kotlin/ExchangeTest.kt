package org.example.bitvavo.jvm

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class ExchangeTest {

    @Test
    fun testNoMatchScenario() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("10000", 98, 25500),
            BuyOrder("10003", 99, 50000),
        )
        val sellOrders = listOf(
            SellOrder("10005", 105, 20000),
            SellOrder("10001", 100, 500),
            SellOrder("10002", 100, 10000),
            SellOrder("10004", 103, 100)
        )
        exchange.placeBuyOrders(buyOrders)
        exchange.placeSellOrders(sellOrders)
    }

    @Test
    fun testMatchingSellEvent() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("1", 99, 1000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200)
        )
        val sellOrder1 = SellOrder("1234", 101, 2000)
        val sellOrder2 = SellOrder("12345", 95, 2000)
        val actualTrades = mutableListOf<Trade>()
        exchange.placeBuyOrders(buyOrders)
        exchange.placeSellOrder(sellOrder1)
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeSellOrder(sellOrder2)
        val expectedTrades = listOf(
            Trade(sellOrder2, buyOrders[0]),
            Trade(SellOrder("12345", 95, 1000), buyOrders[1]),
            Trade(SellOrder("12345", 95, 500), buyOrders[2])
        )
        assertTradesMatch(expectedTrades, actualTrades)
    }

    @Test
    fun testMatchingBuyEvent() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("10000", 98, 25500),
            BuyOrder("10003", 99, 50000),
        )
        val sellOrders = listOf(
            SellOrder("10005", 105, 20000),
            SellOrder("10001", 100, 500),
            SellOrder("10002", 100, 10000),
            SellOrder("10004", 103, 100)
        )
        val buyOrder = BuyOrder("10006", 105, 16000)
        val actualTrades = mutableListOf<Trade>()
        exchange.placeBuyOrders(buyOrders)
        exchange.placeSellOrders(sellOrders)
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeBuyOrder(buyOrder)
        val expectedTrades = listOf(
            Trade(buyOrder, sellOrders[1]),
            Trade(BuyOrder("10006", 105, 15500), sellOrders[2]),
            Trade(BuyOrder("10006", 105, 5500), sellOrders[3]),
            Trade(BuyOrder("10006", 105, 5400), sellOrders[0])
        )
        assertTradesMatch(expectedTrades, actualTrades)
    }

    @Test
    fun testMatchingBuyEvent2() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrder = BuyOrder("10000", 101, 200)
        val sellOrders = listOf(
            SellOrder("10001", 100, 200),
            SellOrder("10002", 99, 50),
            SellOrder("10003", 98, 50),
        )
        val newBuyOrder = BuyOrder("10004", 101, 200)
        val actualTrades = mutableListOf<Trade>()
        exchange.placeSellOrders(sellOrders)
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeBuyOrder(buyOrder)
        exchange.placeBuyOrder(newBuyOrder)
        val expectedTrades = listOf(
            Trade(buyOrder, sellOrders[2]),
            Trade(BuyOrder("10000", 101, 150), sellOrders[1]),
            Trade(BuyOrder("10000", 101, 100), sellOrders[0]),
            Trade(newBuyOrder, SellOrder("10001", 100, 100))
        )
        assertTradesMatch(expectedTrades, actualTrades)
    }

    @Test
    fun testMatchAllEvents() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("10000", 999999, 999999999),
            BuyOrder("10003", 99, 50000),
            BuyOrder("10006", 105, 16000),
            BuyOrder("10009", 110, 100),
        )
        val sellOrders = listOf(
            SellOrder("10005", 105, 20000),
            SellOrder("10001", 100, 500),
            SellOrder("10002", 100, 10000),
            SellOrder("10004", 103, 100),
            SellOrder("10007", 103, 999969399),
            SellOrder("10008", 106, 100),
            SellOrder("10010", 99, 66000),
        )
        val actualTrades = mutableListOf<Trade>()
        // The book is empty, no trade should happen,
        // and order should be placed in the book.
        exchange.placeBuyOrder(buyOrders[0])
        exchange.invokeOnTrade { actualTrades.add(it) }
        // Place 3 sell orders where all find a match.
        // `buyOrder[0]` has a limit-price of 999999, that means
        // whichever sell order we place it will be matched with this order.
        exchange.placeSellOrders(sellOrders.subList(0, 3))
        exchange.expectNoTrade()
        exchange.placeBuyOrder(buyOrders[1]) // no sells in book right now
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeSellOrder(sellOrders[3]) // matches against `buyOrder[0]`
        exchange.expectNoTrade()
        exchange.placeBuyOrder(buyOrders[2]) // no sells in book right now
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeSellOrder(sellOrders[4]) // matches against `buyOrder[0]` and clears it out
        exchange.expectNoTrade()
        exchange.placeSellOrder(sellOrders[5]) // no buys in book right now
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeBuyOrder(buyOrders[3]) // matches against `sellOrder[5]`
        // Note now the book has two buy orders with ids `10003` and `10006`.
        exchange.placeSellOrder(sellOrders[6]) // matches against `buyOrder[2]` and `buyOrder[1]`
        val expectedTrades = listOf(
            Trade(sellOrders[0], buyOrders[0]),
            Trade(sellOrders[1], BuyOrder("10000", 999999, 999979999)),
            Trade(sellOrders[2], BuyOrder("10000", 999999, 999979499)),
            Trade(sellOrders[3], BuyOrder("10000", 999999, 999969499)),
            Trade(sellOrders[4], BuyOrder("10000", 999999, 999969399)),
            Trade(BuyOrder("10009", 110, 100), sellOrders[5]),
            Trade(sellOrders[6], buyOrders[2]),
            Trade(SellOrder("10010", 99, 50000), buyOrders[1])
        )
        assertTradesMatch(expectedTrades, actualTrades)
        assertTrue(exchange.getOrderBookOutput().isEmpty()) // book should be empty now
    }

    @Test
    fun testOrderBookOutput() {
        val exchange = Exchange()
        val buyOrders = listOf(
            BuyOrder("1", 99, 50000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200),
            BuyOrder("1235", 1, 1200)
        )
        val sellOrder1 = SellOrder("1234", 101, 2000)
        val sellOrder2 = SellOrder("12345", 95, 2000)
        exchange.placeBuyOrders(buyOrders)
        exchange.placeSellOrder(sellOrder1)
        exchange.placeSellOrder(sellOrder2)
        val output = exchange.getOrderBookOutput()
        println(output)
    }

    @Test
    fun testDeterminismScenario() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("10000", 98, 25500),
            BuyOrder("10003", 99, 50000),
            BuyOrder("10006", 105, 16000)
        )
        val sellOrders = listOf(
            SellOrder("10005", 105, 20000),
            SellOrder("10001", 100, 500),
            SellOrder("10002", 100, 10000),
            SellOrder("10004", 103, 100)
        )
        exchange.placeBuyOrder(buyOrders[0])
        exchange.placeSellOrder(sellOrders[0])
        exchange.placeSellOrder(sellOrders[1])
        exchange.placeSellOrder(sellOrders[2])
        exchange.placeBuyOrder(buyOrders[1])
        exchange.placeSellOrder(sellOrders[3])
        exchange.invokeOnTrade {}
        exchange.placeBuyOrder(buyOrders[2])
        val output = exchange.getOrderBookOutput()
        val hash = output.hashStringMD5()
        assertEquals("373712dd293720b5512cbc07ad10d253", hash)
    }

    @Test
    fun testDeterminismScenarioWithNoMatch() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("10000", 98, 25500),
            BuyOrder("10003", 99, 50000),
        )
        val sellOrders = listOf(
            SellOrder("10005", 105, 20000),
            SellOrder("10001", 100, 500),
            SellOrder("10002", 100, 10000),
            SellOrder("10004", 103, 100)
        )
        exchange.placeBuyOrder(buyOrders[0])
        exchange.placeSellOrder(sellOrders[0])
        exchange.placeSellOrder(sellOrders[1])
        exchange.placeSellOrder(sellOrders[2])
        exchange.placeBuyOrder(buyOrders[1])
        exchange.placeSellOrder(sellOrders[3])
        val output = exchange.getOrderBookOutput()
        val hash = output.hashStringMD5()
        assertEquals("a03051214b14de693d9ef1e99d56298a", hash)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun String.hashStringMD5(): String {
        val md5 = MessageDigest.getInstance("MD5")
        return md5.digest(this.toByteArray()).toHexString()
    }
}

private fun Exchange.expectNoTrade() {
    invokeOnTrade { fail("A trade should not happen") }
}

private fun assertTradesMatch(expected: List<Trade>, actual: List<Trade>) {
    if (actual.size != expected.size) {
        fail("Expected size is ${expected.size}, but actual is:${actual.size}")
    }
    for (index in expected.indices) {
        assertEquals(expected[index], actual[index])
    }
}

// --- batches ---

fun Exchange.placeBuyOrders(orders: List<BuyOrder>) {
    for (order in orders) placeBuyOrder(order)
}

fun Exchange.placeSellOrders(orders: List<SellOrder>) {
    for (order in orders) placeSellOrder(order)
}