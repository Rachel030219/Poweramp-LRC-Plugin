package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.widget.Toast

import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

import androidx.preference.*
import dev.sasikanth.colorsheet.ColorSheet
import java.io.BufferedWriter
import java.io.OutputStreamWriter

class ConfigurationFragment: PreferenceFragmentCompat() {
    private val CREATE_LOG = 1000
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)
        applyInitialization(findPreference("duration"), findPreference("height"))
        val colors: MutableList<Int> = ArrayList()
        for (color in arrayOf(R.color.lrc_current_red, R.color.lrc_current_blue, R.color.lrc_current_green, R.color.lrc_current_yellow, R.color.lrc_current_purple)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                colors.add(resources.getColor(color, activity?.theme))
            else
                colors.add(resources.getColor(color))
        }
        findPreference<Preference>("color")?.setOnPreferenceClickListener {
            ColorSheet().colorPicker( colors = colors.toIntArray(), listener = { color ->
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("color", color).apply()
            } ).show(fragmentManager!!)
            true
        }
        findPreference<SwitchPreferenceCompat>("legacy")?.apply{
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // isEnabled = true
            }
            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(context, R.string.preference_after_restart, Toast.LENGTH_SHORT).show()
                true
            }
        }
        findPreference<SwitchPreferenceCompat>("encoding")?.apply {
            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(context, R.string.preference_after_reload, Toast.LENGTH_SHORT).show()
                true
            }
        }
        findPreference<Preference>("report")?.apply {
            setOnPreferenceClickListener {
                generateLog()
                true
            }
        }
    }

    private fun applyInitialization (vararg preferenceItems: EditTextPreference?) {
        for (item in preferenceItems) {
            item?.apply {
                val description = when (key) {
                    "duration" -> resources.getString(R.string.preference_ui_duration_description)
                    "height" -> resources.getString(R.string.preference_ui_height_description)
                    else -> resources.getString(R.string.error)
                }
                summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
                    if (TextUtils.isEmpty(preference.text)) {
                        description + resources.getString(R.string.preference_default)
                    } else {
                        description + preference.text
                    }
                }
                setOnBindEditTextListener {
                    it.inputType = InputType.TYPE_CLASS_NUMBER
                }
                setOnPreferenceChangeListener { _, _ ->
                    Toast.makeText(context, R.string.preference_after_restart, Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun generateLog () {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE,  SimpleDateFormat("yyyyMMdd-HHmmss").format(Date()) + ".txt")
        }
        startActivityForResult(intent, CREATE_LOG)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CREATE_LOG && resultCode == Activity.RESULT_OK) {
            context?.contentResolver?.openOutputStream(data?.data!!).use {
                BufferedWriter(OutputStreamWriter(it!!)).append(LogGenerator(context!!).generate()).run {
                    flush()
                    close()
                }
            }
            Toast.makeText(context, "Done, stored at ${data?.data}", Toast.LENGTH_SHORT).show()
        }
    }
}