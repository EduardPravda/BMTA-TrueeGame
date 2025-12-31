package cz.pravda.trueegame.data

data class MemoryCard(
    val id: Int,
    val imageId: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)