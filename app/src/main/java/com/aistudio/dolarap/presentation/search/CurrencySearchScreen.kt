package com.aistudio.dolarap.presentation.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.AvailableCurrency
import com.aistudio.dolarap.presentation.components.EmptyStateView
import com.aistudio.dolarap.presentation.components.ErrorStateView
import com.aistudio.dolarap.presentation.components.FeedbackPillBanner
import com.aistudio.dolarap.presentation.components.LoadingStateView
import com.aistudio.dolarap.presentation.components.asString
import com.aistudio.dolarap.presentation.theme.DolarapThemeTokens
import org.koin.androidx.compose.koinViewModel

@Composable
fun CurrencySearchRoute(
  modifier: Modifier = Modifier,
  viewModel: CurrencySearchViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  CurrencySearchScreen(
    uiState = uiState,
    onQueryChange = viewModel::onQueryChange,
    onClearQuery = viewModel::clearQuery,
    onRefreshCatalog = viewModel::refreshCatalog,
    onAddCurrency = viewModel::addCurrency,
    onRemoveCurrency = viewModel::removeCurrency,
    onDismissMessages = viewModel::dismissMessages,
    modifier = modifier,
  )
}

@Composable
fun CurrencySearchScreen(
  uiState: CurrencySearchUiState,
  onQueryChange: (String) -> Unit,
  onClearQuery: () -> Unit,
  onRefreshCatalog: () -> Unit,
  onAddCurrency: (String) -> Unit,
  onRemoveCurrency: (String) -> Unit,
  onDismissMessages: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp).testTag("search_screen"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = stringResource(R.string.search_title),
          style = MaterialTheme.typography.headlineMedium,
          modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
          onClick = onRefreshCatalog,
          enabled = uiState !is CurrencySearchUiState.Loading &&
            !(uiState is CurrencySearchUiState.Success && uiState.isRefreshing) &&
            !(uiState is CurrencySearchUiState.Empty && uiState.isRefreshing),
          modifier = Modifier.testTag("search_refresh_catalog_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = stringResource(R.string.search_refresh_catalog))
        }
      }

      OutlinedTextField(
        value = uiState.query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().testTag("search_currency_input"),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        leadingIcon = {
          Icon(imageVector = Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
          if (uiState.query.isNotEmpty()) {
            IconButton(
              onClick = onClearQuery,
              modifier = Modifier.testTag("search_clear_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = stringResource(R.string.action_clear),
              )
            }
          }
        },
        placeholder = {
          Text(
            text = stringResource(R.string.search_hint),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        },
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (uiState) {
      is CurrencySearchUiState.Loading ->
        LoadingStateView(
          message = stringResource(R.string.search_loading_message),
          testTag = "search_loading_state",
        )

      is CurrencySearchUiState.Error ->
        ErrorStateView(
          message = uiState.message.asString(),
          onRetry = onRefreshCatalog,
          testTag = "search_error_state",
          retryTestTag = "search_error_retry_button",
        )

      is CurrencySearchUiState.Empty -> {
        if (uiState.isRefreshing) {
          LinearProgressIndicator(modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp))
          Spacer(modifier = Modifier.height(8.dp))
        }
        EmptyStateView(
          icon = Icons.Filled.SearchOff,
          title =
            if (uiState.isCatalogEmpty) {
              stringResource(R.string.search_empty_catalog_title)
            } else {
              stringResource(R.string.search_empty_query_title)
            },
          message =
            if (uiState.isCatalogEmpty) {
              stringResource(R.string.search_empty_catalog_message)
            } else {
              stringResource(R.string.search_empty_query_message, uiState.query)
            },
          actionLabel =
            if (uiState.isCatalogEmpty) {
              stringResource(R.string.search_refresh_catalog)
            } else {
              stringResource(R.string.action_clear)
            },
          onAction = if (uiState.isCatalogEmpty) onRefreshCatalog else onClearQuery,
          testTag = "search_empty_state",
          actionTestTag = "search_empty_action_button",
        )
      }

      is CurrencySearchUiState.Success -> {
        Column(modifier = Modifier.fillMaxSize().widthIn(max = 800.dp)) {
          if (uiState.isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
          }
          uiState.errorMessage?.let { err ->
            FeedbackPillBanner(
              message = err.asString(),
              isError = true,
              onDismiss = onDismissMessages,
              modifier = Modifier.testTag("search_error_banner"),
            )
            Spacer(modifier = Modifier.height(8.dp))
          }
          uiState.feedbackMessage?.let { feedback ->
            FeedbackPillBanner(
              message = feedback.asString(),
              isError = false,
              onDismiss = onDismissMessages,
              modifier = Modifier.testTag("search_feedback_banner"),
            )
            Spacer(modifier = Modifier.height(8.dp))
          }

          LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("search_results_list"),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(
              items = uiState.currencies,
              key = { it.pairCode },
            ) { currency ->
              AvailableCurrencyRow(
                currency = currency,
                isPending = uiState.pendingCodes.contains(currency.code),
                onAdd = { onAddCurrency(currency.code) },
                onRemove = { onRemoveCurrency(currency.code) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AvailableCurrencyRow(
  currency: AvailableCurrency,
  isPending: Boolean,
  onAdd: () -> Unit,
  onRemove: () -> Unit,
) {
  val semantic = DolarapThemeTokens.semanticColors
  Card(
    modifier = Modifier.fillMaxWidth().testTag("search_item_${currency.code}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
      ) {
        Text(
          text = currency.code,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = currency.friendlyName,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${currency.pairCode} • ${currency.symbol}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        when {
          currency.isDefault -> {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Filled.Lock,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(12.dp),
                )
                Text(
                  text = stringResource(R.string.search_status_default),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
          currency.isSelected -> {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = semantic.positiveContainer,
            ) {
              Text(
                text = stringResource(R.string.search_status_selected),
                style = MaterialTheme.typography.labelSmall,
                color = semantic.positive,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          else -> {
            Text(
              text = stringResource(R.string.search_status_available, currency.pairCode),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      when {
        currency.isDefault -> Unit
        currency.isSelected -> {
          OutlinedButton(
            onClick = onRemove,
            enabled = !isPending,
            modifier = Modifier.testTag("search_remove_button_${currency.code}"),
          ) {
            if (isPending) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
              Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = stringResource(R.string.action_remove),
                color = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
        else -> {
          Button(
            onClick = onAdd,
            enabled = !isPending && currency.isAvailable,
            modifier = Modifier.testTag("search_add_button_${currency.code}"),
          ) {
            if (isPending) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = stringResource(R.string.action_add))
            }
          }
        }
      }
    }
  }
}
