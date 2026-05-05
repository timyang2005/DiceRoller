package com.diceroller.app

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private lateinit var countSlider: SeekBar; private lateinit var countValue: TextView
    private lateinit var colorRed: SeekBar; private lateinit var colorGreen: SeekBar; private lateinit var colorBlue: SeekBar
    private lateinit var colorPreview: View
    private lateinit var colorRedValue: TextView; private lateinit var colorGreenValue: TextView; private lateinit var colorBlueValue: TextView
    private lateinit var showSumSwitch: Switch; private lateinit var soundSwitch: Switch
    private var selectedPreset: String = "D6"; private var diceCount = 2; private var showSum = true; private var soundEnabled = true
    private val presetButtons = mutableListOf<Button>()
    private val presetColors = listOf(Triple(255,255,255) to "白色", Triple(220,50,50) to "红色", Triple(50,120,220) to "蓝色", Triple(50,180,50) to "绿色", Triple(255,200,50) to "金色", Triple(180,50,220) to "紫色", Triple(50,200,200) to "青色", Triple(255,140,50) to "橙色", Triple(40,40,40) to "黑色")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        initViews(); loadSettings(); setupListeners()
    }

    private fun initViews() {
        countSlider = findViewById(R.id.countSlider); countValue = findViewById(R.id.countValue)
        colorRed = findViewById(R.id.colorRed); colorGreen = findViewById(R.id.colorGreen); colorBlue = findViewById(R.id.colorBlue)
        colorPreview = findViewById(R.id.colorPreview)
        colorRedValue = findViewById(R.id.colorRedValue); colorGreenValue = findViewById(R.id.colorGreenValue); colorBlueValue = findViewById(R.id.colorBlueValue)
        showSumSwitch = findViewById(R.id.showSumSwitch); soundSwitch = findViewById(R.id.soundSwitch)
        val presetContainer = findViewById<LinearLayout>(R.id.presetContainer)
        for ((index, preset) in DicePreset.entries.withIndex()) {
            val row = index / 4; val col = index % 4
            if (col == 0) { val rowLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); gravity = Gravity.CENTER_HORIZONTAL }; presetContainer.addView(rowLayout) }
            val button = Button(this).apply { text = preset.displayName; id = View.generateViewId(); textSize = 14f; setAllCaps(false); setPadding(16,8,16,8); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 4; marginEnd = 4 }; tag = preset.name; setOnClickListener { selectedPreset = preset.name; updatePresetButtons() } }
            presetButtons.add(button); (presetContainer.getChildAt(row) as LinearLayout).addView(button)
        }
        val quickColorContainer = findViewById<LinearLayout>(R.id.quickColorContainer)
        for ((rgb, name) in presetColors) {
            val button = Button(this).apply { text = name; textSize = 11f; setAllCaps(false); setPadding(8,4,8,4); setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(rgb.first, rgb.second, rgb.third)); layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 2; marginEnd = 2 }; setOnClickListener { colorRed.progress = rgb.first; colorGreen.progress = rgb.second; colorBlue.progress = rgb.third; updateColorPreview() } }
            quickColorContainer.addView(button)
        }
    }

    private fun loadSettings() {
        selectedPreset = prefs.getString("dice_preset", "D6") ?: "D6"; diceCount = prefs.getInt("dice_count", 2)
        val savedColor = prefs.getInt("dice_color", 0xFFFFFFFF.toInt()); showSum = prefs.getBoolean("show_sum", true); soundEnabled = prefs.getBoolean("sound_enabled", true)
        updatePresetButtons(); countSlider.progress = diceCount - 1; countValue.text = diceCount.toString()
        colorRed.progress = Color.red(savedColor); colorGreen.progress = Color.green(savedColor); colorBlue.progress = Color.blue(savedColor)
        colorRedValue.text = Color.red(savedColor).toString(); colorGreenValue.text = Color.green(savedColor).toString(); colorBlueValue.text = Color.blue(savedColor).toString()
        showSumSwitch.isChecked = showSum; soundSwitch.isChecked = soundEnabled; updateColorPreview()
    }

    private fun updatePresetButtons() { for (button in presetButtons) { if (button.tag == selectedPreset) { button.setBackgroundColor(Color.parseColor("#4CAF50")); button.setTextColor(Color.WHITE) } else { button.setBackgroundColor(Color.parseColor("#E0E0E0")); button.setTextColor(Color.parseColor("#333333")) } } }

    private fun setupListeners() {
        countSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { diceCount = p + 1; countValue.text = diceCount.toString() }; override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {} })
        val colorListener = object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) { colorRedValue.text = colorRed.progress.toString(); colorGreenValue.text = colorGreen.progress.toString(); colorBlueValue.text = colorBlue.progress.toString(); updateColorPreview() }; override fun onStartTrackingTouch(s: SeekBar?) {}; override fun onStopTrackingTouch(s: SeekBar?) {} }
        colorRed.setOnSeekBarChangeListener(colorListener); colorGreen.setOnSeekBarChangeListener(colorListener); colorBlue.setOnSeekBarChangeListener(colorListener)
        showSumSwitch.setOnCheckedChangeListener { _, isChecked -> showSum = isChecked }; soundSwitch.setOnCheckedChangeListener { _, isChecked -> soundEnabled = isChecked }
    }

    private fun updateColorPreview() { colorPreview.setBackgroundColor(Color.rgb(colorRed.progress, colorGreen.progress, colorBlue.progress)) }
    override fun onPause() { super.onPause(); saveSettings() }
    private fun saveSettings() { prefs.edit().putString("dice_preset", selectedPreset).putInt("dice_count", diceCount).putInt("dice_color", Color.rgb(colorRed.progress, colorGreen.progress, colorBlue.progress)).putBoolean("show_sum", showSum).putBoolean("sound_enabled", soundEnabled).apply() }
    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
