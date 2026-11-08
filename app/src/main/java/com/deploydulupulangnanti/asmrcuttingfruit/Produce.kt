package com.deploydulupulangnanti.asmrcuttingfruit

data class Produce(val id: String, val name: String, val note: String, val vegetable: Boolean, val color: Int, val sound: Int)

object Pantry {
    val items = listOf(
        Produce("watermelon", "Watermelon", "Juicy & mellow", false, 0xFFE76460.toInt(), 0),
        Produce("kiwi", "Kiwi", "Soft & delicate", false, 0xFF8DA65A.toInt(), 1),
        Produce("orange", "Orange", "Fresh & zesty", false, 0xFFDF9C43.toInt(), 0),
        Produce("apple", "Apple", "Crisp & bright", false, 0xFFC86452.toInt(), 2),
        Produce("strawberry", "Strawberry", "Small & sweet", false, 0xFFC95B60.toInt(), 1),
        Produce("dragonfruit", "Dragon fruit", "Soft & speckled", false, 0xFFC46B94.toInt(), 1),
        Produce("cucumber", "Cucumber", "Cool & crunchy", true, 0xFF799461.toInt(), 2),
        Produce("carrot", "Carrot", "A satisfying snap", true, 0xFFD88C45.toInt(), 2),
        Produce("tomato", "Tomato", "Gentle & juicy", true, 0xFFD4624D.toInt(), 0),
        Produce("avocado", "Avocado", "Smooth & buttery", false, 0xFF91A269.toInt(), 1),
        Produce("lemon", "Lemon", "A little sunshine", false, 0xFFD1AF44.toInt(), 0),
        Produce("eggplant", "Eggplant", "Tender & earthy", true, 0xFF8A7199.toInt(), 1)
    )
    fun dailyIndices(day: Long): List<Int> = items.indices.shuffled(kotlin.random.Random(day)).take(5)
}

enum class Mode(val label: String, val detail: String) {
    FREE("Free Slice", "No timer. Just a little calm."),
    FOCUS("Focus Session", "One quiet minute, just for you."),
    DAILY("Daily Basket", "Five fresh picks. A new basket each day.")
}

fun formatRemaining(milliseconds: Long): String {
    val seconds=(milliseconds.coerceAtLeast(0)+999)/1000
    return String.format(java.util.Locale.US,"%d:%02d",seconds/60,seconds%60)
}
