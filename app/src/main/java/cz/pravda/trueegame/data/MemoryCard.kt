package cz.pravda.trueegame.data

// Datová třída reprezentující jednu kartičku
data class MemoryCard(
    val id: Int,             // Unikátní ID (abychom rozeznali dvě stejné karty)
    val imageId: Int,        // ID obrázku (R.drawable.neco), který je na líci
    var isFaceUp: Boolean = false, // Je otočená obrázkem nahoru?
    var isMatched: Boolean = false // Je už spárovaná (uhodnutá)?
)