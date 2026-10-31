package com.cocoa.expenses

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.cocoa.expenses.data.CocoaDatabase
import com.cocoa.expenses.ui.theme.CocoaTheme
import com.cocoa.expenses.ui.theme.WarmWhiteBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = CocoaDatabase.getInstance(this)
        setContent {
            CocoaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(WarmWhiteBackground),
                    color = WarmWhiteBackground
                ) {
                    CocoaRootApp(database = database)
                }
            }
        }
    }
}
