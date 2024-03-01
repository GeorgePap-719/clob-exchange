package org.example.bitvavo.jvm

import java.util.Scanner

fun main() {
    println("Hello World!")
//    val input = readln()
    val scanner = Scanner(System.`in`)
    val input = scanner.nextLine()
    println(input + "readln succeeded")
}