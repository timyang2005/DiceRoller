package com.diceroller.app

import kotlin.math.sqrt
import kotlin.random.Random

data class Dice(
    val faces: Int,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var vz: Float = 0f,
    var rotation: Float = 0f,
    var rotationSpeed: Float = 0f,
    var result: Int = 1,
    var isSettled: Boolean = false,
    var color: Int = 0xFFFFFFFF.toInt(),
    var size: Float = 80f,
    var bounceCount: Int = 0,
    var settleTimer: Float = 0f
) {
    val shapeType: Int
        get() = DicePreset.fromFaces(faces).shapeType

    fun randomResult() {
        result = Random.nextInt(1, faces + 1)
    }

    fun currentSpeed(): Float {
        return sqrt((vx * vx + vy * vy + vz * vz).toDouble()).toFloat()
    }

    fun isMoving(): Boolean {
        return currentSpeed() > 2f || rotationSpeed > 5f || z > 1f
    }

    fun getPolygonVertices(): FloatArray {
        val s = size / 2f
        return when (shapeType) {
            0 -> floatArrayOf(0f, -s, -s * 0.866f, s * 0.5f, s * 0.866f, s * 0.5f)
            1 -> floatArrayOf(-s, -s, s, -s, s, s, -s, s)
            2 -> floatArrayOf(0f, -s, s, 0f, 0f, s, -s, 0f)
            3 -> floatArrayOf(0f, -s, s * 0.95f, -s * 0.31f, s * 0.59f, -s * 0.81f, -s * 0.59f, -s * 0.81f, -s * 0.95f, -s * 0.31f)
            4 -> {
                val vertices = mutableListOf<Float>()
                for (i in 0 until 12) {
                    val angle = Math.toRadians(i * 30.0 - 90.0)
                    val r = if (i % 2 == 0) s else s * 0.8f
                    vertices.add((r * Math.cos(angle)).toFloat())
                    vertices.add((r * Math.sin(angle)).toFloat())
                }
                vertices.toFloatArray()
            }
            5 -> {
                val vertices = mutableListOf<Float>()
                for (i in 0 until 20) {
                    val angle = Math.toRadians(i * 18.0 - 90.0)
                    val r = if (i % 2 == 0) s else s * 0.85f
                    vertices.add((r * Math.cos(angle)).toFloat())
                    vertices.add((r * Math.sin(angle)).toFloat())
                }
                vertices.toFloatArray()
            }
            6 -> {
                val vertices = mutableListOf<Float>()
                for (i in 0 until 30) {
                    val angle = Math.toRadians(i * 12.0 - 90.0)
                    val r = if (i % 2 == 0) s else s * 0.92f
                    vertices.add((r * Math.cos(angle)).toFloat())
                    vertices.add((r * Math.sin(angle)).toFloat())
                }
                vertices.toFloatArray()
            }
            else -> floatArrayOf(-s, -s, s, -s, s, s, -s, s)
        }
    }
}
