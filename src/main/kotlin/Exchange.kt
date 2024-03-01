package org.example.bitvavo.jvm

/**
 * Represents a CLOB.
 */
// Notes: I have to break the matching order pattern.
//
//TODO: add kdocs
class Exchange {
    private val buyBook = mutableListOf<BuyOrderWithPriority>()
    private val sellBook = mutableListOf<SellOrderWithPriority>()

    private var priority = 0

    fun placeBuyOrder(order: BuyOrder) {
        if (sellBook.isEmpty()) {
            val orderWithPriority = attachPriority(order)
            placeInBook(orderWithPriority)
            return
        }
        var orderQuantity = order.quantity
        // Try to match aggressively.
        for ((index, sell) in sellBook.withIndex()) {
            if (order.limitPrice > sell.limitPrice) continue
            // Update order
            //TODO: maybe this will be more readable with `when`.
            if (orderQuantity <= sell.quantity) {
                // Call onTrade, to print for success trade.
                onTrade()
                // Update sell with new values.
                if (orderQuantity == sell.quantity) {
                    sellBook.removeAt(index)
                } else {
                    val orderWithPriority = SellOrderWithPriority(
                        sell.priority,
                        sell.limitPrice,
                        sell.quantity - order.quantity
                    )
                    sellBook[index] = orderWithPriority
                }
                // Break. ----> TODO Improve comment.
                return
            } else {
                // Call onTrade, to print for success trade.
                onTrade()
                orderQuantity -= sell.quantity
                sellBook.removeAt(index)
            }
        }
        // In case the order was either not fulfilled or completed,
        // place it in the book.
        if (orderQuantity > 0) {
            val orderWithPriority = attachPriority(BuyOrder(order.limitPrice, orderQuantity))
            placeInBook(orderWithPriority)
        }
    }

    private fun placeInBook(order: BuyOrderWithPriority) {
        buyBook.add(order)
        buyBook.sortWith(buyBookComparator)
    }

    private fun attachPriority(order: BuyOrder): BuyOrderWithPriority {
        priority++
        return BuyOrderWithPriority(priority, order.limitPrice, order.quantity)
    }

    fun placeSellOrder(order: SellOrder) {
        if (buyBook.isEmpty()) {
            val orderWithPriority
        }
    }

    private fun placeInBook(order: SellOrderWithPriority) {
        sellBook.add(order)
        sellBook.sortWith(sellBookComparator)
    }

    private fun attachPriority(order: SellOrder): SellOrderWithPriority {
        priority++
        return SellOrderWithPriority(priority, order.limitPrice, order.quantity)
    }

    fun onTrade() {
        TODO()
    }

    fun getBookContents() {
        TODO()
    }

    private data class BuyOrderWithPriority(
        val priority: Int,
        val limitPrice: Int,
        val quantity: Int
    )

    private val buyBookComparator =
        compareByDescending(BuyOrderWithPriority::limitPrice)
            .thenBy(BuyOrderWithPriority::priority)

    private data class SellOrderWithPriority(
        val priority: Int,
        val limitPrice: Int,
        val quantity: Int
    )

    private val sellBookComparator =
        compareBy(SellOrderWithPriority::limitPrice)
            .thenBy(SellOrderWithPriority::priority)
}

//TODO: check for bounds based on expected inputs.
// I think we are ok with Int, as max input will be: 999,999,999,
// maybe it can be turned into constrain in constructor,
// will think about it later.
// Price max is 999,999
data class BuyOrder(val limitPrice: Int, val quantity: Int)

data class SellOrder(val limitPrice: Int, val quantity: Int)


