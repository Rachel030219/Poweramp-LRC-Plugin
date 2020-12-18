package net.rachel030219.poweramplrc

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.preference.*
import dev.sasikanth.colorsheet.ColorSheet
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ConfigurationFragment: PreferenceFragmentCompat() {
    private val CREATE_LOG = 1000
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preference, rootKey)
        applyInitialization(findPreference("offset"), findPreference("duration"), findPreference("height"), findPreference("textSize"), findPreference("opacity"), findPreference("strokeWidth"))
        val colors: MutableList<Int> = ArrayList()
        val strokeColors: MutableList<Int> = ArrayList()
        for (color in arrayOf(
            R.color.lrc_current_red,
            R.color.lrc_current_darkred,
            R.color.lrc_current_blue,
            R.color.lrc_current_green,
            R.color.lrc_current_yellow,
            R.color.lrc_current_purple,
            R.color.lrc_current_white,
            R.color.lrc_current_black)) {
            colors.add(ResourcesCompat.getColor(resources, color, requireActivity().theme))
        }
        for (color in arrayOf(R.color.lrc_stroke_dark, R.color.lrc_stroke_light))
            strokeColors.add(ResourcesCompat.getColor(resources, color, requireActivity().theme))
        findPreference<Preference>("textColor")?.setOnPreferenceClickListener {
            ColorSheet().colorPicker(colors = colors.toIntArray(), listener = { color ->
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("textColor", color).apply()
            }, selectedColor = PreferenceManager.getDefaultSharedPreferences(context).getInt("textColor", colors[0])).show(parentFragmentManager)
            true
        }
        findPreference<Preference>("strokeColor")?.setOnPreferenceClickListener {
            ColorSheet().colorPicker(colors = strokeColors.toIntArray(), listener = { color ->
                PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("strokeColor", color).apply()
            }, selectedColor = PreferenceManager.getDefaultSharedPreferences(context).getInt("strokeColor", colors[0])).show(parentFragmentManager)
            true
        }
        findPreference<SwitchPreferenceCompat>("embedded")?.apply {
            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(context, R.string.preference_after_reload, Toast.LENGTH_SHORT).show()
                true
            }
        }
        findPreference<Preference>("standalone_add")?.setOnPreferenceClickListener {
            startActivity(Intent(requireActivity(), FoldersActivity::class.java))
            true
        }
        findPreference<SwitchPreferenceCompat>("charset")?.apply {
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
                summaryProvider = Preference.SummaryProvider<EditTextPreference> { item ->
                    when (key) {
                        "offset" -> {
                            if (TextUtils.isEmpty(item.text))
                                resources.getString(R.string.preference_offset_description, getString(R.string.preference_default))
                            else
                                resources.getString(R.string.preference_offset_description, item.text)
                        }
                        "duration" -> {
                            if (TextUtils.isEmpty(item.text))
                                resources.getString(R.string.preference_ui_duration_description, getString(R.string.preference_default))
                            else
                                resources.getString(R.string.preference_ui_duration_description, item.text)
                        }
                        "height" -> {
                            if (TextUtils.isEmpty(item.text))
                                resources.getString(R.string.preference_ui_height_description, getString(R.string.preference_default))
                            else
                                resources.getString(R.string.preference_ui_height_description, item.text)
                        }
                        "textSize" -> {
                            if (TextUtils.isEmpty(item.text))
                                resources.getString(R.string.preference_ui_textsize_description, getString(R.string.preference_default))
                            else
                                resources.getString(R.string.preference_ui_textsize_description, item.text)
                        }
                        "opacity" -> {
                            if (TextUtils.isEmpty(item.text))
                                resources.getString(R.string.preference_ui_opacity_description, getString(R.string.preference_default))
                            else
                                resources.getString(R.string.preference_ui_opacity_description, item.text)
                        }
                        "strokeWidth" -> {
                            if (TextUtils.isEmpty(item.text))
                                resources.getString(R.string.preference_ui_stroke_width_description, getString(R.string.preference_default))
                            else
                                resources.getString(R.string.preference_ui_stroke_width_description, item.text)
                        }
                        else -> resources.getString(R.string.error)
                    }
                }
                setOnBindEditTextListener {
                    if (key != "offset")
                        it.inputType = InputType.TYPE_CLASS_NUMBER
                    else
                        it.inputType = InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_CLASS_NUMBER
                }
                setOnPreferenceChangeListener { _, _ ->
                    if (key != "opacity")
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
                BufferedWriter(OutputStreamWriter(it!!)).append(LogGenerator(requireContext()).generate()).use { writer -> writer.flush() }
            }
            Toast.makeText(context, "Done, stored at ${data?.data}", Toast.LENGTH_SHORT).show()
        }
    }
}