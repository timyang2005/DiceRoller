package com.diceroller.app

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

class DicePhysicsEngine(
    private val tableWidth: Float,
    private val tableHeight: Float
) {
    companion object {
        const val GRAVITY = 2500f
        const val BOUNCE_DAMPING = 0.45f
        const val FRICTION = 0.97f
        const val ANGULAR_DAMPING = 0.96f
        const val SETTLE_SPEED_THRESHOLD = 3f
        const val SETTLE_ANGULAR_THRESHOLD = 8f
        const val SETTLE_TIME_REQUIRED = 0.3f
        const val WALL_BOUNCE = 0.5f
        const val MIN_BOUNCE_VZ = 30f
        const val MAX_INITIAL_VZ = 3000f
        const val MAX_INITIAL_VXY = 1500f
        const val MAX_ANGULAR_SPEED = 2000f
    }

    private val diceList = mutableListOf<Dice>()
    private var onSettleListener: ((List<Dice>) -> Unit)? = null
    private var onBounceListener: ((Dice) -> Unit)? = null
    private var allSettled = false

    fun setOnSettleListener(listener: (List<Dice>) -> Unit) {
        onSettleListener = listener
    }

    fun setOnBounceListener(listener: (Dice) -> Unit) {
        onBounceListener = listener
    }

    fun launchDice(
        count: Int,
        faces: Int,
        color: Int,
        diceSize: Float,
        forceMultiplier: Float
    ) {
        diceList.clear()
        allSettled = false

        val clampedForce = forceMultiplier.coerceIn(0.2f, 3.0f)

        for (i in 0 until count) {
            val dice = Dice(
                faces = faces,
                x = tableWidth / 2f + Random.nextFloat() * 40f - 20f,
                y = tableHeight / 2f + Random.nextFloat() * 40f - 20f,
                z = 0f,
                color = color,
                size = diceSize
            )
            dice.randomResult()

            val baseVZ = 800f + clampedForce * 700f
            dice.vz = baseVZ + Random.nextFloat() * 400f * clampedForce
            dice.vz = dice.vz.coerceAtMost(MAX_INITIAL_VZ)

            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = (200f + Random.nextFloat() * 300f) * clampedForce
            dice.vx = (Math.cos(angle.toDouble()).toFloat() * speed).coerceIn(-MAX_INITIAL_VXY, MAX_INITIAL_VXY)
            dice.vy = (Math.sin(angle.toDouble()).toFloat() * speed).coerceIn(-MAX_INITIAL_VXY, MAX_INITIAL_VXY)

            dice.rotationSpeed = (Random.nextFloat() * 800f + 400f) * clampedForce *
                    if (Random.nextBoolean()) 1f else -1f
            dice.rotationSpeed = dice.rotationSpeed.coerceIn(-MAX_ANGULAR_SPEED, MAX_ANGULAR_SPEED)

            dice.isSettled = false
            dice.bounceCount = 0
            dice.settleTimer = 0f

            diceList.add(dice)
        }
    }

    fun update(dt: Float) {
        if (allSettled) return

        val clampedDt = dt.coerceIn(0.001f, 0.033f)

        for (dice in diceList) {
            if (dice.isSettled) continue

            dice.vz -= GRAVITY * clampedDt
            dice.x += dice.vx * clampedDt
            dice.y += dice.vy * clampedDt
            dice.z += dice.vz * clampedDt
            dice.rotation += dice.rotationSpeed * clampedDt

            if (dice.z <= 0f) {
                dice.z = 0f
                if (abs(dice.vz) > MIN_BOUNCE_VZ) {
                    dice.vz = -dice.vz * BOUNCE_DAMPING
                    dice.bounceCount++
                    dice.vx *= 0.85f
                    dice.vy *= 0.85f
                    dice.rotationSpeed *= 0.8f
                    onBounceListener?.invoke(dice)
                } else {
                    dice.vz = 0f
                }
            }

            val halfSize = dice.size / 2f
            if (dice.x - halfSize < 0f) {
                dice.x = halfSize
                dice.vx = abs(dice.vx) * WALL_BOUNCE
                dice.rotationSpeed *= 0.9f
            }
            if (dice.x + halfSize > tableWidth) {
                dice.x = tableWidth - halfSize
                dice.vx = -abs(dice.vx) * WALL_BOUNCE
                dice.rotationSpeed *= 0.9f
            }
            if (dice.y - halfSize < 0f) {
                dice.y = halfSize
                dice.vy = abs(dice.vy) * WALL_BOUNCE
                dice.rotationSpeed *= 0.9f
            }
            if (dice.y + halfSize > tableHeight) {
                dice.y = tableHeight - halfSize
                dice.vy = -abs(dice.vy) * WALL_BOUNCE
                dice.rotationSpeed *= 0.9f
            }

            if (dice.z <= 0f) {
                dice.vx *= FRICTION
                dice.vy *= FRICTION
                dice.rotationSpeed *= ANGULAR_DAMPING
            }

            if (!dice.isMoving()) {
                dice.settleTimer += clampedDt
                if (dice.settleTimer >= SETTLE_TIME_REQUIRED) {
                    dice.isSettled = true
                    dice.vx = 0f
                    dice.vy = 0f
                    dice.vz = 0f
                    dice.z = 0f
                    dice.rotationSpeed = 0f
                    dice.randomResult()
                }
            } else {
                dice.settleTimer = 0f
            }
        }

        resolveDiceCollisions()

        if (diceList.isNotEmpty() && diceList.all { it.isSettled }) {
            allSettled = true
            onSettleListener?.invoke(diceList.toList())
        }
    }

    private fun resolveDiceCollisions() {
        for (i in diceList.indices) {
            for (j in i + 1 until diceList.size) {
                val a = diceList[i]
                val b = diceList[j]
                if (a.isSettled && b.isSettled) continue

                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val minDist = (a.size + b.size) / 2f

                if (dist < minDist && dist > 0.01f) {
                    val overlap = minDist - dist
                    val nx = dx / dist
                    val ny = dy / dist

                    if (!a.isSettled && !b.isSettled) {
                        a.x -= nx * overlap * 0.5f
                        a.y -= ny * overlap * 0.5f
                        b.x += nx * overlap * 0.5f
                        b.y += ny * overlap * 0.5f
                    } else if (a.isSettled) {
                        b.x += nx * overlap
                        b.y += ny * overlap
                    } else {
                        a.x -= nx * overlap
                        a.y -= ny * overlap
                    }

                    val relVx = a.vx - b.vx
                    val relVy = a.vy - b.vy
                    val relVn = relVx * nx + relVy * ny

                    if (relVn > 0) {
                        val impulse = relVn * 0.5f
                        if (!a.isSettled) {
                            a.vx -= impulse * nx
                            a.vy -= impulse * ny
                        }
                        if (!b.isSettled) {
                            b.vx += impulse * nx
                            b.vy += impulse * ny
                        }
                        a.rotationSpeed += Random.nextFloat() * 100f - 50f
                        b.rotationSpeed += Random.nextFloat() * 100f - 50f
                    }
                }
            }
        }
    }

    fun getDice(): List<Dice> = diceList.toList()

    fun isAllSettled(): Boolean = allSettled

    fun clear() {
        diceList.clear()
        allSettled = false
    }
}
