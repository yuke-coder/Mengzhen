package com.mengzhen.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mengzhen.app.ui.theme.MengZhenTheme
import com.mengzhen.app.ui.navigation.MengZhenApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MengZhenTheme {
                MengZhenApp()
            }
        }
    }
}
