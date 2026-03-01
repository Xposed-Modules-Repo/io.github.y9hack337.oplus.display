package io.github.y9hack337.oplus.display

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.InputStreamReader

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
            val result = tryRestartSystemUiWithRoot()

            runOnUiThread {
                when {
                    result.success -> {
                        Toast.makeText(this, R.string.restart_systemui_success, Toast.LENGTH_SHORT).show()
                    }

                    result.message.isNotBlank() -> {
                        Toast.makeText(
                            this,
                            getString(R.string.restart_systemui_failed_with_reason, result.message),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    else -> {
                        Toast.makeText(this, R.string.restart_systemui_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }

    private fun tryRestartSystemUiWithRoot(): CommandResult {
        val rootCheck = execRoot("id")
        if (rootCheck.exitCode != 0) {
            return CommandResult(
                success = false,
                message = if (rootCheck.message.isNotBlank()) rootCheck.message else "Root shell is not available."
            )
        }

        // Try multiple restart strategies because command availability differs by ROM/toybox build.
        val commands = listOf(
            "for p in \$(pidof com.android.systemui); do kill -9 \$p; done",
            "pkill -9 -f com.android.systemui",
            "killall com.android.systemui",
            "am force-stop com.android.systemui"
        )

        var lastFailure = ""
        for (command in commands) {
            val result = execRoot(command)
            if (result.exitCode == 0) {
                return CommandResult(true, "")
            }
            if (result.message.isNotBlank()) {
                lastFailure = result.message
            }
        }

        return CommandResult(false, lastFailure)
    }

    private fun execRoot(command: String): ProcessResult {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(false)
                .start()

            val stdout = readAll(process.inputStream)
            val stderr = readAll(process.errorStream)
            val exitCode = process.waitFor()
            val message = when {
                stderr.isNotBlank() -> stderr.trim()
                stdout.isNotBlank() -> stdout.trim()
                else -> ""
            }
            ProcessResult(exitCode, message)
        } catch (e: Exception) {
            ProcessResult(-1, e.message ?: "")
        }
    }

    private fun readAll(stream: java.io.InputStream): String {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            val sb = StringBuilder()
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                sb.append(line).append('\n')
            }
            return sb.toString()
        }
    }

    private data class ProcessResult(val exitCode: Int, val message: String)

    private data class CommandResult(val success: Boolean, val message: String)
}
