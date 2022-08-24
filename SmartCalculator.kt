package calculator

import kotlin.math.pow
import java.math.BigInteger

enum class Function { NOP, COMMAND, CALCULATE }

abstract class Element
class NumberElement(val value: BigInteger) : Element()
class VariableElement(val name: String) : Element()
class OperatorElement(val operator: Operator) : Element()
class AssignmentElement(val name: String) : Element()

class InvalidIdentifierException(message: String = "") : Exception(message)
class InvalidAssignmentException(message: String = "") : Exception(message)
class UnknownVariableException(message: String = "") : Exception(message)
class InvalidExpressionException(message: String = "") : Exception(message)

var running = true
val variables = mutableMapOf<String, BigInteger>()

fun main() {
    do {
        val input = readln()
        when(chooseFunction(input)) {
            Function.NOP -> continue
            Function.COMMAND -> processCommand(input)
            Function.CALCULATE -> {
                try {
                    val result = calculateRPN(input)
                    if(result.isNotBlank())
                        println(result)
                } catch(e: InvalidIdentifierException) { println("Invalid identifier") }
                catch(e: InvalidAssignmentException) { println("Invalid assignment") }
                catch(e: UnknownVariableException) { println("Unknown variable") }
                catch(e: InvalidExpressionException) { println("Invalid expression") }
                catch(e: Exception) { println("Unhandled exception:\n${e.stackTraceToString()}") }
            }
        }
    } while(running)
}

fun chooseFunction(input: String): Function = when {
    input.isEmpty() -> Function.NOP
    input.first() == '/' -> Function.COMMAND
    else -> Function.CALCULATE
}

fun processCommand(input: String) = when(input) {
    "/help" -> println("The program calculates addition and subtraction of number.\nuse /exit to exit")
    "/exit" -> {
        println("Bye!")
        running = false
    }
    else -> println("Unknown command")
}

fun calculateRPN(input: String): String {
    val rpnStack = ArrayDeque<BigInteger>()
    for(element in shuntingYard(input)) {
        when (element) {
            is NumberElement -> rpnStack.addLast(element.value)
            is OperatorElement -> {
                try {
                    val secondNumber = rpnStack.removeLast()
                    val firstNumber = rpnStack.removeLast()
                    when (element.operator) {
                        Operator.ADD -> rpnStack.addLast(firstNumber + secondNumber)
                        Operator.SUB -> rpnStack.addLast(firstNumber - secondNumber)
                        Operator.MUL -> rpnStack.addLast(firstNumber * secondNumber)
                        Operator.DIV -> rpnStack.addLast(firstNumber / secondNumber)
                        Operator.LP -> throw TypeCastException()
                        Operator.RP -> throw TypeCastException()
                        Operator.EXP -> rpnStack.addLast(firstNumber.toDouble().pow(secondNumber.toDouble()).toBigDecimal().toBigInteger())
                    }
                } catch (e: NoSuchElementException) { throw InvalidExpressionException() }
            }
            is AssignmentElement -> {
                if(rpnStack.size != 1) throw InvalidAssignmentException()
                variables[element.name] = rpnStack.removeLast()
                return ""
            }
        }
    }
    if(rpnStack.size != 1) throw InvalidExpressionException()
    return rpnStack.removeLast().toString()
}

enum class Operator { ADD, SUB, MUL, DIV, LP, RP, EXP }

fun shuntingYard(originalInput: String): ArrayDeque<Element> {
    val opStack = ArrayDeque<OperatorElement>()
    val outputQueue = ArrayDeque<Element>()

    // barebones syntax check
    if(Regex("[^-a-zA-Z\\d+*/()^=\\s]]").containsMatchIn(originalInput)) throw InvalidExpressionException()

    // assignment handling
    val isAssignment=originalInput.contains("=")
    if(isAssignment && originalInput.count { it == '=' } !=1) throw InvalidAssignmentException()
    if(isAssignment && !Regex("^\\s*[a-zA-Z]+\\s*=").containsMatchIn(originalInput)) throw InvalidAssignmentException()
    val assignmentVarName = if(isAssignment) Regex("\\s*([a-zA-Z]+)\\s*=").find(originalInput, 0)!!.groupValues[1] else null
    val input = originalInput.replace(Regex("\\s*[a-zA-Z]+\\s*="), "")

    val elements = Regex("[+-]?\\d+|\\++|-+|[*/()^]|[a-zA-Z]+").findAll(input).map{ it.groupValues[0] }
    for(element in elements) {
        val newElement = when {
            Regex("[+-]?\\d+").matches(element) -> NumberElement(element.toBigInteger())
            Regex("[a-zA-Z]+").matches(element) -> VariableElement(element)
            Regex("[-+*/()^]+").matches(element) -> OperatorElement(
                    when(element.first()) {
                        '+' -> Operator.ADD
                        '-' -> if(element.length % 2 != 0) Operator.SUB else Operator.ADD
                        '*' -> Operator.MUL
                        '/' -> Operator.DIV
                        '(' -> Operator.LP
                        ')' -> Operator.RP
                        '^' -> Operator.EXP
                        else -> throw if(isAssignment) InvalidAssignmentException() else UnsupportedOperationException()
                    }
            )
            else -> throw if(isAssignment) InvalidAssignmentException() else InvalidExpressionException()
        }
        when(newElement) {
            is NumberElement -> outputQueue.addLast(newElement)
            is OperatorElement -> {
                if (newElement.operator == Operator.RP) {
                    if(opStack.size < 1) throw InvalidExpressionException()
                    while (opStack.size > 0 && opStack.last().operator != Operator.LP)
                        outputQueue.addLast(opStack.removeLast())
                    if(opStack.size < 1 || opStack.last().operator != Operator.LP) throw InvalidExpressionException()
                    opStack.removeLast()
                } else {
                    while (newElement.operator != Operator.LP && opStack.size > 0 && opStack.last().operator != Operator.LP && newElement.precedence(opStack.last()) >= 0)
                        outputQueue.addLast(opStack.removeLast())
                    opStack.addLast(newElement)
                }
            }
            is VariableElement -> {
                val value = variables[newElement.name] ?: throw if(isAssignment) InvalidAssignmentException() else UnknownVariableException()
                outputQueue.addLast(NumberElement(value))
            }
        }
    }
    while(opStack.size > 0) if(opStack.last().operator != Operator.LP) outputQueue.addLast(opStack.removeLast()) else throw if(isAssignment) InvalidAssignmentException() else InvalidExpressionException()
    if(isAssignment && assignmentVarName != null) outputQueue.addLast(AssignmentElement(assignmentVarName))
    return outputQueue
}

fun OperatorElement.precedence(other: OperatorElement): Int = other.precedenceValue() - this.precedenceValue()

fun OperatorElement.precedenceValue(): Int = when(this.operator) {
    Operator.ADD -> 1
    Operator.SUB -> 1
    Operator.MUL -> 2
    Operator.DIV -> 2
    Operator.LP -> throw TypeCastException()
    Operator.RP -> throw TypeCastException()
    Operator.EXP -> 3
}