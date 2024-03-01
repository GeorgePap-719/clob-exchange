package org.example.bitvavo.jvm

class ExperimentSort {
    val buyComp = compareByDescending<BuyOrderWithPriority2> { it.limitPrice }.thenBy { it.priority }

    val sellComp = compareBy<SellOrderWithPriority2> { it.limitPrice }.thenBy { it.priority }

    val buyBook = mutableListOf<BuyOrderWithPriority2>()
    val sellBook = mutableListOf<SellOrderWithPriority2>()
}

data class BuyOrderWithPriority2(
    val priority: Int,
    val limitPrice: Int,
    val quantity: Int
)

data class SellOrderWithPriority2(val priority: Int, val limitPrice: Int, val quantity: Int)


fun main() {
    // test really fast some stuff

    val test = ExperimentSort()
    val sells = listOf(
        SellOrderWithPriority2(1, 100, 500), // -> a
        SellOrderWithPriority2(2, 100, 10000), // -> b
        SellOrderWithPriority2(3, 103, 100), // -> c
        SellOrderWithPriority2(4, 105, 20000), // -> c
    )
    test.sellBook.addAll(sells)
    // expect -> b, c, a
    test.sellBook.sortWith(test.sellComp)
    for (sell in test.sellBook) println(sell)
}
