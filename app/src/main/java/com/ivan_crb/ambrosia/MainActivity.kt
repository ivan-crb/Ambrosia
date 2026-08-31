package com.ivan_crb.ambrosia

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ivan_crb.ambrosia.components.MyNavigationBar
import com.ivan_crb.ambrosia.ui.theme.AmbrosiaTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ivan_crb.ambrosia.viewmodel.RecipeViewModel

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: RecipeViewModel = viewModel()
            val settings by viewModel.userSettings.collectAsState()

            val darkTheme = when (settings.themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            AmbrosiaTheme(darkTheme = darkTheme) {
                MyNavigationBar()
            }
        }
    }
}