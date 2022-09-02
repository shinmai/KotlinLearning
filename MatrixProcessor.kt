package processor

import java.util.*
import kotlin.math.abs
import kotlin.math.floor

typealias Matrix = Array<Array<Double>>

val scanner = Scanner(System.`in`).useLocale(Locale.US)

fun main() {
    while(true) {
        val userChoice = chooseFunction()
        if(userChoice == 0) break
        runFunction(userChoice)
    }
}

fun chooseFunction(): Int {
    print("1. Add matrices\n" +
            "2. Multiply matrix by a constant\n" +
            "3. Multiply matrices\n" +
            "4. Transpose matrix\n" +
            "5. Calculate a determinant\n" +
            "6. Inverse matrix\n" +
            "0. Exit\n" +
            "Your choice: ")
    return scanner.nextInt()
}

fun runFunction(function: Int) = when(function) {
    1 -> addMatrices()
    2 -> multiplyMatrixByConstant()
    3 -> multiplyMatrices()
    4 -> transposeMatrix()
    5 -> calculateDeterminant()
    6 -> invertMatrix()
    else -> throw NoWhenBranchMatchedException("Illegal function")
}

fun addMatrices() {
    val matrix1 = inputMatrix("Enter size of first matrix: ", "Enter first matrix:\n")
    val matrix2 = inputMatrix("Enter size of second matrix: ", "Enter second matrix:\n")
    matrix1.add(matrix2)
    println("The result is: ")
    matrix1.print()
    println()
}

fun multiplyMatrixByConstant() {
    val matrix1 = inputMatrix()
    print("Enter constant: ")
    val scalar = inputScalar()
    matrix1.multiply(scalar)
    println("The result is: ")
    matrix1.print()
}

fun multiplyMatrices() {
    val matrix1 = inputMatrix("Enter size of first matrix: ", "Enter first matrix:\n")
    val matrix2 = inputMatrix("Enter size of second matrix: ", "Enter second matrix:\n")
    val productMatrix = matrix1.multiply(matrix2)
    println("The result is: ")
    productMatrix.print()
}

fun transposeMatrix() {
    println()
    println("1. Main diagonal\n" +
            "2. Side diagonal\n" +
            "3. Vertical line\n" +
            "4. Horizontal line\n" +
            "Your choice: ")
    val axis = scanner.nextInt()
    val matrix = inputMatrix()
    val transposedMatrix: Matrix = when(axis) {
        1 -> matrix.transposeMainDiag()
        2 -> matrix.transposeSideDiag()
        3 -> matrix.transposeVertical()
        4 -> matrix.transposeHorizontal()
        else -> throw NoWhenBranchMatchedException("Illegal transposition axis")
    }
    println("The result is:")
    transposedMatrix.print()
}

fun calculateDeterminant() {
    val matrix = inputMatrix()
    println("The result is:")
    val result = determinant(matrix)
    println("${if(floor(result) !=result)result else result.toInt()}")
}

fun invertMatrix() {
    val matrix = inputMatrix()
    println("The result is:")
    matrix.inverse().print()
}

fun inputMatrix(sizePrompt: String = "Enter size of matrix: ", prompt: String = "Enter matrix:\n"): Matrix {
    print(sizePrompt)
    val sizeN = scanner.nextInt()
    val sizeM = scanner.nextInt()
    print(prompt)
    return Array(sizeN) { Array(sizeM) { scanner.nextDouble() } }
}

fun inputScalar(): Double = scanner.nextDouble()
fun zeroMatrix(n: Int, m: Int): Matrix =  Array(n) { Array(m) { 0.0 } }

fun Matrix.add(other: Matrix) {
    if(this.size != other.size || this[0].size != other[0].size) throw IllegalArgumentException("Matrix sizes need to match for addition")
    for(row in this.indices) for (col in this[0].indices)
            this[row][col] += other[row][col]
}

fun Matrix.add(other: Double) {
    for(row in this.indices) for (col in this[0].indices)
        this[row][col] += other
}

fun Matrix.multiply(other: Double) {
    for(row in this.indices) for (col in this[0].indices)
        this[row][col] *= other
}

fun Matrix.multiply(other: Matrix): Matrix {
    val returnMatrix = zeroMatrix(this.size, other[0].size)
    for (r in 0 until this.size)
        for (c in 0 until other[0].size) {
            for (i in 0 until this[0].size)
                returnMatrix[r][c] += this[r][i] * other[i][c];
        }
    return returnMatrix
}

fun Matrix.wrongMultiply(other: Matrix) {
    if(this.size != other.size || this[0].size != other[0].size) throw IllegalArgumentException("Matrix sizes need to match for multiplication")
    for(row in this.indices) for (col in this[0].indices)
        this[row][col] *= other[row][col]
}

fun Matrix.transposeMainDiag(): Matrix {
    val returnMatrix = zeroMatrix(this[0].size, this.size)
    for (row in indices)
        for (col in this[0].indices)
            returnMatrix[row][col] = this[col][row]
    return returnMatrix
}

fun Matrix.transposeSideDiag(): Matrix {
    val returnMatrix = zeroMatrix(this[0].size, this.size)

    for (col in 0 until this.size) for (row in 0 until this.size - col) {
        returnMatrix[row][col] = this[this.size - 1 - col][this.size - 1 - row]
        returnMatrix[this.size - 1 - col][this.size - 1 - row] = this[row][col]
    }

    return returnMatrix
}

fun Matrix.transposeVertical(): Matrix {
    val returnMatrix = zeroMatrix(this[0].size, this.size)
    for (row in indices)
        for (col in this[0].indices)
            returnMatrix[row][col] = this[row][this[0].size-1-col]
    return returnMatrix
}

fun Matrix.transposeHorizontal(): Matrix {
    val returnMatrix = zeroMatrix(this[0].size, this.size)
    for (row in indices)
        for (col in this[0].indices)
            returnMatrix[row][col] = this[this.size-1-row][col]
    return returnMatrix
}

fun Matrix.print() {
    var haveFractions=false
    for(row in this) for(cell in row)
        haveFractions = haveFractions || Math.floor(cell) != cell
    for(row in this) {
        for (cell in row)
            print("${if(haveFractions)cell else cell.toInt()} ")
        println()
    }
}

fun determinant(matrix: Matrix): Double {
    val n = matrix.size
    if (n == 1) return matrix[0][0]
    return if (n == 2) matrix[0][0] * matrix[1][1] - matrix[0][1] * matrix[1][0] else laplace(matrix)
}

fun laplace(m: Matrix): Double {
    val n = m.size

    // Base case is 3x3 determinant
    if (n == 3) {
        val a = m[0][0]
        val b = m[0][1]
        val c = m[0][2]
        val d = m[1][0]
        val e = m[1][1]
        val f = m[1][2]
        val g = m[2][0]
        val h = m[2][1]
        val i = m[2][2]
        return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    }
    var det = 0.0
    for (i in 0 until n) {
        for (j in 0 until n) {
            val c = m[i][j]
            if (abs(c) > 0.00000001) {
                val newMatrix = constructMatrix(m, j)
                val parity: Double = if (j and 1 == 0) + 1.0 else (-1).toDouble()
                det += (parity * c * laplace(newMatrix))
            }
        }
    }
    return det.toDouble()
}

fun constructMatrix(m: Matrix, skipColumn: Int): Matrix {
    val n = m.size
    val newMatrix = zeroMatrix(n-1, n-1)
    var ii = 0
    var i = 1
    while (i < n) {
        var jj = 0
        for (j in 0 until n) {
            if (j == skipColumn) continue
            val v = m[i][j]
            newMatrix[ii][jj++] = v
        }
        i++
        ii++
    }
    return newMatrix
}

fun Matrix.inverse(): Matrix {
    if (this.size != this[0].size) throw IllegalArgumentException("Matrix needs to be square to be inverted")
    val n = this.size
    val augmented = zeroMatrix(n, n*2)
    for (i in 0 until n) {
        for (j in 0 until n) augmented[i][j] = this[i][j]
        augmented[i][i + n] = 1.0
    }
    solve(augmented)
    val inv = zeroMatrix(n, n)
    for (i in 0 until n) for (j in 0 until n) inv[i][j] = augmented[i][j + n]
    return inv
}

fun solve(augmentedMatrix: Matrix) {
    val nRows = augmentedMatrix.size
    val nCols = augmentedMatrix[0].size
    var lead = 0
    for (r in 0 until nRows) {
        if (lead >= nCols) break
        var i = r
        while (abs(augmentedMatrix[i][lead]) < 0.00000001) {
            if (++i == nRows) {
                i = r
                if (++lead == nCols) return
            }
        }
        val temp = augmentedMatrix[r]
        augmentedMatrix[r] = augmentedMatrix[i]
        augmentedMatrix[i] = temp
        var lv = augmentedMatrix[r][lead]
        for (j in 0 until nCols) augmentedMatrix[r][j] /= lv
        i = 0
        while (i < nRows) {
            if (i != r) {
                lv = augmentedMatrix[i][lead]
                for (j in 0 until nCols) augmentedMatrix[i][j] -= lv * augmentedMatrix[r][j]
            }
            i++
        }
        lead++
    }
}