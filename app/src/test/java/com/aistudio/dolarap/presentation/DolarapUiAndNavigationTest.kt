package com.aistudio.dolarap.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.aistudio.dolarap.R
import com.aistudio.dolarap.domain.model.QuoteFreshnessStatus
import com.aistudio.dolarap.domain.service.CurrencyConverter
import com.aistudio.dolarap.domain.service.QuoteFreshnessEvaluator
import com.aistudio.dolarap.domain.service.TimeProvider
import com.aistudio.dolarap.domain.usecase.AddCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ConvertCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.ObserveDashboardQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RefreshCatalogUseCase
import com.aistudio.dolarap.domain.usecase.RefreshQuotesUseCase
import com.aistudio.dolarap.domain.usecase.RemoveCurrencyUseCase
import com.aistudio.dolarap.domain.usecase.SearchAvailableCurrenciesUseCase
import com.aistudio.dolarap.domain.usecase.ToggleFavoriteUseCase
import com.aistudio.dolarap.presentation.converter.ConverterScreen
import com.aistudio.dolarap.presentation.converter.ConverterViewModel
import com.aistudio.dolarap.presentation.navigation.DolarapApp
import com.aistudio.dolarap.presentation.navigation.DolarapRoutes
import com.aistudio.dolarap.presentation.quotes.QuotesScreen
import com.aistudio.dolarap.presentation.quotes.QuotesUiState
import com.aistudio.dolarap.presentation.quotes.QuotesViewModel
import com.aistudio.dolarap.presentation.search.CurrencySearchScreen
import com.aistudio.dolarap.presentation.search.CurrencySearchViewModel
import com.aistudio.dolarap.presentation.theme.DarkDolarapSemanticColors
import com.aistudio.dolarap.presentation.theme.DolarapTheme
import com.aistudio.dolarap.presentation.theme.LightDolarapSemanticColors
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DolarapUiAndNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `navigates across all three screens without creating duplicate destinations on backstack`() {
    val quoteRepo = FakeQuoteRepository()
    val catalogRepo = FakeCatalogRepository(quoteRepository = quoteRepo)
    val quotesViewModel =
      QuotesViewModel(
        observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(quoteRepo),
        refreshQuotesUseCase = RefreshQuotesUseCase(quoteRepo),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(quoteRepo),
        removeCurrencyUseCase = RemoveCurrencyUseCase(quoteRepo),
        refreshOnStart = false,
      )
    val searchViewModel =
      CurrencySearchViewModel(
        searchAvailableCurrenciesUseCase = SearchAvailableCurrenciesUseCase(catalogRepo),
        refreshCatalogUseCase = RefreshCatalogUseCase(catalogRepo),
        addCurrencyUseCase = AddCurrencyUseCase(catalogRepo, quoteRepo),
        removeCurrencyUseCase = RemoveCurrencyUseCase(quoteRepo),
        syncIfEmptyOnStart = false,
      )
    val converterViewModel =
      ConverterViewModel(
        observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(quoteRepo),
        convertCurrencyUseCase =
          ConvertCurrencyUseCase(
            quoteRepository = quoteRepo,
            currencyConverter = CurrencyConverter(),
            freshnessEvaluator = QuoteFreshnessEvaluator(),
            timeProvider = TimeProvider { 1_700_000_100_000L },
          ),
        refreshQuotesUseCase = RefreshQuotesUseCase(quoteRepo),
      )

    lateinit var navController: NavHostController

    composeTestRule.setContent {
      navController = rememberNavController()
      DolarapTheme {
        DolarapApp(
          navController = navController,
          quotesContent = { onNavigateToSearch, onNavigateToConverter ->
            val state = quotesViewModel.uiState.collectAsStateWithLifecycle().value
            QuotesScreen(
              uiState = state,
              onRefresh = quotesViewModel::refreshQuotes,
              onSetFavoritesOnly = quotesViewModel::setFavoritesOnly,
              onToggleFavorite = quotesViewModel::toggleFavorite,
              onRemoveCurrency = quotesViewModel::removeCurrency,
              onDismissBannerError = quotesViewModel::dismissBannerError,
              onDismissFeedback = quotesViewModel::dismissFeedback,
              onNavigateToSearch = onNavigateToSearch,
              onNavigateToConverter = onNavigateToConverter,
            )
          },
          searchContent = {
            val state = searchViewModel.uiState.collectAsStateWithLifecycle().value
            CurrencySearchScreen(
              uiState = state,
              onQueryChange = searchViewModel::onQueryChange,
              onClearQuery = searchViewModel::clearQuery,
              onRefreshCatalog = searchViewModel::refreshCatalog,
              onAddCurrency = searchViewModel::addCurrency,
              onRemoveCurrency = searchViewModel::removeCurrency,
              onDismissMessages = searchViewModel::dismissMessages,
            )
          },
          converterContent = {
            val state = converterViewModel.uiState.collectAsStateWithLifecycle().value
            ConverterScreen(
              uiState = state,
              onAmountChange = converterViewModel::onAmountChange,
              onSelectSource = converterViewModel::selectSourceCurrency,
              onSelectTarget = converterViewModel::selectTargetCurrency,
              onSwap = converterViewModel::swapCurrencies,
              onRefreshQuotes = converterViewModel::refreshQuotes,
              onDismissRefreshError = converterViewModel::dismissRefreshError,
            )
          },
        )
      }
    }

    composeTestRule.waitForIdle()
    assertEquals(DolarapRoutes.QUOTES, navController.currentBackStackEntry?.destination?.route)
    composeTestRule.onNodeWithTag("quotes_screen").assertIsDisplayed()

    // Navega para Pesquisa pelo botão rápido da tela de Cotações
    composeTestRule.onNodeWithTag("quotes_go_search_button").performClick()
    composeTestRule.waitForIdle()
    assertEquals(DolarapRoutes.SEARCH, navController.currentBackStackEntry?.destination?.route)
    composeTestRule.onNodeWithTag("search_screen").assertIsDisplayed()

    // Navega para Conversor pela barra de navegação
    composeTestRule.onNodeWithTag("nav_item_converter").performClick()
    composeTestRule.waitForIdle()
    assertEquals(DolarapRoutes.CONVERTER, navController.currentBackStackEntry?.destination?.route)
    composeTestRule.onNodeWithTag("converter_screen").assertIsDisplayed()

    // Tocar novamente no mesmo destino não duplica entrada na pilha
    composeTestRule.onNodeWithTag("nav_item_converter").performClick()
    composeTestRule.waitForIdle()
    val routesOnStack =
      navController.currentBackStack.value.mapNotNull { it.destination.route }
    assertEquals(routesOnStack.distinct().size, routesOnStack.size)

    // Retorna para Cotações
    composeTestRule.onNodeWithTag("nav_item_quotes").performClick()
    composeTestRule.waitForIdle()
    assertEquals(DolarapRoutes.QUOTES, navController.currentBackStackEntry?.destination?.route)
  }

  @Test
  fun `quotes screen renders Loading, Empty favorites, Stale banner, and toggles favorites`() {
    val staleSnapshot =
      FakeQuoteRepository.defaultSnapshot(freshnessStatus = QuoteFreshnessStatus.STALE)
    val repo = FakeQuoteRepository(staleSnapshot)
    val viewModel =
      QuotesViewModel(
        observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(repo),
        refreshQuotesUseCase = RefreshQuotesUseCase(repo),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(repo),
        removeCurrencyUseCase = RemoveCurrencyUseCase(repo),
        refreshOnStart = false,
      )

    composeTestRule.setContent {
      DolarapTheme {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        QuotesScreen(
          uiState = state,
          onRefresh = viewModel::refreshQuotes,
          onSetFavoritesOnly = viewModel::setFavoritesOnly,
          onToggleFavorite = viewModel::toggleFavorite,
          onRemoveCurrency = viewModel::removeCurrency,
          onDismissBannerError = viewModel::dismissBannerError,
          onDismissFeedback = viewModel::dismissFeedback,
          onNavigateToSearch = {},
          onNavigateToConverter = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quotes_stale_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_list").performScrollToNode(hasTestTag("quote_card_USD"))
    composeTestRule.onNodeWithTag("quote_bid_USD").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quote_ask_USD").assertIsDisplayed()

    // As 10 moedas padrão não devem exibir botão de remoção
    composeTestRule.onAllNodesWithTag("remove_currency_USD").assertCountEquals(0)

    // Alternar filtro Favoritas sem favoritos exibe EmptyState
    composeTestRule.onNodeWithTag("quotes_list").performScrollToNode(hasTestTag("quotes_filter_favorites"))
    composeTestRule.onNodeWithTag("quotes_filter_favorites").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quotes_empty_state").assertIsDisplayed()

    // Volta para Todas, marca USD como favorita e filtra novamente
    composeTestRule.onNodeWithTag("quotes_empty_action_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quotes_list").performScrollToNode(hasTestTag("quote_card_USD"))
    composeTestRule.onNodeWithTag("favorite_toggle_USD").performClick()
    composeTestRule.onNodeWithTag("quotes_list").performScrollToNode(hasTestTag("quotes_filter_favorites"))
    composeTestRule.onNodeWithTag("quotes_filter_favorites").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quotes_list").performScrollToNode(hasTestTag("quote_card_USD"))
    composeTestRule.onNodeWithTag("quote_card_USD").assertIsDisplayed()
  }

  @Test
  fun `quotes screen renders explicit Loading and Error states with retry`() {
    var uiState: QuotesUiState by mutableStateOf(QuotesUiState.Loading)
    var retried = false
    composeTestRule.setContent {
      DolarapTheme {
        QuotesScreen(
          uiState = uiState,
          onRefresh = { retried = true },
          onSetFavoritesOnly = {},
          onToggleFavorite = {},
          onRemoveCurrency = {},
          onDismissBannerError = {},
          onDismissFeedback = {},
          onNavigateToSearch = {},
          onNavigateToConverter = {},
        )
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quotes_loading_state").assertIsDisplayed()

    uiState =
      QuotesUiState.Error(
        message = com.aistudio.dolarap.presentation.format.UiText(R.string.error_unexpected)
      )
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("quotes_error_state").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_error_retry_button").performClick()
    assertEquals(true, retried)
  }

  @Test
  fun `search screen filters currencies and adds additional currency`() {
    val quoteRepo = FakeQuoteRepository()
    val catalogRepo = FakeCatalogRepository(quoteRepository = quoteRepo)
    val viewModel =
      CurrencySearchViewModel(
        searchAvailableCurrenciesUseCase = SearchAvailableCurrenciesUseCase(catalogRepo),
        refreshCatalogUseCase = RefreshCatalogUseCase(catalogRepo),
        addCurrencyUseCase = AddCurrencyUseCase(catalogRepo, quoteRepo),
        removeCurrencyUseCase = RemoveCurrencyUseCase(quoteRepo),
        syncIfEmptyOnStart = false,
      )

    composeTestRule.setContent {
      DolarapTheme {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        CurrencySearchScreen(
          uiState = state,
          onQueryChange = viewModel::onQueryChange,
          onClearQuery = viewModel::clearQuery,
          onRefreshCatalog = viewModel::refreshCatalog,
          onAddCurrency = viewModel::addCurrency,
          onRemoveCurrency = viewModel::removeCurrency,
          onDismissMessages = viewModel::dismissMessages,
        )
      }
    }

    composeTestRule.waitForIdle()
    // Moeda padrão USD nunca exibe botão remover
    composeTestRule.onAllNodesWithTag("search_remove_button_USD").assertCountEquals(0)

    composeTestRule.onNodeWithTag("search_currency_input").performTextReplacement("norueguesa")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_item_NOK").assertIsDisplayed()

    composeTestRule.onNodeWithTag("search_add_button_NOK").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("search_feedback_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("search_remove_button_NOK").assertIsDisplayed()
  }

  @Test
  fun `converter screen calculates, swaps currencies, and handles unavailable quote`() {
    val repo = FakeQuoteRepository()
    val viewModel =
      ConverterViewModel(
        observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(repo),
        convertCurrencyUseCase =
          ConvertCurrencyUseCase(
            quoteRepository = repo,
            currencyConverter = CurrencyConverter(),
            freshnessEvaluator = QuoteFreshnessEvaluator(),
            timeProvider = TimeProvider { 1_700_000_100_000L },
          ),
        refreshQuotesUseCase = RefreshQuotesUseCase(repo),
      )

    composeTestRule.setContent {
      DolarapTheme {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        ConverterScreen(
          uiState = state,
          onAmountChange = viewModel::onAmountChange,
          onSelectSource = viewModel::selectSourceCurrency,
          onSelectTarget = viewModel::selectTargetCurrency,
          onSwap = viewModel::swapCurrencies,
          onRefreshQuotes = viewModel::refreshQuotes,
          onDismissRefreshError = viewModel::dismissRefreshError,
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("converter_amount_input").performScrollTo().performTextReplacement("100")
    composeTestRule.waitForIdle()
    // 100 USD * 5.5000 = R$ 550,00
    composeTestRule.onNodeWithTag("converter_result_value").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("converter_result_value").assertTextEquals("R$ 550,00")
    composeTestRule.onNodeWithTag("converter_rate_used").performScrollTo().assertIsDisplayed()

    // Inverter para BRL -> USD
    composeTestRule.onNodeWithTag("converter_swap_button").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("converter_amount_input").performScrollTo().performTextReplacement("55")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("converter_result_value").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("converter_result_value").assertTextEquals("US$ 10,00")

    // Sem cotação local disponível
    repo.setSnapshot(FakeQuoteRepository.defaultSnapshot(withQuotes = false))
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("converter_quote_unavailable").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `quotes header card renders solid high-contrast elements in both light and dark themes`() {
    val repo = FakeQuoteRepository()
    val viewModel =
      QuotesViewModel(
        observeDashboardQuotesUseCase = ObserveDashboardQuotesUseCase(repo),
        refreshQuotesUseCase = RefreshQuotesUseCase(repo),
        toggleFavoriteUseCase = ToggleFavoriteUseCase(repo),
        removeCurrencyUseCase = RemoveCurrencyUseCase(repo),
        refreshOnStart = false,
      )

    var useDarkTheme by mutableStateOf(false)

    composeTestRule.setContent {
      DolarapTheme(darkTheme = useDarkTheme) {
        val state = viewModel.uiState.collectAsStateWithLifecycle().value
        QuotesScreen(
          uiState = state,
          onRefresh = viewModel::refreshQuotes,
          onSetFavoritesOnly = viewModel::setFavoritesOnly,
          onToggleFavorite = viewModel::toggleFavorite,
          onRemoveCurrency = viewModel::removeCurrency,
          onDismissBannerError = viewModel::dismissBannerError,
          onDismissFeedback = viewModel::dismissFeedback,
          onNavigateToSearch = {},
          onNavigateToConverter = {},
        )
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("DOLARAP").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_last_sync_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_global_freshness_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_refresh_button").assertIsDisplayed()

    useDarkTheme = true
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("DOLARAP").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_last_sync_text").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_global_freshness_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("quotes_refresh_button").assertIsDisplayed()

    listOf(LightDolarapSemanticColors, DarkDolarapSemanticColors).forEach { palette ->
      assertEquals(1f, palette.onHeroBrand.alpha, 0.001f)
      assertEquals(1f, palette.onHeroTitle.alpha, 0.001f)
      assertEquals(1f, palette.onHeroSecondary.alpha, 0.001f)
      assertEquals(1f, palette.heroButtonContent.alpha, 0.001f)

      listOf(palette.heroGradientStart, palette.heroGradientEnd).forEach { bg ->
        assertTrue(contrastRatio(palette.onHeroBrand, bg) >= 4.5f)
        assertTrue(contrastRatio(palette.onHeroTitle, bg) >= 7.0f)
        assertTrue(contrastRatio(palette.onHeroSecondary, bg) >= 7.0f)
      }
      assertTrue(contrastRatio(palette.heroButtonContent, palette.heroButtonContainer) >= 7.0f)
      assertTrue(contrastRatio(palette.positive, palette.positiveContainer) >= 4.5f)
      assertTrue(contrastRatio(palette.warning, palette.warningContainer) >= 4.5f)
    }
  }

  private fun contrastRatio(foreground: Color, background: Color): Float {
    val l1 = foreground.luminance()
    val l2 = background.luminance()
    val lighter = max(l1, l2)
    val darker = min(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
  }
}
