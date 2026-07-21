package com.finanza.next

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

class QuickExpenseShortcutActivity : Activity() {
    companion object {
        fun createIntent(context: Context): Intent = Intent(context, QuickExpenseShortcutActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, QuickExpenseActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
