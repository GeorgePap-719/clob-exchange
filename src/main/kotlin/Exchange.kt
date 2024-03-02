package org.example.bitvavo.jvm

import java.text.NumberFormat

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
        val indexesToRemove = mutableListOf<Int>()
        var orderQuantity = order.quantity
        // Try to match aggressively.
        for (index in 0..<sellBook.size) {
            val sell = sellBook[index]
            // Skip higher prices as it is the buyer's limit.
            if (order.limitPrice < sell.limitPrice) continue
            val output = createTrade(
                BuyOrder(
                    id = order.id,
                    limitPrice = order.limitPrice,
                    quantity = orderQuantity
                ),
                sell
            )
            //TODO: maybe this will be more readable with `when`.
            if (orderQuantity <= sell.quantity) {
                invokeTradeHandler(output)
                if (orderQuantity == sell.quantity) {
                    indexesToRemove.add(index)
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
                cleanupIndexesInSellBook(indexesToRemove)
                return
            } else {
                // Call onTrade, to print for success trade.
                invokeTradeHandler(output)
                orderQuantity -= sell.quantity
                indexesToRemove.add(index)
            }
        }
        // In this case order has leftover quantities,
        // place order in the book, for future matching.
        if (orderQuantity > 0) {
            val buyOrder = BuyOrder(order.id, order.limitPrice, orderQuantity)
            val orderWithPriority = attachPriority(buyOrder)
            placeInBook(orderWithPriority)
        }
        cleanupIndexesInSellBook(indexesToRemove)
    }

    private fun cleanupIndexesInSellBook(indexes: List<Int>) {
        var removed = 0
        for (index in indexes) {
            sellBook.removeAt(index - removed)
            removed++
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
        // TODO: add comments why we need this.
        val indexesToRemove = mutableListOf<Int>()
        var orderQuantity = order.quantity
        // Try to match aggressively.
        for (index in 0..<buyBook.size) {
            val buy = buyBook[index]
            // Skip lower prices as it is the seller's limit.
            if (order.limitPrice > buy.limitPrice) continue
            val output = createTrade(
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
                    indexesToRemove.add(index)
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
                cleanupIndexesInBuyBook(indexesToRemove)
                return
            } else {
                invokeTradeHandler(output)
                orderQuantity -= buy.quantity
                indexesToRemove.add(index)
            }
        }
        // In this case order has leftover quantities, // TODO --> this includes the case where we do not have match in price.
        // place order in the book, for future matching.
        if (orderQuantity > 0) {
            val sellOrder = SellOrder(order.id, order.limitPrice, orderQuantity)
            val orderWithPriority = attachPriority(sellOrder)
            placeInBook(orderWithPriority)
        }
        cleanupIndexesInBuyBook(indexesToRemove)
    }

    private fun cleanupIndexesInBuyBook(indexes: List<Int>) {
        var removed = 0
        for (index in indexes) {
            buyBook.removeAt(index - removed)
            removed++
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

    //TODO: is this redundant?
    private fun createTrade(
        aggressingOrder: Order,
        restingOrder: Order
    ): Trade = Trade(aggressingOrder, restingOrder)

    private fun invokeTradeHandler(trade: Trade) {
        val tradeHandler = tradeHandler ?: return
        tradeHandler(trade)
    }

    /**
     * Registers an action to perform when a trade takes place.
     */
    fun invokeOnTrade(action: (input: Trade) -> Unit) {
        tradeHandler = action
    }

    /**
     * Returns a [String] that represents the order's book output.
     * It follows a format of: "000,000,000 000000 | 000000 000,000,000", and when a value is too
     * small to cover the reserved area is padded with spaces.
     */
    fun getOrderBookOutput(): String {
        val formatter = NumberFormat.getIntegerInstance()
        val builder = StringBuilder()
        var buyBookIndex = 0
        var sellBookIndex = 0
        // Maybe this can be improved?, TODO if there is enough time
        while (buyBookIndex < buyBook.size || sellBookIndex < sellBook.size) {
            val buy = if (buyBookIndex < buyBook.size) buyBook[buyBookIndex] else null
            var buyQuantityFormat = buy?.let { formatter.format(buy.quantity) } ?: ""
            buyQuantityFormat = buyQuantityFormat.padStart(9)
            var buyPriceFormat = buy?.let { formatter.format(buy.limitPrice) } ?: ""
            buyPriceFormat = buyPriceFormat.padStart(6)
            val sell = if (sellBookIndex < sellBook.size) sellBook[sellBookIndex] else null
            var sellQuantityFormat = sell?.let { formatter.format(sell.quantity) } ?: ""
            sellQuantityFormat = sellQuantityFormat.padEnd(9)
            var sellPriceFormat = sell?.let { formatter.format(sell.limitPrice) } ?: ""
            sellPriceFormat = sellPriceFormat.padEnd(6)
            val output = "$buyQuantityFormat $buyPriceFormat | $sellPriceFormat $sellQuantityFormat"
            builder.append(output)
            builder.append("\n")
            buyBookIndex++
            sellBookIndex++
        }
        return builder.toString()
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
 * A class to make passing and asserting data easier.
 * TODO kdocs
 */
@Suppress("MemberVisibilityCanBePrivate")
data class Trade(val aggressingOrder: Order, val restingOrder: Order) {
    // Trade output must indicate the aggressing order-id,
    // the resting order-id, the price of the match
    // and the quantity traded, followed by a newline.
    fun tradeOutput(): String {
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

    //TODO: add comment about this override.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Trade
        if (aggressingOrder.id != other.aggressingOrder.id) return false
        if (aggressingOrder.limitPrice != other.aggressingOrder.limitPrice) return false
        if (aggressingOrder.quantity != other.aggressingOrder.quantity) return false
        if (restingOrder.id != other.restingOrder.id) return false
        if (restingOrder.limitPrice != other.restingOrder.limitPrice) return false
        if (restingOrder.quantity != other.restingOrder.quantity) return false
        return true
    }

    override fun hashCode(): Int {
        var result = aggressingOrder.hashCode()
        result = 31 * result + restingOrder.hashCode()
        return result
    }
}

// A marker interface
interface Order {
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

private typealias TradeHandler = (input: Trade) -> Unit