package connectfour

const val MIN_DIM = 5
const val MAX_DIM = 9

fun main() {
    val game = ConnectFour()
    game.startGame()
}

enum class Direction { HORIZONTAL, VERTICAL, DIAGONAL_DOWN_1, DIAGONAL_DOWN_2, DIAGONAL_UP_1, DIAGONAL_UP_2 }

class ConnectFour {
    private var height = 6
    private var width = 7
    private var firstPlayer = ""
    private var secondPlayer = ""
    private val whitespaceRe = Regex("\\s")
    private var firstPlayersTurn = true
    private var firstPlayersStarts = true
    private var board: Array<Array<Char>> = arrayOf()
    private var running = false
    private var gamesToRun = -1
    private var currentGame = 1
    private var firstPlayerScore = 0
    private var secondPlayerScore = 0

    fun startGame() {
        println("Connect Four\n" +
                "First player's name:")
        firstPlayer = readln()
        println("Second player's name: ")
        secondPlayer = readln()

        defineBoard()

        do {
            println("Do you want to play single or multiple games?\n" +
                    "For a single game, input 1 or press Enter\n" +
                    "Input a number of games:")
            val gameNumberInput = readln()
            if(gameNumberInput.isEmpty()) {
                gamesToRun=1
                break
            }
            gamesToRun = try { gameNumberInput.toInt() } catch (e: NumberFormatException) {
                println("Invalid input")
                continue
            }
            if(gamesToRun<1) println("Invalid input")
        } while(gamesToRun<1)

        println("$firstPlayer VS $secondPlayer\n$height X $width board")
        if(gamesToRun > 1) println("Total $gamesToRun games")

        do {
            clearBoard()
            if(gamesToRun > 1) println("Game #$currentGame")
            else println("Single game")

            firstPlayersTurn=firstPlayersStarts
            runGame()

            if(gamesToRun > 1) println("Score\n$firstPlayer: $firstPlayerScore $secondPlayer: $secondPlayerScore")
            firstPlayersStarts=!firstPlayersStarts
        } while(currentGame++ < gamesToRun)
        println("Game over!")
    }

    private fun defineBoard() {
        var found = false
        do {
            println("Set the board dimensions (Rows x Columns)\n" +
                    "Press Enter for default (6 x 7)")
            val dimensionsInput = readln()
            val dimensions=dimensionsInput.lowercase().replace(whitespaceRe, "").split("x")
            if(dimensionsInput.isBlank()) {
                width = 7
                height = 6
                found = true
            } else if(dimensions.size!=2) println("Invalid input")
            else {
                val (heightInput, widthInput) = try { dimensions.map(String::toInt) } catch (e: NumberFormatException) {
                    println("Invalid input")
                    continue
                }

                if(widthInput !in MIN_DIM..MAX_DIM) println("Board columns should be from 5 to 9")
                else if(heightInput !in MIN_DIM..MAX_DIM) println("Board rows should be from 5 to 9")
                else {
                    width=widthInput
                    height=heightInput
                    found = true
                }
            }
        } while(!found)
    }

    private fun clearBoard() {
        board = arrayOf()
        for (y in 0..height) {
            var array = arrayOf<Char>()
            for (x in 0..width) {
                array += ' '
            }
            board += array
        }
    }

    private fun runGame() {
        printBoard()
        running=true
        do {
            handleTurn()
        } while(running)
    }

    private fun getColumnTopPosition(column: Int): Int {
        var y = 0
        while(y <= height) {
            if(board[y][column] != ' ') return y-1
            y++
        }
        return height
    }

    private fun handleTurn() {
        println("${if(firstPlayersTurn) firstPlayer else secondPlayer}'s turn:")
        val columnInput = readln()
        if(columnInput.lowercase() == "end") {
            gamesToRun=-1
            running = false
            return
        }
        val column = try { columnInput.toInt() } catch (e: NumberFormatException) { return println("Incorrect column number") }
        if(column !in 1..width) return println("The column number is out of range (1 - $width)")
        val topPosition = getColumnTopPosition(column)
        if(topPosition == 0) return println("Column $column is full")
        board[topPosition][column]=(if(firstPlayersTurn) 'o' else '*')
        firstPlayersTurn=!firstPlayersTurn
        printBoard()
        checkWinCondition()
    }

    private fun printBoard() {
        for(i in 1..width) print(" $i")
        println()
        for(y in 1..height) {
            print('║')
            for (x in 1..width) print("${board[y][x]}║")
            println()
        }
        print('╚')
        for(i in 2..width*2) print(if(i%2==0) '═' else '╩')
        println('╝')
    }

    @Throws(Exception::class)
    private fun checkForWinInDirection(range1start: Int, range1end: Int, range2end: Int, direction: Direction): Boolean {
        var runLength: Int
        var runChar: Char
        loop@ for (range1i in range1start..range1end) {
            runLength = 0
            runChar = ' '
            for (range2i in 1..range2end) {
                val curV: Char = when (direction) {
                    Direction.HORIZONTAL -> board[range1i][range2i]
                    Direction.VERTICAL -> board[range2i][range1i]
                    Direction.DIAGONAL_DOWN_1 -> {
                        if(range1i + (range2i - 1) > height) continue@loop
                        board[range1i + (range2i - 1)][range2i]
                    }
                    Direction.DIAGONAL_DOWN_2 -> {
                        if (range1i + (range2i - 1) > width) continue@loop
                        board[range2i][range1i + (range2i - 1)]
                    }
                    Direction.DIAGONAL_UP_1 -> {
                        if(range1i - (range2i - 1) < 1) continue@loop
                        board[range1i - (range2i - 1)][range2i]
                    }
                    Direction.DIAGONAL_UP_2 -> {
                        if(range1i + (range2i - 1) > width) continue@loop
                        board[height - (range2i - 1)][range1i + (range2i - 1)]
                    }
                }
                if (curV == runChar) runLength++
                else {
                    runChar = curV
                    runLength = 1
                }
                if (runLength >= 4 && runChar != ' ') {
                    running=false
                    println("Player ${if(runChar=='o') firstPlayer else secondPlayer} won")
                    if(runChar=='o') firstPlayerScore += 2
                    else secondPlayerScore += 2
                    return true
                }
            }
        }
        return false
    }

    private fun checkWinCondition() {
        if(checkForWinInDirection(1, height, width, Direction.HORIZONTAL)) return
        if(checkForWinInDirection(1, width, height, Direction.VERTICAL)) return
        if(checkForWinInDirection(1, height-3, width, Direction.DIAGONAL_DOWN_1)) return
        if(checkForWinInDirection(2, width-3, height, Direction.DIAGONAL_DOWN_2)) return
        if(checkForWinInDirection(4, height, width, Direction.DIAGONAL_UP_1)) return
        if(checkForWinInDirection(2, width-3, height, Direction.DIAGONAL_UP_2)) return

        // draw
        for(i in 1..width)  if(getColumnTopPosition(i) != 0) return
        running = false
        println("It is a draw")
        firstPlayerScore++
        secondPlayerScore++
    }

}