package com.aistudio.dolarap.presentation.converter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.ConversionResult
import com.aistudio.dolarap.domain.model.CurrencyDefaults
import com.aistudio.dolarap.presentation.components.ErrorStateView
import com.aistudio.dolarap.presentation.components.FeedbackPillBanner
import com.aistudio.dolarap.presentation.components.LoadingStateView
import com.aistudio.dolarap.presentation.components.StatusBannerCard
import com.aistudio.dolarap.presentation.components.asString
import com.aistudio.dolarap.presentation.format.BrazilianFormatter
import com.aistudio.dolarap.presentation.theme.DolarapNumericTypography
import com.aistudio.dolarap.presentation.theme.DolarapThemeTokens
import org.koin.androidx.compose.koinViewModel

private enum class SelectorTarget {
  SOURCE,
  TARGET,
}

@Composable
fun ConverterRoute(
  modifier: Modifier = Modifier,
  viewModel: ConverterViewModel = koinViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  ConverterScreen(
    uiState = uiState,
    onAmountChange = viewModel::onAmountChange,
    onSelectSource = viewModel::selectSourceCurrency,
    onSelectTarget = viewModel::selectTargetCurrency,
    onSwap = viewModel::swapCurrencies,
    onRefreshQuotes = viewModel::refreshQuotes,
    onDismissRefreshError = viewModel::dismissRefreshError,
    modifier = modifier,
  )
}

@Composable
fun ConverterScreen(
  uiState: ConverterUiState,
  onAmountChange: (String) -> Unit,
  onSelectSource: (String) -> Unit,
  onSelectTarget: (String) -> Unit,
  onSwap: () -> Unit,
  onRefreshQuotes: () -> Unit,
  onDismissRefreshError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectorTarget by remember { mutableStateOf<SelectorTarget?>(null) }

  Box(
    modifier = modifier.fillMaxSize().testTag("converter_screen"),
    contentAlignment = Alignment.TopCenter,
  ) {
    when (uiState) {
      ConverterUiState.Loading ->
        LoadingStateView(
          message = stringResource(R.string.converter_loading_message),
          testTag = "converter_loading_state",
        )

      is ConverterUiState.Error ->
        ErrorStateView(
          message = uiState.message.asString(),
          onRetry = onRefreshQuotes,
          testTag = "converter_error_state",
          retryTestTag = "converter_error_retry_button",
        )

      is ConverterUiState.Ready -> {
        val optionsMap = remember(uiState.options) { uiState.options.associateBy { it.code } }
        val sourceOption = optionsMap[uiState.sourceCode]
        val targetOption = optionsMap[uiState.targetCode]

        Column(
          modifier =
            Modifier.fillMaxWidth()
              .widthIn(max = 680.dp)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = stringResource(R.string.converter_title),
              style = MaterialTheme.typography.headlineMedium,
              modifier = Modifier.weight(1f),
            )
            OutlinedButton(
              onClick = onRefreshQuotes,
              enabled = !uiState.isRefreshing,
              modifier = Modifier.testTag("converter_header_refresh_button"),
            ) {
              if (uiState.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              } else {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              }
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = stringResource(R.string.action_refresh))
            }
          }

          uiState.refreshError?.let { refreshErr ->
            FeedbackPillBanner(
              message = refreshErr.asString(),
              isError = true,
              onDismiss = onDismissRefreshError,
              modifier = Modifier.testTag("converter_refresh_error_banner"),
            )
          }

          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              OutlinedTextField(
                value = uiState.rawAmount,
                onValueChange = onAmountChange,
                label = { Text(stringResource(R.string.converter_amount_label)) },
                placeholder = { Text(stringResource(R.string.converter_amount_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = DolarapNumericTypography.quoteBid,
                modifier = Modifier.fillMaxWidth().testTag("converter_amount_input"),
              )

              CurrencySelectorField(
                label = stringResource(R.string.converter_source_label),
                option = sourceOption,
                fallbackCode = uiState.sourceCode,
                onClick = { selectorTarget = SelectorTarget.SOURCE },
                testTag = "converter_source_selector",
              )

              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FilledTonalIconButton(
                  onClick = onSwap,
                  modifier = Modifier.size(48.dp).testTag("converter_swap_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.SwapVert,
                    contentDescription = stringResource(R.string.action_swap_currencies),
                  )
                }
              }

              CurrencySelectorField(
                label = stringResource(R.string.converter_target_label),
                option = targetOption,
                fallbackCode = uiState.targetCode,
                onClick = { selectorTarget = SelectorTarget.TARGET },
                testTag = "converter_target_selector",
              )
            }
          }

          ConversionResultCard(
            resultState = uiState.resultState,
            targetOption = targetOption,
            referenceTimestampMillis = uiState.referenceTimestampMillis,
            isRefreshing = uiState.isRefreshing,
            onRefreshQuotes = onRefreshQuotes,
          )
        }

        selectorTarget?.let { target ->
          val currentlySelected =
            if (target == SelectorTarget.SOURCE) uiState.sourceCode else uiState.targetCode
          CurrencyPickerDialog(
            options = uiState.options,
            selectedCode = currentlySelected,
            onSelect = { code ->
              if (target == SelectorTarget.SOURCE) {
                onSelectSource(code)
              } else {
                onSelectTarget(code)
              }
              selectorTarget = null
            },
            onDismiss = { selectorTarget = null },
          )
        }
      }
    }
  }
}

@Composable
private fun CurrencySelectorField(
  label: String,
  option: ConverterCurrencyOption?,
  fallbackCode: String,
  onClick: () -> Unit,
  testTag: String,
) {
  val code = option?.code ?: fallbackCode
  val displayName = option?.displayName ?: fallbackCode
  val symbol = option?.symbol ?: fallbackCode

  Surface(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().testTag(testTag),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
      ) {
        Text(
          text = code,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "$displayName ($symbol)",
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Icon(
        imageVector = Icons.Filled.ArrowDropDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ConversionResultCard(
  resultState: ConverterResultState,
  targetOption: ConverterCurrencyOption?,
  referenceTimestampMillis: Long?,
  isRefreshing: Boolean,
  onRefreshQuotes: () -> Unit,
) {
  val semantic = DolarapThemeTokens.semanticColors

  when (resultState) {
    ConverterResultState.Empty -> {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("converter_empty_result"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.CurrencyExchange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = stringResource(R.string.converter_empty_amount_title),
              style = MaterialTheme.typography.titleSmall,
            )
            Text(
              text = stringResource(R.string.converter_empty_amount_message),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    is ConverterResultState.InvalidInput -> {
      StatusBannerCard(
        title = stringResource(R.string.converter_amount_label),
        message = resultState.message.asString(),
        icon = Icons.Filled.CurrencyExchange,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.testTag("converter_invalid_input"),
      )
    }

    is ConverterResultState.QuoteUnavailable -> {
      Card(
        modifier = Modifier.fillMaxWidth().testTag("converter_quote_unavailable"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = semantic.warningContainer),
        border = BorderStroke(1.dp, semantic.warning.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.CloudOff,
              contentDescription = null,
              tint = semantic.onWarningContainer,
            )
            Text(
              text = stringResource(R.string.converter_unavailable_quote_title),
              style = MaterialTheme.typography.titleMedium,
              color = semantic.onWarningContainer,
            )
          }
          Text(
            text = resultState.message.asString(),
            style = MaterialTheme.typography.bodyMedium,
            color = semantic.onWarningContainer,
          )
          Button(
            onClick = onRefreshQuotes,
            enabled = !isRefreshing,
            modifier = Modifier.testTag("converter_refresh_quotes_button"),
          ) {
            Icon(
              imageVector = Icons.Filled.Refresh,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.action_refresh))
          }
        }
      }
    }

    is ConverterResultState.Calculated -> {
      val conversion = resultState.result
      val targetSymbol = targetOption?.symbol ?: conversion.targetCode
      val formattedResult =
        BrazilianFormatter.formatCurrencyAmount(
          amount = conversion.convertedAmount,
          currencyCode = conversion.targetCode,
          symbol = targetSymbol,
          decimalPlaces = conversion.decimalPlaces,
        )

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (conversion.isStale) {
          StatusBannerCard(
            title = stringResource(R.string.quotes_stale_banner_title),
            message = stringResource(R.string.converter_stale_warning),
            icon = Icons.Filled.Schedule,
            containerColor = semantic.warningContainer,
            contentColor = semantic.onWarningContainer,
            modifier = Modifier.testTag("converter_stale_banner"),
          )
        }

        Card(
          modifier = Modifier.fillMaxWidth().testTag("converter_result_card"),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = stringResource(R.string.converter_result_label),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = formattedResult,
              style = DolarapNumericTypography.conversionHeadline,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.testTag("converter_result_value"),
            )

            HorizontalDivider(
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
            )

            ConversionRateDetails(
              conversion = conversion,
              referenceTimestampMillis = referenceTimestampMillis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ConversionRateDetails(
  conversion: ConversionResult,
  referenceTimestampMillis: Long?,
) {
  val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
  val rateText =
    "1 ${conversion.sourceCode} = ${BrazilianFormatter.formatRateValue(conversion.effectiveRate)} ${conversion.targetCode}"

  Text(
    text = "${stringResource(R.string.converter_rate_label)}: $rateText",
    style = DolarapNumericTypography.quoteSecondary,
    color = contentColor,
    modifier = Modifier.testTag("converter_rate_used"),
  )

  val brl = CurrencyDefaults.REFERENCE_CURRENCY_CODE
  when {
    conversion.sourceCode != brl && conversion.targetCode != brl &&
      conversion.sourceBidRate != null && conversion.targetBidRate != null -> {
      Text(
        text =
          stringResource(
            R.string.converter_cross_info_format,
            conversion.sourceCode,
            BrazilianFormatter.formatBrlQuoteRate(conversion.sourceBidRate),
            conversion.targetCode,
            BrazilianFormatter.formatBrlQuoteRate(conversion.targetBidRate),
          ),
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.85f),
      )
    }
    conversion.sourceAskRate != null -> {
      Text(
        text =
          stringResource(
            R.string.converter_ask_info_format,
            BrazilianFormatter.formatBrlQuoteRate(conversion.sourceAskRate),
          ),
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.85f),
      )
    }
    conversion.targetAskRate != null -> {
      Text(
        text =
          stringResource(
            R.string.converter_ask_info_format,
            BrazilianFormatter.formatBrlQuoteRate(conversion.targetAskRate),
          ),
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.85f),
      )
    }
  }

  referenceTimestampMillis?.let { ts ->
    Text(
      text =
        stringResource(
          R.string.converter_quote_date_format,
          BrazilianFormatter.formatDateTime(ts),
        ),
      style = MaterialTheme.typography.bodySmall,
      color = contentColor.copy(alpha = 0.85f),
      modifier = Modifier.testTag("converter_quote_timestamp"),
    )
  }
}

@Composable
private fun CurrencyPickerDialog(
  options: List<ConverterCurrencyOption>,
  selectedCode: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.converter_selector_title)) },
    text = {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("converter_currency_picker_list"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        items(options, key = { it.code }) { option ->
          val isSelected = option.code == selectedCode
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onSelect(option.code) }
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .testTag("converter_currency_option_${option.code}"),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = option.code,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.width(52.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = option.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (!option.hasLocalQuote) {
                Text(
                  text = stringResource(R.string.quotes_badge_unavailable),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (isSelected) {
              Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_close))
      }
    },
  )
}
