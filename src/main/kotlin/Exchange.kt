package org.example.bitvavo.jvm

import java.util.*
import kotlin.math.roundToInt

class Exchange {
    //TODO: add kdocs
//    private val buyHeap = PriorityQueue<BuyOrderWithPriority> { order1, order2 ->
//        // I think is not completely right,
//        //TODO: fix it later.
//        (order1.limitPrice - order2.limitPrice + order1.priority - order2.priority).roundToInt()
//    }
//    //TODO: add kdocs
//    private val sellHeap = PriorityQueue<BuyOrderWithPriority> { order1, order2 ->
//        // I think is not completely right,
//        //TODO: fix it later.
//        (order1.limitPrice - order2.limitPrice + order1.priority - order2.priority).roundToInt()
//    }

    private var priority = 0

    fun placeBuyOrder(order: BuyOrder) {
        val orderWithPriority = attachPriority(order)

    }

    fun placeSellOrder() {}

    private data class BuyOrderWithPriority(
        val priority: Int,
        val limitPrice: Int,
        val quantity: Int
    )

    private fun attachPriority(order: BuyOrder): BuyOrderWithPriority {
        priority++
        return BuyOrderWithPriority(priority, order.limitPrice, order.quantity)
    }

    private data class SellOrderWithPriority(
        val priority: Int,
        val limitPrice: Int,
        val quantity: Int
    )

    private fun attachPriority(order: SellOrder): SellOrderWithPriority {
        priority++
        return SellOrderWithPriority(priority, order.limitPrice, order.quantity)
    }
}

//TODO: check for bounds based on expected inputs.
// I think we are ok with Int, as max input will be: 999,999,999,
// maybe it can be turned into constrain in constructor,
// will think about it later.
// Price max is 999,999
data class BuyOrder(val limitPrice: Int, val quantity: Int)

data class SellOrder(val limitPrice: Int, val quantity: Int)