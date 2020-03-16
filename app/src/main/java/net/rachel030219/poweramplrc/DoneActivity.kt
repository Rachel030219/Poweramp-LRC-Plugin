package net.rachel030219.poweramplrc

import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.android.setupwizardlib.view.NavigationBar
import kotlinx.android.synthetic.main.activity_done.*

class DoneActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_done)

        done_setup.setIllustration(getDrawable(R.drawable.suw_layout_background))
        done_setup.setIllustrationAspectRatio(4f)
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
