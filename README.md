# Central Limit Order Book

Represents an exchange, type of clob, where orders are matched using `price time priority`.
This means orders are matched by the order of price and then by the arrival time in the book.

The program accepts orders from the standard input.

To run it through gradle use `./gradlew run < test1.txt`

To run the program through script use `./exchage < test1.txt`