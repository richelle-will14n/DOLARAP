package com.aistudio.dolarap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.aistudio.dolarap.presentation.theme.DolarapTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DolarapTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          BootstrapPlaceholder(modifier = Modifier.padding(innerPadding))
        }
      }
    }
  }
}

@Composable
fun BootstrapPlaceholder(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp).testTag("e01_bootstrap_placeholder"),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = stringResource(id = R.string.app_name),
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.primary,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(id = R.string.e01_placeholder_subtitle),
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
      text = stringResource(id = R.string.e01_placeholder_status),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Preview(showBackground = true)
@Composable
fun BootstrapPlaceholderPreview() {
  DolarapTheme { BootstrapPlaceholder() }
}
