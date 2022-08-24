package tasklist

import kotlinx.datetime.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.lang.reflect.ParameterizedType

val tasks = mutableListOf<Task>()

data class Task(var task: String, var due: LocalDateTime, var priority: String)

const val HEADER = "+----+------------+-------+---+---+--------------------------------------------+\n| N  |    Date    | Time  | P | D |                   Task                     |\n+----+------------+-------+---+---+--------------------------------------------+"
const val DIVIDER = "+----+------------+-------+---+---+--------------------------------------------+"
const val INFOLINE_FMT = "| %-2d | %s | %s | %s |%-44s|"
const val LINE_FMT = "|    |            |       |   |   |%-44s|"
const val DATE_RE_STR = "\\d{4}-[01]?\\d-[0-3]?\\d"
const val TIME_RE_STR = "[0-2]?\\d:[0-5]?\\d"

const val BLUE = "\u001b[104m \u001B[0m"
const val GREEN = "\u001b[102m \u001B[0m"
const val YELLOW = "\u001b[103m \u001B[0m"
const val RED = "\u001b[101m \u001B[0m"

val dateRe = Regex(DATE_RE_STR)
val timeRe = Regex(TIME_RE_STR)

class LocalDateTimeAdapter {
    @ToJson fun toJson(objectToSeialize: LocalDateTime): String = objectToSeialize.toString()
    @FromJson fun fromJson(JSONString: String): LocalDateTime =  LocalDateTime.parse(JSONString)
}

fun main() {
    val jsonFile = File("tasklist.json")
    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .add(LocalDateTimeAdapter())
        .build()
    val type: ParameterizedType = Types.newParameterizedType(List::class.java, Task::class.java)
    val taskListAdapter: JsonAdapter<List<Task>> = moshi.adapter(type)
    if(jsonFile.exists())
        taskListAdapter.fromJson(jsonFile.readText())?.let { tasks.addAll(it) }

    while(true) {
        println("Input an action (add, print, edit, delete, end):")
        when(readln()) {
            "add" -> addNewTask()
            "print" -> printTasks()
            "edit" -> editTask()
            "delete" -> deleteTask()
            "end" -> break
            else -> print("The input action is invalid")
        }
    }
    println("Tasklist exiting!")
    jsonFile.writeText(taskListAdapter.toJson(tasks))
}

fun getPriority(): String = getValidatedInput("Input the task priority (C, H, N, L):",
        processData = fun(input: String): String { return input.trim().uppercase() },
        checkData = fun(input: String): Boolean = when(input) {
            "C", "H", "N", "L" -> true
            else -> false
        })

fun getDate(): List<Int> = getValidatedInput("Input the date (yyyy-mm-dd):",
        processData = fun(input: String): String = input.trim(),
        checkData = fun(input: String): Boolean {
            if(!dateRe.matches(input)) {
                println("The input date is invalid")
                return false
            }
            try {
                val (y, m, d) = input.split("-").map{ it.toInt() }
                LocalDate(y, m, d)
            } catch(e: IllegalArgumentException) {
                println("The input date is invalid")
                return false
            }
            return true
        }
    ).split("-").map{ it.toInt() }

fun getTime(): List<Int> = getValidatedInput("Input the time (hh:mm):",
    processData = fun(input: String): String = input.trim(),
    checkData = fun(input: String): Boolean {
        if(!timeRe.matches(input)) {
            println("The input time is invalid")
            return false
        }
        try {
            val (h, m) = input.split(":").map{ it.toInt() }
            LocalDateTime(2000, 1, 1, h, m)
        } catch(e: IllegalArgumentException) {
            println("The input time is invalid")
            return false
        }
        return true
    }
).split(":").map{ it.toInt() }

fun getDateTime(): LocalDateTime {
    val (dueYear, dueMonth, dueDay) = getDate()
    val (dueHour, dueMinute) = getTime()
    return LocalDateTime(dueYear, dueMonth, dueDay, dueHour, dueMinute)
}

fun getDateForDT(dt: LocalDateTime): LocalDateTime {
    val (dueYear, dueMonth, dueDay) = getDate()
    return LocalDateTime(dueYear, dueMonth, dueDay, dt.hour, dt.minute)
}

fun getTimeForDT(dt: LocalDateTime): LocalDateTime {
    val (dueHour, dueMinute) = getTime()
    return LocalDateTime(dt.year, dt.month, dt.dayOfMonth, dueHour, dueMinute)
}

fun getTaskString(): String {
    println("Input a new task (enter a blank line to end):")
    var taskString = ""
    while(true) {
        val line = readln().trim()
        if(line.isEmpty()) break
        taskString+=if(taskString.isEmpty()) line else "\n$line"
    }
    return taskString
}

fun addNewTask() {
    val priority = getPriority()
    val dueDT = getDateTime()
    val taskString = getTaskString()

    if(taskString.isEmpty()) println("The task is blank")
    else tasks.add(Task(taskString, dueDT, priority))
}

fun getPriority4bit(priority: String): String = when(priority) {
    "C" -> RED
    "H" -> YELLOW
    "N" -> GREEN
    "L" -> BLUE
    else -> priority
}

fun getDue4bit(due: String): String = when(due) {
    "I" -> GREEN
    "T" -> YELLOW
    "O" -> RED
    else -> due
}

fun printTasks() {
    if(tasks.isEmpty()) println("No tasks have been input")
    else {
        println(HEADER)
        tasks.forEachIndexed { index, task -> printTask(index, task) }
    }
}

fun printTask(index: Int, task: Task) {
    var first = true
    for(line in task.task.lines().map{ it.chunked(44) }.flatten()) {
        if(first) {
            println(String.format(
                INFOLINE_FMT,
                index+1,
                task.due.toString().replaceFirst("T", " | "),
                getPriority4bit(task.priority),
                getDue4bit(dueTag(task.due)),
                line
            ))
            first = false
        } else
        println(String.format(LINE_FMT, line))
    }

    println(DIVIDER)
}

fun dueTag(dueDT: LocalDateTime): String {
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.of("UTC+0")).date
    val numberOfDays = currentDate.daysUntil(dueDT.date)
    return when {
        numberOfDays == 0 -> "T"
        numberOfDays > 0 -> "I"
        else -> "O"
    }
}

fun deleteTask() {
    printTasks()
    if(tasks.size == 0) return
    val taskNumber = getValidatedInput("Input the task number (1-${tasks.size}):",
        checkData = fun(input: String): Boolean {
            try {
                val inputInt = input.toInt()
                if(inputInt !in 1..tasks.size) {
                    println("Invalid task number")
                    return false
                }
            } catch(e: IllegalArgumentException) {
                println("Invalid task number")
                return false
            }
            return true
        }).toInt()-1
    tasks.removeAt(taskNumber)
    println("The task is deleted")
}

fun editTask() {
    printTasks()
    if(tasks.size == 0) return
    val taskNumber = getValidatedInput("Input the task number (1-${tasks.size}):",
        checkData = fun(input: String): Boolean {
            try {
                val inputInt = input.toInt()
                if(inputInt !in 1..tasks.size) {
                    println("Invalid task number")
                    return false
                }
            } catch(e: IllegalArgumentException) {
                println("Invalid task number")
                return false
            }
            return true
        }).toInt()-1

    val field = getValidatedInput("Input a field to edit (priority, date, time, task): ",
        processData = fun(input: String): String { return input.lowercase() },
        checkData = fun(input: String): Boolean {
            return when(input) {
                "priority", "date", "time", "task" -> true
                else -> {
                    println("Invalid field")
                    false
                }
            }
        })

    val target = tasks[taskNumber]

    when(field) {
        "priority" -> target.priority = getPriority()
        "date" -> target.due = getDateForDT(target.due)
        "time" -> target.due = getTimeForDT(target.due)
        "task" -> target.task = getTaskString()
    }
    println("The task is changed")
}

fun getValidatedInput(prompt: String, checkData: (String) -> Boolean, processData: ((String) -> String) ? = null): String {
    while(true) {
        println(prompt)
        var line = readln()
        if(processData!=null) line=processData(line)
        if(checkData(line)) return line
    }
}