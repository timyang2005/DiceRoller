package com.diceroller.app

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    private lateinit var surfaceView: DiceSurfaceView
    private lateinit var resultText: TextView
    private lateinit var sumText: TextView
    private lateinit var hintText: TextView
    private lateinit var diceContainer: FrameLayout
    private lateinit var soundManager: SoundManager
    private lateinit var prefs: SharedPreferences
    private var physicsEngine: DicePhysicsEngine? = null
    private var clickTimes = mutableListOf<Long>()
    private var currentForce = 1.0f
    private var isRolling = false
    private var diceFaces: Int = 6
    private var diceCount: Int = 2
    private var diceColor: Int = 0xFFFFFFFF.toInt()
    private var showSum: Boolean = true
    private var soundEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        soundManager = SoundManager(this)
        diceContainer = findViewById(R.id.diceContainer)
        resultText = findViewById(R.id.resultText)
        sumText = findViewById(R.id.sumText)
        hintText = findViewById(R.id.hintText)
        surfaceView = DiceSurfaceView(this)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                physicsEngine = DicePhysicsEngine(width.toFloat(), height.toFloat())
                surfaceView.setPhysicsEngine(physicsEngine!!)
                physicsEngine?.setOnSettleListener { diceList -> runOnUiThread { onDiceSettled(diceList) } }
                physicsEngine?.setOnBounceListener { dice -> if (soundEnabled) soundManager.playBounce(dice.bounceCount) }
                surfaceView.startRendering()
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) { surfaceView.stopRendering() }
        })
        diceContainer.addView(surfaceView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        loadSettings()
        diceContainer.setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_DOWN) handleTap(); true }
    }

    private fun handleTap() {
        val now = System.currentTimeMillis(); clickTimes.add(now)
        clickTimes = clickTimes.filter { it > now - 1500L }.toMutableList()
        currentForce = 0.3f + (clickTimes.size.coerceAtMost(10).toFloat() / 10) * 2.7f
        if (!isRolling) rollDice()
    }

    override fun onResume() { super.onResume(); loadSettings(); surfaceView.startRendering() }
    override fun onPause() { super.onPause(); surfaceView.stopRendering() }
    override fun onDestroy() { super.onDestroy(); soundManager.release() }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { menuInflater.inflate(R.menu.main_menu, menu); return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> { startActivityForResult(Intent(this, SettingsActivity::class.java), 1001); true }
            R.id.action_clear -> { clearResults(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rollDice() {
        val engine = physicsEngine ?: return; isRolling = true
        resultText.visibility = View.GONE; sumText.visibility = View.GONE; hintText.visibility = View.GONE
        val diceSize = when { diceCount <= 2 -> 90f; diceCount <= 4 -> 76f; diceCount <= 6 -> 67f; diceCount <= 10 -> 58f; else -> 49f }
        if (soundEnabled) soundManager.playRoll(currentForce)
        engine.launchDice(diceCount, diceFaces, diceColor, diceSize, currentForce)
        surfaceView.setForceMultiplier(currentForce, true)
        clickTimes.clear(); currentForce = 1.0f
    }

    private fun onDiceSettled(diceList: List<Dice>) {
        isRolling = false; surfaceView.setForceMultiplier(1f, false)
        if (soundEnabled) soundManager.playSettle()
        val results = diceList.map { it.result }; resultText.text = results.joinToString(", "); resultText.visibility = View.VISIBLE
        if (showSum && diceList.size > 1) { sumText.text = getString(R.string.sum_format, results.sum()); sumText.visibility = View.VISIBLE } else { sumText.visibility = View.GONE }
    }

    private fun clearResults() { physicsEngine?.clear(); resultText.visibility = View.GONE; sumText.visibility = View.GONE; hintText.visibility = View.VISIBLE; isRolling = false }

    private fun loadSettings() {
        diceFaces = prefs.getString("dice_preset", "D6")?.let { try { DicePreset.valueOf(it).faces } catch (_: Exception) { 6 } } ?: 6
        diceCount = prefs.getInt("dice_count", 2); diceColor = prefs.getInt("dice_color", 0xFFFFFFFF.toInt())
        showSum = prefs.getBoolean("show_sum", true); soundEnabled = prefs.getBoolean("sound_enabled", true)
        soundManager.setEnabled(soundEnabled)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION") super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) loadSettings()
    }
}
