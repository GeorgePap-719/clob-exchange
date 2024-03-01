package org.example.bitvavo.jvm

import java.util.*

class ExperimentSort {
    val comp = compareByDescending<BuyOrderWithPriority> { it.limitPrice }.thenBy { it.priority }

    val buyBook = mutableListOf<BuyOrderWithPriority>()
}

data class BuyOrderWithPriority(
    val priority: Int,
    val limitPrice: Int,
    val quantity: Int
)

data class SellOrderWithPriority(val priority: Int, val limitPrice: Int, val quantity: Int)


fun main() {
    // test really fast some stuff

//    val test = ExperimentSort()
//    val buys = listOf(
//        BuyOrderWithPriority(1, 99, 1000),
//        BuyOrderWithPriority(2, 99, 500),
//        BuyOrderWithPriority(3, 98, 1200),
//    )
//    test.buyBook.addAll(buys)
//    println(test.buyBook.toString())

    val test2 = ExperimentSort()
    val buys2 = listOf(
        BuyOrderWithPriority(1, 95, 1000), // -> a
        BuyOrderWithPriority(3, 99, 500), // -> b
        BuyOrderWithPriority(2, 99, 1200), // -> c
    )
    test2.buyBook.addAll(buys2)
    // expect -> b, c, a
    test2.buyBook.sortWith(test2.comp)
    for (buy in test2.buyBook) println(buy)
    //println(test2.buyBook.toString())

}
