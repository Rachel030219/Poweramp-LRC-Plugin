package net.rachel030219.poweramplrc

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.android.setupwizardlib.view.NavigationBar
import kotlinx.android.synthetic.main.activity_done.*

class DoneActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_done)

        done_setup.setIllustration(ResourcesCompat.getDrawable(resources, R.drawable.suw_layout_background, theme))
        done_setup.setIllustrationAspectRatio(2.5f)
        done_setup.navigationBar.setNavigationBarListener(
            object : NavigationBar.NavigationBarListener {
                override fun onNavigateBack() {
                    onBackPressed()
                }

                override fun onNavigateNext() {
                    finish()
                }
            })
    }

}
