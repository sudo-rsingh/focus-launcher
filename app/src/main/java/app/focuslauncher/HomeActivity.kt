package app.focuslauncher

import android.annotation.SuppressLint
import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher

/**
 * The whole launcher: two doors, Phone and Messages.
 *
 * It never lists installed apps. It asks the system for the default dialer and
 * the default SMS app, and nothing else.
 */
class HomeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<View>(R.id.phone).setOnClickListener { open(phoneIntent()) }
        findViewById<View>(R.id.messages).setOnClickListener { open(messagesIntent()) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Home is the bottom of the stack; back has nowhere to go.
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
            ) {}
        }

        if (savedInstanceState == null) requestHomeRole()
    }

    // Only reached on API < 33. Newer versions use the OnBackInvokedCallback above.
    // Lint's suggested fix needs AndroidX, which this app doesn't use.
    @SuppressLint("GestureBackNavigation")
    @Deprecated("Back handling for API < 33; newer versions use OnBackInvokedCallback.")
    override fun onBackPressed() {
        // Swallowed on purpose.
    }

    /** The default dialer's own main screen (recents, contacts), else a bare dial pad. */
    private fun phoneIntent(): Intent {
        val dialer = getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        return dialer?.let(packageManager::getLaunchIntentForPackage)
            ?: Intent(Intent.ACTION_DIAL)
    }

    /** The default SMS app, else whatever claims the messaging category. */
    private fun messagesIntent(): Intent {
        val sms = Telephony.Sms.getDefaultSmsPackage(this)
        return sms?.let(packageManager::getLaunchIntentForPackage)
            ?: Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show()
        }
    }

    /** If this isn't the default home app yet, show the system's "set as home" prompt. */
    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roles = getSystemService(RoleManager::class.java) ?: return
        if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
            @Suppress("DEPRECATION") // The role request has to be started for a result.
            startActivityForResult(roles.createRequestRoleIntent(RoleManager.ROLE_HOME), 0)
        }
    }
}
