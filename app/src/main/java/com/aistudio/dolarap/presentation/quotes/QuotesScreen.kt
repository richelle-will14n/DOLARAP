package com.aistudio.dolarap.presentation.quotes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.CurrencyQuote
import com.aistudio.dolarap.domain.model.SyncStatus
import com.aistudio.dolarap.presentation.components.EmptyStateView
import com.aistudio.dolarap.presentation.components.ErrorStateView
import com.aistudio.dolarap.presentation.components.FeedbackPillBanner
import com.aistudio.dolarap.presentation.components.FreshnessBadge
import com.aistudio.dolarap.presentation.components.LoadingStateView
import com.aistudio.dolarap.presentation.components.StatusBannerCard
import com.aistudio.dolarap.presentation.components.VariationPill
import com.aistudio.dolarap.presentation.components.asString
import com.aistudio.dolarap.presentation.format.BrazilianFormatter
import com.aistudio.dolarap.presentation.theme.DolarapNumericTypography
import com.aistudio.dolarap.presentation.theme.DolarapThemeTokens
import org.koin.androidx.compose.koinViewModel

@Composable
fun QuotesRoute(
  onNavigateToSearch: () -> Unit,
  onNavigateToConverter: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: QuotesViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  QuotesScreen(
    uiState = uiState,
    onRefresh = viewModel::refreshQuotes,
    onSetFavoritesOnly = viewModel::setFavoritesOnly,
    onToggleFavorite = viewModel::toggleFavorite,
    onRemoveCurrency = viewModel::removeCurrency,
    onDismissBannerError = viewModel::dismissBannerError,
    onDismissFeedback = viewModel::dismissFeedback,
    onNavigateToSearch = onNavigateToSearch,
    onNavigateToConverter = onNavigateToConverter,
    modifier = modifier,
  )
}

@Composable
fun QuotesScreen(
  uiState: QuotesUiState,
  onRefresh: () -> Unit,
  onSetFavoritesOnly: (Boolean) -> Unit,
  onToggleFavorite: (String) -> Unit,
  onRemoveCurrency: (String) -> Unit,
  onDismissBannerError: () -> Unit,
  onDismissFeedback: () -> Unit,
  onNavigateToSearch: () -> Unit,
  onNavigateToConverter: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize().testTag("quotes_screen"),
  ) {
    val columns = if (maxWidth >= 720.dp) 2 else 1

    when (uiState) {
      QuotesUiState.Loading ->
        LoadingStateView(
          message = stringResource(R.string.quotes_loading_message),
          testTag = "quotes_loading_state",
        )

      is QuotesUiState.Error ->
        ErrorStateView(
          message = uiState.message.asString(),
          onRetry = onRefresh,
          testTag = "quotes_error_state",
          retryTestTag = "quotes_error_retry_button",
        )

      is QuotesUiState.Empty ->
        Column(
          modifier =
            Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
          QuotesHeaderCard(
            syncStatus = uiState.syncStatus,
            favoritesOnly = uiState.favoritesOnly,
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            onSetFavoritesOnly = onSetFavoritesOnly,
            onNavigateToSearch = onNavigateToSearch,
            onNavigateToConverter = onNavigateToConverter,
          )
          uiState.bannerError?.let { errorText ->
            Spacer(modifier = Modifier.height(10.dp))
            FeedbackPillBanner(
              message = errorText.asString(),
              isError = true,
              onDismiss = onDismissBannerError,
              modifier = Modifier.testTag("quotes_error_banner"),
            )
          }
          EmptyStateView(
            icon = if (uiState.favoritesOnly) Icons.Outlined.StarBorder else Icons.Filled.CurrencyExchange,
            title =
              if (uiState.favoritesOnly) {
                stringResource(R.string.quotes_empty_favorites_title)
              } else {
                stringResource(R.string.quotes_empty_list_title)
              },
            message =
              if (uiState.favoritesOnly) {
                stringResource(R.string.quotes_empty_favorites_message)
              } else {
                stringResource(R.string.quotes_empty_list_message)
              },
            actionLabel =
              if (uiState.favoritesOnly) {
                stringResource(R.string.action_view_all)
              } else {
                stringResource(R.string.action_refresh)
              },
            onAction =
              if (uiState.favoritesOnly) {
                { onSetFavoritesOnly(false) }
              } else {
                onRefresh
              },
            testTag = "quotes_empty_state",
            actionTestTag = "quotes_empty_action_button",
          )
        }

      is QuotesUiState.Success -> {
        val semantic = DolarapThemeTokens.semanticColors
        LazyVerticalGrid(
          columns = GridCells.Fixed(columns),
          modifier = Modifier.fillMaxSize().testTag("quotes_list"),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item(span = { GridItemSpan(columns) }, key = "header") {
            QuotesHeaderCard(
              syncStatus = uiState.syncStatus,
              favoritesOnly = uiState.favoritesOnly,
              isRefreshing = uiState.isRefreshing,
              onRefresh = onRefresh,
              onSetFavoritesOnly = onSetFavoritesOnly,
              onNavigateToSearch = onNavigateToSearch,
              onNavigateToConverter = onNavigateToConverter,
            )
          }

          if (uiState.syncStatus.isStale) {
            item(span = { GridItemSpan(columns) }, key = "stale_banner") {
              StatusBannerCard(
                title = stringResource(R.string.quotes_stale_banner_title),
                message = stringResource(R.string.quotes_stale_banner_message),
                icon = Icons.Filled.Schedule,
                containerColor = semantic.warningContainer,
                contentColor = semantic.onWarningContainer,
                modifier = Modifier.testTag("quotes_stale_banner"),
              )
            }
          }

          uiState.bannerError?.let { errorText ->
            item(span = { GridItemSpan(columns) }, key = "error_banner") {
              FeedbackPillBanner(
                message = errorText.asString(),
                isError = true,
                onDismiss = onDismissBannerError,
                modifier = Modifier.testTag("quotes_error_banner"),
              )
            }
          }

          uiState.feedbackMessage?.let { feedback ->
            item(span = { GridItemSpan(columns) }, key = "feedback_banner") {
              FeedbackPillBanner(
                message = feedback.asString(),
                isError = false,
                onDismiss = onDismissFeedback,
                modifier = Modifier.testTag("quotes_feedback_banner"),
              )
            }
          }

          items(
            items = uiState.items,
            key = { it.code },
          ) { item ->
            CurrencyQuoteCard(
              item = item,
              onToggleFavorite = { onToggleFavorite(item.code) },
              onRemoveCurrency = { onRemoveCurrency(item.code) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuotesHeaderCard(
  syncStatus: SyncStatus,
  favoritesOnly: Boolean,
  isRefreshing: Boolean,
  onRefresh: () -> Unit,
  onSetFavoritesOnly: (Boolean) -> Unit,
  onNavigateToSearch: () -> Unit,
  onNavigateToConverter: () -> Unit,
) {
  val semantic = DolarapThemeTokens.semanticColors
  val syncSummaryText =
    syncStatus.lastSuccessfulSyncAtMillis?.let { timestamp ->
      stringResource(R.string.quotes_last_sync_format, BrazilianFormatter.formatDateTime(timestamp))
    } ?: stringResource(R.string.quotes_never_synced)

  Column(
    modifier = Modifier.fillMaxWidth().widthIn(max = 900.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .background(
            brush = Brush.linearGradient(
              colors = listOf(semantic.heroGradientStart, semantic.heroGradientEnd),
            )
          )
          .border(
            width = 1.dp,
            color = semantic.heroBorder,
            shape = RoundedCornerShape(22.dp),
          )
          .padding(18.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = semantic.onHeroBrand,
            )
            Text(
              text = stringResource(R.string.quotes_title),
              style = MaterialTheme.typography.headlineMedium,
              color = semantic.onHeroTitle,
            )
          }

          FilledTonalButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            colors =
              ButtonDefaults.filledTonalButtonColors(
                containerColor = semantic.heroButtonContainer,
                contentColor = semantic.heroButtonContent,
                disabledContainerColor = semantic.heroProgressTrack,
                disabledContentColor = semantic.onHeroSecondary,
              ),
            modifier = Modifier.testTag("quotes_refresh_button"),
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = semantic.onHeroSecondary,
              )
            } else {
              Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = semantic.heroButtonContent,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.action_refresh))
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = syncSummaryText,
            style = MaterialTheme.typography.bodySmall,
            color = semantic.onHeroSecondary,
            modifier = Modifier.weight(1f).testTag("quotes_last_sync_text"),
          )
          Spacer(modifier = Modifier.width(8.dp))
          FreshnessBadge(
            status = syncStatus.freshnessStatus,
            modifier = Modifier.testTag("quotes_global_freshness_badge"),
          )
        }

        if (isRefreshing) {
          LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
            color = semantic.heroProgressIndicator,
            trackColor = semantic.heroProgressTrack,
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      FilterChip(
        selected = !favoritesOnly,
        onClick = { onSetFavoritesOnly(false) },
        label = { Text(stringResource(R.string.quotes_filter_all)) },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        modifier = Modifier.testTag("quotes_filter_all"),
      )
      FilterChip(
        selected = favoritesOnly,
        onClick = { onSetFavoritesOnly(true) },
        leadingIcon = {
          Icon(
            imageVector = if (favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
        },
        label = { Text(stringResource(R.string.quotes_filter_favorites)) },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
        modifier = Modifier.testTag("quotes_filter_favorites"),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      OutlinedButton(
        onClick = onNavigateToSearch,
        modifier = Modifier.weight(1f).testTag("quotes_go_search_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.AddCircleOutline,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.quotes_quick_search),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      OutlinedButton(
        onClick = onNavigateToConverter,
        modifier = Modifier.weight(1f).testTag("quotes_go_converter_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.CurrencyExchange,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(R.string.quotes_quick_converter),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun CurrencyQuoteCard(
  item: CurrencyQuote,
  onToggleFavorite: () -> Unit,
  onRemoveCurrency: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().testTag("quote_card_${item.code}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
        ) {
          Text(
            text = item.code,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = item.displayName,
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (item.isDefault) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
              ) {
                Text(
                  text = stringResource(R.string.quotes_badge_default),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
          }
          Text(
            text = "${item.pairCode} • ${item.symbol}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        IconButton(
          onClick = onToggleFavorite,
          modifier = Modifier.testTag("favorite_toggle_${item.code}"),
        ) {
          Icon(
            imageVector = if (item.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription =
              if (item.isFavorite) {
                stringResource(R.string.quotes_favorite_remove_cd, item.code)
              } else {
                stringResource(R.string.quotes_favorite_add_cd, item.code)
              },
            tint =
              if (item.isFavorite) {
                MaterialTheme.colorScheme.tertiary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
          )
        }

        if (!item.isDefault) {
          IconButton(
            onClick = onRemoveCurrency,
            modifier = Modifier.testTag("remove_currency_${item.code}"),
          ) {
            Icon(
              imageVector = Icons.Filled.DeleteOutline,
              contentDescription = stringResource(R.string.quotes_remove_currency_cd, item.code),
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      val quote = item.quote
      if (quote != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Column {
            Text(
              text = stringResource(R.string.quotes_bid_label),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = BrazilianFormatter.formatBrlQuoteRate(quote.bid),
              style = DolarapNumericTypography.quoteBid,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("quote_bid_${item.code}"),
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = stringResource(R.string.quotes_ask_label),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = BrazilianFormatter.formatBrlQuoteRate(quote.ask),
              style = DolarapNumericTypography.quoteSecondary,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag("quote_ask_${item.code}"),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            VariationPill(pctChange = quote.pctChange)
            Text(
              text = BrazilianFormatter.formatVariationBid(quote.varBid),
              style = DolarapNumericTypography.quoteSecondary,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FreshnessBadge(
            status = item.freshnessStatus,
            modifier = Modifier.testTag("quote_freshness_${item.code}"),
          )
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = stringResource(R.string.quotes_unavailable_value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("quote_unavailable_${item.code}"),
          )
          FreshnessBadge(
            status = item.freshnessStatus,
            modifier = Modifier.testTag("quote_freshness_${item.code}"),
          )
        }
      }
    }
  }
}
