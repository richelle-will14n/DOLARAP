package com.aistudio.dolarap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aistudio.dolarap.presentation.navigation.DolarapApp
import com.aistudio.dolarap.presentation.theme.DolarapTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DolarapTheme {
        DolarapApp()
      }
    }
  }
}
