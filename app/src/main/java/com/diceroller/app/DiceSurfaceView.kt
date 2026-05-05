package com.diceroller.app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.SurfaceView

class DiceSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr) {

    private var physicsEngine: DicePhysicsEngine? = null
    private var renderThread: RenderThread? = null
    private var isRunning = false

    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2D5A27"); style = Paint.Style.FILL }
    private val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1B3A15"); style = Paint.Style.STROKE; strokeWidth = 8f }
    private val tableFeltPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3A7A32"); style = Paint.Style.FILL }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1A000000"); style = Paint.Style.FILL }
    private val dicePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val diceStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = Color.parseColor("#333333") }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#222222"); style = Paint.Style.FILL; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val forceIndicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val forceIndicatorBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.parseColor("#33000000") }

    private var forceMultiplier = 1.0f
    private var showForceIndicator = false

    fun setPhysicsEngine(engine: DicePhysicsEngine) { physicsEngine = engine }
    fun setForceMultiplier(force: Float, show: Boolean = true) { forceMultiplier = force; showForceIndicator = show }

    fun startRendering() { if (isRunning) return; isRunning = true; renderThread = RenderThread(); renderThread?.start() }
    fun stopRendering() { isRunning = false; renderThread?.let { it.interrupt(); try { it.join(500) } catch (_: InterruptedException) {} }; renderThread = null }

    private inner class RenderThread : Thread() {
        private var lastTime = System.nanoTime()
        override fun run() {
            while (isRunning) {
                val canvas: Canvas? = try { holder.lockCanvas() } catch (_: Exception) { null }
                if (canvas != null) {
                    val now = System.nanoTime(); val dt = (now - lastTime) / 1_000_000_000f; lastTime = now
                    physicsEngine?.update(dt); drawFrame(canvas)
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) { break }
                } else { try { Thread.sleep(16) } catch (_: InterruptedException) { break } }
            }
        }
    }

    private fun drawFrame(canvas: Canvas) {
        val w = canvas.width.toFloat(); val h = canvas.height.toFloat()
        drawTable(canvas, w, h)
        val engine = physicsEngine ?: return
        val diceList = engine.getDice().sortedByDescending { it.z }
        for (dice in diceList) { drawDice(canvas, dice) }
        if (showForceIndicator && !engine.isAllSettled() && diceList.isNotEmpty()) { drawForceIndicator(canvas, w, h) }
    }

    private fun drawTable(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRect(0f, 0f, w, h, tablePaint)
        val margin = 20f
        canvas.drawRoundRect(RectF(margin, margin, w - margin, h - margin), 16f, 16f, tableFeltPaint)
        canvas.drawRoundRect(RectF(margin, margin, w - margin, h - margin), 16f, 16f, tableBorderPaint)
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2A6A22"); style = Paint.Style.STROKE; strokeWidth = 1f; pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) }
        canvas.drawLine(w / 2f, margin + 20f, w / 2f, h - margin - 20f, linePaint)
        canvas.drawLine(margin + 20f, h / 2f, w - margin - 20f, h / 2f, linePaint)
    }

    private fun drawDice(canvas: Canvas, dice: Dice) {
        canvas.save()
        val heightScale = 1f + dice.z / 800f
        val shadowOffset = dice.z / 15f
        shadowPaint.alpha = (80 - dice.z / 10f).coerceIn(10f, 80f).toInt()
        canvas.save(); canvas.translate(dice.x + shadowOffset, dice.y + shadowOffset); canvas.scale(heightScale, heightScale); drawDiceShape(canvas, dice, true); canvas.restore()
        canvas.translate(dice.x, dice.y); canvas.rotate(dice.rotation); canvas.scale(heightScale, heightScale)
        drawDiceShape(canvas, dice, false)
        if (dice.isSettled || dice.settleTimer > 0.1f) { drawDiceResult(canvas, dice) }
        canvas.restore()
    }

    private fun drawDiceShape(canvas: Canvas, dice: Dice, isShadow: Boolean) {
        val vertices = dice.getPolygonVertices()
        val path = Path(); path.moveTo(vertices[0], vertices[1])
        for (i in 2 until vertices.size step 2) { path.lineTo(vertices[i], vertices[i + 1]) }
        path.close()
        if (isShadow) { canvas.drawPath(path, shadowPaint) }
        else {
            dicePaint.color = dice.color; canvas.drawPath(path, dicePaint)
            highlightPaint.color = lightenColor(dice.color, 0.3f); highlightPaint.alpha = 60
            canvas.drawCircle(-dice.size / 2f * 0.2f, -dice.size / 2f * 0.2f, dice.size / 2f * 0.35f, highlightPaint)
            canvas.drawPath(path, diceStrokePaint)
        }
    }

    private fun drawDiceResult(canvas: Canvas, dice: Dice) {
        val resultText = dice.result.toString()
        val fontSize = when { dice.faces <= 6 -> dice.size * 0.45f; dice.faces <= 20 -> dice.size * 0.35f; else -> dice.size * 0.25f }
        textPaint.textSize = fontSize; textPaint.color = getContrastTextColor(dice.color)
        val textShadowPaint = Paint(textPaint).apply { color = Color.parseColor("#33000000") }
        canvas.drawText(resultText, 1f, 1f + fontSize / 3f, textShadowPaint)
        canvas.drawText(resultText, 0f, fontSize / 3f, textPaint)
    }

    private fun drawForceIndicator(canvas: Canvas, w: Float, h: Float) {
        val barWidth = 200f; val barHeight = 12f; val x = (w - barWidth) / 2f; val y = h - 60f
        canvas.drawRoundRect(RectF(x, y, x + barWidth, y + barHeight), 6f, 6f, forceIndicatorBgPaint)
        val fillWidth = barWidth * (forceMultiplier / 3f).coerceIn(0f, 1f)
        forceIndicatorPaint.color = when { forceMultiplier < 1f -> Color.parseColor("#4CAF50"); forceMultiplier < 2f -> Color.parseColor("#FF9800"); else -> Color.parseColor("#F44336") }
        canvas.drawRoundRect(RectF(x, y, x + fillWidth, y + barHeight), 6f, 6f, forceIndicatorPaint)
    }

    private fun lightenColor(color: Int, factor: Float): Int {
        return Color.rgb((Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255), (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255), (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255))
    }

    private fun getContrastTextColor(bgColor: Int): Int {
        val luminance = (0.299 * Color.red(bgColor) + 0.587 * Color.green(bgColor) + 0.114 * Color.blue(bgColor)) / 255.0
        return if (luminance > 0.5) Color.parseColor("#222222") else Color.parseColor("#FFFFFF")
    }

    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); stopRendering() }
}
