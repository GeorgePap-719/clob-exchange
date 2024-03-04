package org.example.bitvavo.jvm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ExchangeTest {

    @Test
    fun testNoMatchScenario() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("1", 99, 1000),
            BuyOrder("12", 99, 500),
            BuyOrder("123", 98, 1200)
        )
        exchange.placeBuyOrders(buyOrders)
    }

    @Test
    fun testNoMatchScenario2() {
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
        exchange.placeBuyOrders(buyOrders)
        exchange.placeSellOrder(sellOrder1)
        val actualTrades = mutableListOf<Trade>()
        exchange.invokeOnTrade { actualTrades.add(it) }
        val sellOrder2 = SellOrder("12345", 95, 2000)
        val expectedTrades = listOf(
            Trade(sellOrder2, buyOrders[0]),
            Trade(SellOrder("12345", 95, 1000), buyOrders[1]),
            Trade(SellOrder("12345", 95, 500), buyOrders[2]),
        )
        exchange.placeSellOrder(sellOrder2)
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
        exchange.placeBuyOrders(buyOrders)
        exchange.placeSellOrders(sellOrders)
        val actualTrades = mutableListOf<Trade>()
        exchange.invokeOnTrade { actualTrades.add(it) }
        val buyOrder = BuyOrder("10006", 105, 16000)
        exchange.placeBuyOrder(buyOrder)
        val expectedTrades = listOf(
            Trade(buyOrder, sellOrders[1]),
            Trade(BuyOrder("10006", 105, 15500), sellOrders[2]),
            Trade(BuyOrder("10006", 105, 5500), sellOrders[3]),
            Trade(BuyOrder("10006", 105, 5400), sellOrders[0]),

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
        exchange.placeSellOrders(sellOrders)
        val actualTrades = mutableListOf<Trade>()
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeBuyOrder(buyOrder)
        val newBuyOrder =  BuyOrder("10004", 101, 200)
        exchange.placeBuyOrder(newBuyOrder)
        val expectedTrades = listOf(
            Trade(buyOrder, sellOrders[2]),
            Trade(BuyOrder("10000", 101, 150), sellOrders[1]),
            Trade(BuyOrder("10000", 101, 100), sellOrders[0]),
            Trade(newBuyOrder, SellOrder("10001", 100, 100))
        )
        assertTradesMatch(expectedTrades, actualTrades)
        println(exchange.getOrderBookOutput())
    }

    @Test
    fun testComplexScenario() {
        val exchange = Exchange()
        exchange.expectNoTrade()
        val buyOrders = listOf(
            BuyOrder("10000", 999999, 999999999),
            BuyOrder("10003", 99, 50000),
            BuyOrder("10006", 105, 16000),
        )
        val sellOrders = listOf(
            SellOrder("10005", 105, 20000),
            SellOrder("10001", 100, 500),
            SellOrder("10002", 100, 10000),
            SellOrder("10004", 103, 100),
        )
        val actualTrades = mutableListOf<Trade>()
        exchange.placeBuyOrder(buyOrders[0])
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeSellOrders(sellOrders.subList(0, 3))
        exchange.expectNoTrade()
        exchange.placeBuyOrder(buyOrders[1])
        exchange.invokeOnTrade { actualTrades.add(it) }
        exchange.placeSellOrder(sellOrders[3])
        exchange.expectNoTrade()
        exchange.placeBuyOrder(buyOrders[2])
        // TODO: clear all book
        //exchange.pla
        val expectedTrades = listOf(
            Trade(sellOrders[0], buyOrders[0]),
            Trade(sellOrders[1], BuyOrder("10000", 999999, 999979999)),
            Trade(sellOrders[2], BuyOrder("10000", 999999, 999979499)),
            Trade(sellOrders[3], BuyOrder("10000", 999999, 999969499)),
        )
        assertTradesMatch(expectedTrades, actualTrades)
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
        exchange.placeBuyOrders(buyOrders)
        val sellOrder1 = SellOrder("1234", 101, 2000)
        exchange.placeSellOrder(sellOrder1)
        val sellOrder2 = SellOrder("12345", 95, 2000)
        exchange.placeSellOrder(sellOrder2)
        val output = exchange.getOrderBookOutput()
        println(output)
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