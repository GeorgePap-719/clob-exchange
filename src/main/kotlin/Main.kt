package org.example.bitvavo.jvm

fun main() {
    println("Hello World!")
    val input = readln()
    println(input + "readln succeeded")

    val exchange = Exchange()
    exchange.invokeOnTrade { println(it) }

}