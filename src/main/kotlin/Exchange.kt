package org.example.bitvavo.jvm

import java.text.NumberFormat
import java.util.*

/**
 * Represents an exchange, type of central limit order book, where orders are matched using `price time priority`.
 * This means orders are matched by the order of price and then by the arrival time in the book.
 *
 * Exchange trades only occur during the processing of a newly posted order, and happen immediately.
 * As orders are placed in the exchange, they are considered for aggressive matching first against
 * the opposite side of the book. Once this is completed, any remaining order quantity will
 * rest on their own side of the book.
 *
 * ### Implementation notes
 *
 * This Exchange implementation provides O(log(n)) time for placing orders in book with no match;
 * O(k * log(n)) for matching orders where `k` is the number of matching orders and `n` the number
 * of total orders in the opposite side of the book.
 */
class Exchange {
    /*
     * These variables are essentially the two sides of the book, where
     * each order resides. They maintain a `price time priority` order.
     * This means that if the aggressive order does not have a matching price
     * with the head of the resting orders, then neither will with the rest of the
     * book.
     *
     * The flow for an insert operation is: when we have an order ready
     * to be placed in the book, we have to attach it priority by using the `attachPriority()`,
     * and then place it in the book.
     *
     * NB: These structures cannot be iterated as their iterators
     * are not guaranteed to traverse the elements in any particular order.
     */
    private val buyBook = PriorityQueue(
        compareByDescending(BuyOrderWithPriority::limitPrice)
            .thenBy(BuyOrderWithPriority::priority)
    )
    private val sellBook = PriorityQueue(
        compareBy(SellOrderWithPriority::limitPrice)
            .thenBy(SellOrderWithPriority::priority)
    )

    /*
     * This counter tracks the total number of orders received.
     * It is used to define a "time" order for the books.
     *
     * The counter is incremented by `attachPriority()` function,
     * and it should be preferred when we need to attach priority to an order.
     *
     * Note that the books by themselves do not enforce the counter
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
        var orderQuantity = order.quantity
        while (!sellBook.isEmpty()) {
            val matchingSellOrder = sellBook.peek()
            // The first order we find that does not match the limit price we return,
            // since all other resting orders will also do not match.
            if (order.limitPrice < matchingSellOrder.limitPrice) {
                val buyOrder = BuyOrder(order.id, order.limitPrice, orderQuantity)
                placeInBook(buyOrder)
                return
            }
            val output = createTrade(
                BuyOrder(
                    id = order.id,
                    limitPrice = order.limitPrice,
                    quantity = orderQuantity
                ),
                matchingSellOrder
            )
            // A trade has happened, "log" it and update values accordingly.
            invokeTradeHandler(output)
            when {
                orderQuantity < matchingSellOrder.quantity -> {
                    // Do not increase priority here as we have not
                    // used all the shares from this resting order,
                    // update with new quantity and place it back in the book.
                    val newHead = SellOrderWithPriority(
                        id = matchingSellOrder.id,
                        limitPrice = matchingSellOrder.limitPrice,
                        quantity = matchingSellOrder.quantity - orderQuantity,
                        priority = matchingSellOrder.priority
                    )
                    sellBook.remove()
                    sellBook.add(newHead)
                    // The order has been completed.
                    return
                }

                orderQuantity == matchingSellOrder.quantity -> {
                    sellBook.remove()
                    // The order has been completed.
                    return
                }

                else -> {
                    orderQuantity -= matchingSellOrder.quantity
                    sellBook.remove()
                }
            }
        }
        // Any remaining order quantity, either in case there is no match
        // or the order has leftovers, place it the order book for future matching.
        if (orderQuantity > 0) {
            val buyOrder = BuyOrder(order.id, order.limitPrice, orderQuantity)
            placeInBook(buyOrder)
        }
    }

    private fun placeInBook(order: BuyOrder) {
        val orderWithPriority = attachPriority(order)
        buyBook.add(orderWithPriority)
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

    /**
     * Places a [sell-order][SellOrder] and tries immediately
     * to find a match against the `resting` orders, using `price time priority`.
     */
    fun placeSellOrder(order: SellOrder) {
        var orderQuantity = order.quantity
        while (!buyBook.isEmpty()) {
            val matchingBuyOrder = buyBook.peek()
            // The first order we find that does not match the limit price we return,
            // since all other resting orders will also do not match.
            if (order.limitPrice > matchingBuyOrder.limitPrice) {
                val sellOrder = SellOrder(order.id, order.limitPrice, orderQuantity)
                placeInBook(sellOrder)
                return
            }
            val output = createTrade(
                SellOrder(
                    id = order.id,
                    limitPrice = order.limitPrice,
                    quantity = orderQuantity
                ),
                matchingBuyOrder
            )
            // A trade has happened, "log" it and update values accordingly.
            invokeTradeHandler(output)
            when {
                orderQuantity < matchingBuyOrder.quantity -> {
                    // Do not increase priority here as we have not
                    // used all the shares from this resting order,
                    // update with new quantity and place it back in the book.
                    val newHead = BuyOrderWithPriority(
                        id = matchingBuyOrder.id,
                        limitPrice = matchingBuyOrder.limitPrice,
                        quantity = matchingBuyOrder.quantity - orderQuantity,
                        priority = matchingBuyOrder.priority
                    )
                    buyBook.remove()
                    buyBook.add(newHead)
                    // The order has been completed.
                    return
                }

                orderQuantity == matchingBuyOrder.quantity -> {
                    buyBook.remove()
                    // The order has been completed.
                    return
                }

                else -> {
                    orderQuantity -= matchingBuyOrder.quantity
                    buyBook.remove()
                }
            }
        }
        // Any remaining order quantity, either in case there is no match
        // or the order has leftovers, place it the order book for future matching.
        if (orderQuantity > 0) {
            val sellOrder = SellOrder(order.id, order.limitPrice, orderQuantity)
            placeInBook(sellOrder)
        }
    }

    private fun placeInBook(order: SellOrder) {
        val orderWithPriority = attachPriority(order)
        sellBook.add(orderWithPriority)
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
        val buyBook = PriorityQueue<Order>(buyBook)
        val sellBook = PriorityQueue<Order>(sellBook)
        while (!buyBook.isEmpty() || !sellBook.isEmpty()) {
            val buyFormat = getLineOutputFor(buyBook, true)
            val sellFormat = getLineOutputFor(sellBook, false)
            val output = "$buyFormat | $sellFormat"
            builder.append(output)
            builder.append("\n")
        }
        return builder.toString()
    }

    private fun getLineOutputFor(
        book: PriorityQueue<Order>,
        /* Indicates in which side the padding should be applied.
         By extension, this also indicates whether this is a buy
         or sell order. */
        padStart: Boolean
    ): String {
        val order: Order? = book.peek()
        var quantity = ""
        var price = ""
        if (order != null) {
            quantity = formatter.format(order.quantity)
            price = order.limitPrice.toString()
            book.remove() // consume element to maintain order
        }
        return if (padStart) {
            quantity = quantity.padStart(12)
            price = price.padStart(6)
            "$quantity $price"
        } else {
            // Note: Even though we should only use left padding,
            // here we have to use right padding to avoid having too many whitespaces
            // between the `|` operator in the final output. Since it will be closer
            // to the final image from the examples.
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
        return "trade ${aggressingOrder.id},${restingOrder.id},$price,$quantity"
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