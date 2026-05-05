package com.diceroller.app

enum class DicePreset(val faces: Int, val displayName: String, val shapeType: Int) {
    D4(4, "D4", 0),
    D6(6, "D6", 1),
    D8(8, "D8", 2),
    D10(10, "D10", 3),
    D12(12, "D12", 4),
    D20(20, "D20", 5),
    D100(100, "D100", 6);

    companion object {
        fun fromFaces(faces: Int): DicePreset {
            return entries.find { it.faces == faces } ?: D6
        }
    }
}
