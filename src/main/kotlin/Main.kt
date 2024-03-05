package org.example.bitvavo.jvm

fun main() {
    var order: String? = readlnOrNull()
        ?: throw IllegalArgumentException("Input is empty")
    var line = 1
    while (order != null) {
        require(order.isNotBlank()) { "Input line $line is empty" }
        placeOrder(order)
        order = readlnOrNull()
        line++
    }
    val output = exchange.getOrderBookOutput()
    if (output.isBlank()) return
    println(output)
}

val exchange = Exchange().apply {
    invokeOnTrade { println(it.tradeOutput()) }
}

// This parser follows a strict format, and in any other case
// it is throwing an `IllegalArgumentException` with the corresponding message.
// Format: "order-id,side,price,quantity".
// Note that the values are separated with a comma.
fun placeOrder(line: String) {
    val inputs = line.split(',')
    require(inputs.size == 4) {
        "The expected number of fields are 4, but got:${inputs.size}"
    }
    val orderId = inputs[0]
    val side = inputs[1]
    val price = inputs[2].toIntOrNull()
        ?: throw IllegalArgumentException(
            "Price field is expected to be type of int but got:${inputs[2]}"
        )
    val quantity = inputs[3].toIntOrNull()
        ?: throw IllegalArgumentException(
            "Quantity field is expected to be type of int but got:${inputs[3]}"
        )
    when (side) {
        "B" -> {
            val order = BuyOrder(id = orderId, limitPrice = price, quantity = quantity)
            exchange.placeBuyOrder(order)
        }

        "S" -> {
            val order = SellOrder(id = orderId, limitPrice = price, quantity = quantity)
            exchange.placeSellOrder(order)
        }

        else -> throw IllegalArgumentException(
            "Side field is expected to be value of either `B` or `S`, but got:$side"
        )
    }
}