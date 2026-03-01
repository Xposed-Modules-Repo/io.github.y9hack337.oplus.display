package io.github.y9hack337.oplus.display

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.button_restart_systemui).setOnClickListener {
            showRestartConfirmDialog()
        }
    }

    private fun showRestartConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.restart_systemui_confirm_title)
            .setMessage(R.string.restart_systemui_confirm_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_restart) { _, _ ->
                restartSystemUi()
            }
            .show()
    }

    private fun restartSystemUi() {
        Thread {
            val exitCode = try {
                val process = Runtime.getRuntime().exec(
                    arrayOf(
                        "su",
                        "-c",
                        "pkill -f com.android.systemui || killall com.android.systemui"
                    )
                )
                process.waitFor()
            } catch (_: Exception) {
                -1
            }

            runOnUiThread {
                val messageRes =
                    if (exitCode == 0) R.string.restart_systemui_success else R.string.restart_systemui_failed
                Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
            }
        }.start()
    }
}
