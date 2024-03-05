package org.example.bitvavo.jvm

import java.text.NumberFormat

/**
 * Represents an exchange, type of central limit order book, where orders are matched using `price time priority`.
 * This means orders are matched by the order of price and then by the arrival time in the book.
 *
 * Exchange trades only occur during the processing of a newly posted order, and happen immediately.
 * As orders are placed in the exchange, they are considered for aggressive matching first against
 * the opposite side of the book. Once this is completed, any remaining order quantity will
 * rest on their own side of the book.
 */
class Exchange {
    /*
     * These variables are essentially the two sides of the book, where
     * each order resides. They maintain a `price time priority` order,
     * this way each incoming order can just iterate the corresponding book
     * to find a match.
     *
     * The `price time priority` order is maintained by sorting the book
     * each time an insertion happens, and using the "priority" variable.
     *
     * The flow for an insert operation is: when we have an order ready
     * to be placed in the book, we have to attach it priority by using the `attachPriority()`,
     * and then place it in the book by using the `placeInBook()`.
     */
    private val buyBook = mutableListOf<BuyOrderWithPriority>()
    private val sellBook = mutableListOf<SellOrderWithPriority>()

    /*
     * This counter tracks the total number of orders received.
     * It is used to define a "time" order for the books.
     *
     * The counter is incremented by `attachPriority()` function,
     * and it should be preferred when we need to attach priority to an order.
     *
     * NB: The books by themselves do not enforce the counter
     * to be ever incremented or unique, we have to handle it "manually".
     * Otherwise, we lose the "time" order priority.
     */
    private var priority = 0

    /**
     * Stores the trade handler installed by [invokeOnTrade] .
     */
    private var tradeHandler: TradeHandler? = null

    /**
     * Places a [buy-order][BuyOrder] and tries immediately
     * to find a match against the `resting` orders, using `price time priority`.
     */
    fun placeBuyOrder(order: BuyOrder) {
        if (sellBook.isEmpty()) {
            val orderWithPriority = attachPriority(order)
            placeInBook(orderWithPriority)
            return
        }
        // In the case that we need to remove a resting order,
        // we will have to store its index and remove it after the iteration is done.
        // Otherwise, the order of the iteration will break.
        val indexesToRemove = mutableListOf<Int>()
        var orderQuantity = order.quantity
        for (index in sellBook.indices) {
            val sell = sellBook[index]
            // Skip higher prices as they pass buyer's limit.
            if (order.limitPrice < sell.limitPrice) continue
            val output = createTrade(
                BuyOrder(
                    id = order.id,
                    limitPrice = order.limitPrice,
                    quantity = orderQuantity
                ),
                sell
            )
            // A trade has happened, "log" it and update values accordingly.
            invokeTradeHandler(output)
            when {
                orderQuantity < sell.quantity -> {
                    // Do not increase priority here as we have not
                    // used all the shares from this resting order,
                    // update with new quantity and place it back in the book.
                    val orderWithPriority = SellOrderWithPriority(
                        id = sell.id,
                        limitPrice = sell.limitPrice,
                        quantity = sell.quantity - orderQuantity,
                        priority = sell.priority
                    )
                    // Since price and priority stay the same,
                    // there is no need sort the arrayList.
                    sellBook[index] = orderWithPriority
                    // The order has been completed.
                    cleanupIndexesInSellBook(indexesToRemove)
                    return
                }

                orderQuantity == sell.quantity -> {
                    indexesToRemove.add(index)
                    // The order has been completed.
                    cleanupIndexesInSellBook(indexesToRemove)
                    return
                }

                else -> {
                    orderQuantity -= sell.quantity
                    indexesToRemove.add(index)
                }
            }
        }
        // Any remaining order quantity, either in case there is no match
        // or the order has leftovers, place it the order book for future matching.
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

    /**
     * Places a [sell-order][SellOrder] and tries immediately
     * to find a match against the `resting` orders, using `price time priority`.
     */
    fun placeSellOrder(order: SellOrder) {
        if (buyBook.isEmpty()) {
            val orderWithPriority = attachPriority(order)
            placeInBook(orderWithPriority)
            return
        }
        // In the case that we need to remove a resting order,
        // we will have to store its index and remove it after the iteration is done.
        // Otherwise, the order of the iteration will break.
        val indexesToRemove = mutableListOf<Int>()
        var orderQuantity = order.quantity
        for (index in buyBook.indices) {
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
            // A trade has happened, "log" it and update values accordingly.
            invokeTradeHandler(output)
            when {
                orderQuantity < buy.quantity -> {
                    // Do not increase priority here as we have not
                    // used all the shares from this resting order,
                    // update with new quantity and place it back in the book.
                    val orderWithPriority = BuyOrderWithPriority(
                        id = buy.id,
                        limitPrice = buy.limitPrice,
                        quantity = buy.quantity - orderQuantity,
                        priority = buy.priority
                    )
                    // Since price and priority stay the same,
                    // there is no need sort the arrayList.
                    buyBook[index] = orderWithPriority
                    // The order has been completed.
                    cleanupIndexesInBuyBook(indexesToRemove)
                    return
                }

                orderQuantity == buy.quantity -> {
                    indexesToRemove.add(index)
                    // The order has been completed.
                    cleanupIndexesInBuyBook(indexesToRemove)
                    return
                }

                else -> {
                    orderQuantity -= buy.quantity
                    indexesToRemove.add(index)
                }
            }
        }
        // Any remaining order quantity, either in case there is no match
        // or the order has leftovers, place it the order book for future matching.
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

    private fun createTrade(
        aggressingOrder: Order,
        restingOrder: Order
    ): Trade = Trade(aggressingOrder, restingOrder)

    private fun invokeTradeHandler(trade: Trade) {
        val tradeHandler = tradeHandler ?: return
        tradeHandler(trade)
    }

    /**
     * Registers a [handler] to invoke when a trade takes place.
     *
     * It is possible to change the underlying [handler] multiple times during the lifetime of this
     * [Exchange].
     */
    fun invokeOnTrade(handler: (input: Trade) -> Unit) {
        tradeHandler = handler
    }

    /**
     * Returns a [String] that represents the order's book output.
     * It follows a format of: "000,000,000 000000 | 000000 000,000,000", and when a value is too
     * small to cover the reserved area is padded with spaces.
     *
     * In case the book is empty it returns an empty [String].
     */
    fun getOrderBookOutput(): String {
        val builder = StringBuilder()
        var buyBookIndex = 0
        var sellBookIndex = 0
        while (buyBookIndex < buyBook.size || sellBookIndex < sellBook.size) {
            val buyFormat = getLineOutputFor(buyBookIndex, buyBook, true)
            val sellFormat = getLineOutputFor(sellBookIndex, sellBook, false)
            val output = "$buyFormat | $sellFormat"
            builder.append(output)
            builder.append("\n")
            buyBookIndex++
            sellBookIndex++
        }
        return builder.toString()
    }

    private fun getLineOutputFor(
        index: Int,
        book: List<Order>,
        /* Indicates in which side the padding should be applied.
         By extension, this also indicates whether this is a buy
         or sell order. */
        padStart: Boolean
    ): String {
        val order = book.getOrNull(index)
        var quantity = ""
        var price = ""
        if (order != null) {
            quantity = formatter.format(order.quantity)
            price = order.limitPrice.toString()
        }
        return if (padStart) {
            quantity = quantity.padStart(12)
            price = price.padStart(6)
            "$quantity $price"
        } else {
            quantity = quantity.padEnd(12)
            price = price.padEnd(6)
            "$price $quantity"
        }
    }

    private val formatter = NumberFormat.getIntegerInstance()
}

data class BuyOrder(
    override val id: String,
    override val limitPrice: Int,
    override val quantity: Int
) : Order {
    init {
        require(limitPrice <= 999999) {
            "The max price is 999999, but got:$limitPrice"
        }
        require(quantity <= 999999999) {
            "The max quantity is 999999999, but got:$quantity"
        }
    }
}

data class SellOrder(
    override val id: String,
    override val limitPrice: Int,
    override val quantity: Int
) : Order {
    init {
        require(limitPrice <= 999999) {
            "The max price is 999999, but got:$limitPrice"
        }
        require(quantity <= 999999999) {
            "The max quantity is 999999999, but got:$quantity"
        }
    }
}

/**
 * Represents a completed trade.
 * This class is created to make passing and asserting trade-data easier.
 */
data class Trade(val aggressingOrder: Order, val restingOrder: Order) {
    /**
     * The price of the match.
     */
    val price: Int = restingOrder.limitPrice

    /**
     * The quantity traded.
     */
    val quantity = if (aggressingOrder.quantity > restingOrder.quantity) {
        restingOrder.quantity
    } else {
        aggressingOrder.quantity
    }

    /**
     * Returns a [String] that represents the trade's output.
     * It follows a format of: "aggressing-order-id, resting-order-id, price-match, quantity-traded".
     */
    fun tradeOutput(): String {
        return "trade ${aggressingOrder.id}, ${restingOrder.id}, $price, $quantity"
    }

    // We care only for the actual content of each trade,
    // not if they represent the same implementation.
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

/**
 * Represents an order to be placed in the exchange.
 * This interface is a helper for the [Trade] class to make the code easier to read.
 */
interface Order {
    val id: String

    /**
     * Represents the worst possible price the trader will trade at.
     */
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