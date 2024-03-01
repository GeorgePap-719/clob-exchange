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

    private var tradeHandler: TradeHandler? = null

    //TODO: add kdocs
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
                invokeTradeHandler("TODO")
                // Update sell with new values.
                if (orderQuantity == sell.quantity) {
                    sellBook.removeAt(index)
                } else {
                    val orderWithPriority = SellOrderWithPriority(
                        id = sell.id,
                        limitPrice = sell.limitPrice,
                        quantity = sell.quantity - order.quantity,
                        priority = sell.priority
                    )
                    // Since price and priority stay the same,
                    // there is no need sort the arrayList.
                    sellBook[index] = orderWithPriority
                }
                // The order has been completed.
                return
            } else {
                // Call onTrade, to print for success trade.
                invokeTradeHandler("TODO")
                orderQuantity -= sell.quantity
                sellBook.removeAt(index)
            }
        }
        // In case the order was either not fulfilled or completed,
        // place it in the book.
        if (orderQuantity > 0) {
            val buyOrder = BuyOrder(order.id, order.limitPrice, orderQuantity)
            val orderWithPriority = attachPriority(buyOrder)
            placeInBook(orderWithPriority)
        }
    }

    private fun placeInBook(order: BuyOrderWithPriority) {
        buyBook.add(order)
        buyBook.sortWith(buyBookComparator)
    }

    private fun attachPriority(order: BuyOrder): BuyOrderWithPriority {
        priority++
        return BuyOrderWithPriority(
            id = order.id,
            limitPrice = order.limitPrice,
            quantity = order.quantity,
            priority = priority
        )
    }

    //TODO: add kdocs
    fun placeSellOrder(order: SellOrder) {
        if (buyBook.isEmpty()) {
            val orderWithPriority = attachPriority(order)
            placeInBook(orderWithPriority)
            return
        }
        var orderQuantity = order.quantity
        for ((index, buy) in buyBook.withIndex()) {
            // Skip lower prices as it is the selle's limit.
            if (order.limitPrice < buy.limitPrice) continue
            if (orderQuantity <= buy.quantity) {
                // Call onTrade, to print for success trade.
                invokeTradeHandler("TODO")
                if (orderQuantity == buy.quantity) {
                    buyBook.removeAt(index)
                } else {
                    val orderWithPriority = BuyOrderWithPriority(
                        id = buy.id,
                        limitPrice = buy.limitPrice,
                        quantity = buy.quantity - orderQuantity,
                        priority = buy.priority
                    )
                    // Since price and priority stay the same,
                    // there is no need sort the arrayList.
                    buyBook[index] = orderWithPriority
                }
                // The order has been completed.
                return
            } else {
                // Call onTrade, to print for success trade.
                invokeTradeHandler("TODO")
                orderQuantity -= buy.quantity
                buyBook.removeAt(index)
            }
        }
        // In case the order was either not fulfilled or completed,
        // place it in the book. ----> TODO improve comment
        if (orderQuantity > 0) {
            val sellOrder = SellOrder(order.id, order.limitPrice, orderQuantity)
            val orderWithPriority = attachPriority(sellOrder)
            placeInBook(orderWithPriority)
        }
    }

    private fun placeInBook(order: SellOrderWithPriority) {
        sellBook.add(order)
        sellBook.sortWith(sellBookComparator)
    }

    private fun attachPriority(order: SellOrder): SellOrderWithPriority {
        priority++
        return SellOrderWithPriority(
            id = order.id,
            limitPrice = order.limitPrice,
            quantity = order.quantity,
            priority = priority
        )
    }

    private fun invokeTradeHandler(tradeInfo: String /* will think about it */) {
        val tradeHandler = tradeHandler ?: return
        tradeHandler(tradeInfo)
    }

    /**
     * Registers an action to perform when a trade takes place.
     */
    // Trade output must indicate the aggressing order-id,
    // the resting order-id, the price of the match
    // and the quantity traded, followed by a newline.
    fun invokeOnTrade(onTrade: (input: String) -> Unit) {
        tradeHandler = onTrade
    }


    fun getBookContents() {
        TODO()
    }

    private data class BuyOrderWithPriority(
        val id: String,
        val limitPrice: Int,
        val quantity: Int,
        val priority: Int
    )

    private val buyBookComparator =
        compareByDescending(BuyOrderWithPriority::limitPrice)
            .thenBy(BuyOrderWithPriority::priority)

    private data class SellOrderWithPriority(
        val id: String,
        val limitPrice: Int,
        val quantity: Int,
        val priority: Int
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
data class BuyOrder(val id: String, val limitPrice: Int, val quantity: Int)

data class SellOrder(val id: String, val limitPrice: Int, val quantity: Int)

private typealias TradeHandler = (input: String) -> Unit


