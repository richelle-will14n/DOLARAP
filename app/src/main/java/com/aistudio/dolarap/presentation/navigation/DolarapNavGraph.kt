package com.aistudio.dolarap.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.dolarap.R
import com.aistudio.dolarap.presentation.converter.ConverterRoute
import com.aistudio.dolarap.presentation.quotes.QuotesRoute
import com.aistudio.dolarap.presentation.search.CurrencySearchRoute

object DolarapRoutes {
  const val QUOTES = "quotes"
  const val SEARCH = "search"
  const val CONVERTER = "converter"
}

enum class DolarapDestination(
  val route: String,
  @param:StringRes val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val navTestTag: String,
) {
  QUOTES(
    route = DolarapRoutes.QUOTES,
    labelRes = R.string.nav_quotes,
    selectedIcon = Icons.AutoMirrored.Filled.ShowChart,
    unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart,
    navTestTag = "nav_item_quotes",
  ),
  SEARCH(
    route = DolarapRoutes.SEARCH,
    labelRes = R.string.nav_search,
    selectedIcon = Icons.Filled.Search,
    unselectedIcon = Icons.Outlined.Search,
    navTestTag = "nav_item_search",
  ),
  CONVERTER(
    route = DolarapRoutes.CONVERTER,
    labelRes = R.string.nav_converter,
    selectedIcon = Icons.Filled.CurrencyExchange,
    unselectedIcon = Icons.Outlined.CurrencyExchange,
    navTestTag = "nav_item_converter",
  ),
}

fun NavHostController.navigateSingleTopTo(route: String) {
  val currentRoute = currentBackStackEntry?.destination?.route
  if (currentRoute == route) return

  navigate(route) {
    popUpTo(graph.findStartDestination().id) {
      saveState = true
    }
    launchSingleTop = true
    restoreState = true
  }
}

@Composable
fun DolarapApp(
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
  quotesContent: @Composable (onNavigateToSearch: () -> Unit, onNavigateToConverter: () -> Unit) -> Unit =
    { onNavigateToSearch, onNavigateToConverter ->
      QuotesRoute(
        onNavigateToSearch = onNavigateToSearch,
        onNavigateToConverter = onNavigateToConverter,
      )
    },
  searchContent: @Composable () -> Unit = { CurrencySearchRoute() },
  converterContent: @Composable () -> Unit = { ConverterRoute() },
) {
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = backStackEntry?.destination

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    if (useNavigationRail) {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          NavigationRail(
            modifier = Modifier.fillMaxHeight().testTag("dolarap_navigation_rail"),
            containerColor = MaterialTheme.colorScheme.surface,
          ) {
            DolarapDestination.entries.forEach { destination ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
              NavigationRailItem(
                selected = selected,
                onClick = { navController.navigateSingleTopTo(destination.route) },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = stringResource(destination.labelRes),
                  )
                },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag(destination.navTestTag),
              )
            }
          }

          DolarapNavHost(
            navController = navController,
            quotesContent = quotesContent,
            searchContent = searchContent,
            converterContent = converterContent,
            modifier = Modifier.weight(1f).fillMaxHeight(),
          )
        }
      }
    } else {
      Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
          NavigationBar(
            modifier = Modifier.testTag("dolarap_bottom_bar"),
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets.navigationBars,
          ) {
            DolarapDestination.entries.forEach { destination ->
              val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
              NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateSingleTopTo(destination.route) },
                icon = {
                  Icon(
                    imageVector =
                      if (selected) destination.selectedIcon else destination.unselectedIcon,
                    contentDescription = stringResource(destination.labelRes),
                  )
                },
                label = { Text(stringResource(destination.labelRes)) },
                modifier = Modifier.testTag(destination.navTestTag),
              )
            }
          }
        },
      ) { innerPadding ->
        DolarapNavHost(
          navController = navController,
          quotesContent = quotesContent,
          searchContent = searchContent,
          converterContent = converterContent,
          modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
      }
    }
  }
}

@Composable
fun DolarapNavHost(
  navController: NavHostController,
  quotesContent: @Composable (onNavigateToSearch: () -> Unit, onNavigateToConverter: () -> Unit) -> Unit,
  searchContent: @Composable () -> Unit,
  converterContent: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  NavHost(
    navController = navController,
    startDestination = DolarapRoutes.QUOTES,
    modifier = modifier.testTag("dolarap_nav_host"),
  ) {
    composable(route = DolarapRoutes.QUOTES) {
      quotesContent(
        { navController.navigateSingleTopTo(DolarapRoutes.SEARCH) },
        { navController.navigateSingleTopTo(DolarapRoutes.CONVERTER) },
      )
    }

    composable(route = DolarapRoutes.SEARCH) {
      searchContent()
    }

    composable(route = DolarapRoutes.CONVERTER) {
      converterContent()
    }
  }
}
