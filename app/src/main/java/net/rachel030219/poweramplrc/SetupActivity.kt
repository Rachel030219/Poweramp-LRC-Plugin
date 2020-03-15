package net.rachel030219.poweramplrc

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.android.setupwizardlib.SetupWizardLayout
import com.android.setupwizardlib.view.NavigationBar.NavigationBarListener


class SetupActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_setup)

        init()
    }

    private fun init() {
        val setupWizard = findViewById<SetupWizardLayout>(R.id.setup)
        setupWizard.setHeaderText(R.string.main_title)
        setupWizard.setIllustration(getDrawable(R.drawable.suw_layout_background))
        setupWizard.setIllustrationAspectRatio(4f)
        setupWizard.navigationBar.setNavigationBarListener(
            object : NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }
                override fun onNavigateNext() {
                    finish()
                }
            })
    }
}