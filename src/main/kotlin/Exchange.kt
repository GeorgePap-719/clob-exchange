package org.example.bitvavo.jvm

/**
 * Represents a CLOB.
 */
// The function are not thread-safe.
//TODO: add kdocs
class Exchange {
    private val buyBook = mutableListOf<BuyOrderWithPriority>()
    private val sellBook = mutableListOf<SellOrderWithPriority>()

    //TODO: add kdocs
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
            // Skip higher prices as it is the buyer's limit.
            if (order.limitPrice > sell.limitPrice) continue
            val output = createTradeOutput(
                BuyOrder(
                    id = order.id,
                    limitPrice = order.limitPrice,
                    quantity = orderQuantity
                ),
                sell
            )
            //TODO: maybe this will be more readable with `when`.
            if (orderQuantity <= sell.quantity) {
                // Update order
                invokeTradeHandler(output)
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
                invokeTradeHandler(output)
                orderQuantity -= sell.quantity
                sellBook.removeAt(index)
            }
        }
        // In this case order has leftover quantities,
        // place order in the book, for future matching.
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

    private val buyBookComparator =
        compareByDescending(BuyOrderWithPriority::limitPrice)
            .thenBy(BuyOrderWithPriority::priority)

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
        // Try to match aggressively.
        for (index in 0..<buyBook.size) {
            val buy = buyBook[index]
            // Skip lower prices as it is the seller's limit.
            if (order.limitPrice < buy.limitPrice) continue
            val output = createTradeOutput(
                SellOrder(
                    id = order.id,
                    limitPrice = order.limitPrice,
                    quantity = orderQuantity
                ),
                buy
            )
            if (orderQuantity <= buy.quantity) {
                invokeTradeHandler(output)
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
                invokeTradeHandler(output)
                orderQuantity -= buy.quantity
                //TODO: what happens in the collection,
                // we remove this?
                buyBook.removeAt(index)
            }
        }
        // In this case order has leftover quantities,
        // place order in the book, for future matching.
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

    private val sellBookComparator =
        compareBy(SellOrderWithPriority::limitPrice)
            .thenBy(SellOrderWithPriority::priority)

    private fun attachPriority(order: SellOrder): SellOrderWithPriority {
        priority++
        return SellOrderWithPriority(
            id = order.id,
            limitPrice = order.limitPrice,
            quantity = order.quantity,
            priority = priority
        )
    }

    // Trade output must indicate the aggressing order-id,
    // the resting order-id, the price of the match
    //and the quantity traded, followed by a newline.
    // format: trade 10006,10001,100,500
    //TODO: verify if price is correct.
    private fun createTradeOutput(
        aggressingOrder: Order,
        restingOrder: Order
    ): String {
        val quantity = if (aggressingOrder.quantity > restingOrder.quantity) {
            restingOrder.quantity
        } else {
            aggressingOrder.quantity
        }
        val output = """
            trade ${aggressingOrder.id}, ${restingOrder.id}, ${aggressingOrder.limitPrice}, $quantity
        """.trimIndent()
        return output
    }

    private fun invokeTradeHandler(tradeOutput: String) {
        val tradeHandler = tradeHandler ?: return
        tradeHandler(tradeOutput)
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
}

//TODO: check for bounds based on expected inputs.
// I think we are ok with Int, as max input will be: 999,999,999,
// maybe it can be turned into constrain in constructor,
// will think about it later.
// Price max is 999,999
data class BuyOrder(
    override val id: String,
    override val limitPrice: Int,
    override val quantity: Int
) : Order

data class SellOrder(
    override val id: String,
    override val limitPrice: Int,
    override val quantity: Int
) : Order

/**
 * A class to make passing, asserting data easier.
 * TODO
 */
class Trade()

// --- utils ---

// A marker interface
private interface Order {
    val id: String
    val limitPrice: Int
    val quantity: Int
}

private data class BuyOrderWithPriority(
    override val id: String,
    override val limitPrice: Int,
    override val quantity: Int,
    val priority: Int
) : Order

private data class SellOrderWithPriority(
    override val id: String,
    override val limitPrice: Int,
    override val quantity: Int,
    val priority: Int
) : Order

private typealias TradeHandler = (input: String) -> Unit