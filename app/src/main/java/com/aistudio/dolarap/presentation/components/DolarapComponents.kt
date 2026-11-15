package com.aistudio.dolarap.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.presentation.format.BrazilianFormatter
import com.aistudio.dolarap.presentation.format.UiText
import com.aistudio.dolarap.presentation.theme.DolarapNumericTypography
import com.aistudio.dolarap.presentation.theme.DolarapThemeTokens
import java.math.BigDecimal

@Composable
fun UiText.asString(): String =
  if (formatArgs.isEmpty()) {
    stringResource(id = resId)
  } else {
    stringResource(id = resId, formatArgs = formatArgs.toTypedArray())
  }

@Composable
fun FreshnessBadge(
  status: QuoteFreshnessStatus,
  modifier: Modifier = Modifier,
) {
  val semantic = DolarapThemeTokens.semanticColors
  val (label, containerColor, contentColor, borderColor, icon) =
    when (status) {
      QuoteFreshnessStatus.FRESH ->
        BadgeStyle(
          label = stringResource(R.string.quotes_badge_fresh),
          container = semantic.positiveContainer,
          content = semantic.positive,
          border = semantic.positiveBorder,
          icon = Icons.Filled.CheckCircle,
        )
      QuoteFreshnessStatus.STALE ->
        BadgeStyle(
          label = stringResource(R.string.quotes_badge_stale),
          container = semantic.warningContainer,
          content = semantic.warning,
          border = semantic.warningBorder,
          icon = Icons.Filled.Schedule,
        )
      QuoteFreshnessStatus.UNAVAILABLE ->
        BadgeStyle(
          label = stringResource(R.string.quotes_badge_unavailable),
          container = MaterialTheme.colorScheme.surfaceVariant,
          content = MaterialTheme.colorScheme.onSurfaceVariant,
          border = MaterialTheme.colorScheme.outlineVariant,
          icon = Icons.Filled.CloudOff,
        )
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun VariationPill(
  pctChange: BigDecimal,
  modifier: Modifier = Modifier,
) {
  val semantic = DolarapThemeTokens.semanticColors
  val (container, content) =
    when {
      pctChange > BigDecimal.ZERO -> semantic.positiveContainer to semantic.positive
      pctChange < BigDecimal.ZERO -> semantic.negativeContainer to semantic.negative
      else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(8.dp),
    color = container,
  ) {
    Text(
      text = BrazilianFormatter.formatPercentageChange(pctChange),
      style = DolarapNumericTypography.badgeNumber,
      color = content,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    )
  }
}

@Composable
fun StatusBannerCard(
  title: String,
  message: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier,
  onDismiss: (() -> Unit)? = null,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f)),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = contentColor,
        modifier = Modifier.size(20.dp),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelLarge,
          color = contentColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodySmall,
          color = contentColor,
        )
      }
      if (onDismiss != null) {
        IconButton(onClick = onDismiss) {
          Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.action_close),
            tint = contentColor,
          )
        }
      }
    }
  }
}

@Composable
fun LoadingStateView(
  message: String,
  modifier: Modifier = Modifier,
  testTag: String = "loading_state_view",
) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp).testTag(testTag),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun EmptyStateView(
  icon: ImageVector,
  title: String,
  message: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  testTag: String = "empty_state_view",
  actionTestTag: String = "empty_state_action",
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(24.dp).testTag(testTag),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp).size(36.dp),
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    if (actionLabel != null && onAction != null) {
      Spacer(modifier = Modifier.height(16.dp))
      OutlinedButton(
        onClick = onAction,
        modifier = Modifier.testTag(actionTestTag),
      ) {
        Text(text = actionLabel)
      }
    }
  }
}

@Composable
fun ErrorStateView(
  message: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "error_state_view",
  retryTestTag: String = "error_retry_button",
) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp).testTag(testTag),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.errorContainer,
    ) {
      Icon(
        imageVector = Icons.Filled.ErrorOutline,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(16.dp).size(36.dp),
      )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = onRetry,
      modifier = Modifier.testTag(retryTestTag),
    ) {
      Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = stringResource(R.string.action_retry))
    }
  }
}

@Composable
fun FeedbackPillBanner(
  message: String,
  isError: Boolean,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val semantic = DolarapThemeTokens.semanticColors
  StatusBannerCard(
    title = if (isError) stringResource(R.string.nav_quotes) else stringResource(R.string.app_name),
    message = message,
    icon = if (isError) Icons.Filled.ErrorOutline else Icons.Filled.Info,
    containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else semantic.positiveContainer,
    contentColor = if (isError) MaterialTheme.colorScheme.onErrorContainer else semantic.positive,
    onDismiss = onDismiss,
    modifier = modifier,
  )
}

private data class BadgeStyle(
  val label: String,
  val container: Color,
  val content: Color,
  val border: Color,
  val icon: ImageVector,
)
