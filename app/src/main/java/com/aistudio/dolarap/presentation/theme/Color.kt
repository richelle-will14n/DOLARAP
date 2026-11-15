package com.aistudio.dolarap.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Marca DOLARAP — Verde Esmeralda Profundo + Ouro Sóbrio + Superfícies Neutras Financeiras
val EmeraldPrimaryLight = Color(0xFF0B4F3A)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldPrimaryContainerLight = Color(0xFFC4F1DF)
val EmeraldOnPrimaryContainerLight = Color(0xFF002116)

val SlateSecondaryLight = Color(0xFF3E564C)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateSecondaryContainerLight = Color(0xFFD5E8DD)
val SlateOnSecondaryContainerLight = Color(0xFF14251E)

val GoldTertiaryLight = Color(0xFF8C6500)
val GoldOnTertiaryLight = Color(0xFFFFFFFF)
val GoldTertiaryContainerLight = Color(0xFFFFE08D)
val GoldOnTertiaryContainerLight = Color(0xFF2C1E00)

val SurfaceBackgroundLight = Color(0xFFF4F7F5)
val SurfaceCardLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFE5ECE8)
val OnSurfaceLight = Color(0xFF141D19)
val OnSurfaceVariantLight = Color(0xFF475750)
val OutlineLight = Color(0xFF8C9B93)
val OutlineVariantLight = Color(0xFFD2DDD7)

val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

// Tema Escuro
val EmeraldPrimaryDark = Color(0xFF7ADBA9)
val EmeraldOnPrimaryDark = Color(0xFF003827)
val EmeraldPrimaryContainerDark = Color(0xFF00513B)
val EmeraldOnPrimaryContainerDark = Color(0xFFC4F1DF)

val SlateSecondaryDark = Color(0xFFB5CCBF)
val SlateOnSecondaryDark = Color(0xFF21352B)
val SlateSecondaryContainerDark = Color(0xFF2D4338)
val SlateOnSecondaryContainerDark = Color(0xFFD5E8DD)

val GoldTertiaryDark = Color(0xFFF5D061)
val GoldOnTertiaryDark = Color(0xFF432E00)
val GoldTertiaryContainerDark = Color(0xFF604300)
val GoldOnTertiaryContainerDark = Color(0xFFFFE08D)

val SurfaceBackgroundDark = Color(0xFF0D1512)
val SurfaceCardDark = Color(0xFF15201B)
val SurfaceVariantDark = Color(0xFF202E28)
val OnSurfaceDark = Color(0xFFE3ECE7)
val OnSurfaceVariantDark = Color(0xFFAABAB1)
val OutlineDark = Color(0xFF6E7F76)
val OutlineVariantDark = Color(0xFF2A3B33)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)

@Immutable
data class DolarapSemanticColors(
  val positive: Color,
  val positiveContainer: Color,
  val positiveBorder: Color,
  val negative: Color,
  val negativeContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val warningBorder: Color,
  val onWarningContainer: Color,
  val heroGradientStart: Color,
  val heroGradientEnd: Color,
  val heroBorder: Color,
  val onHeroBrand: Color,
  val onHeroTitle: Color,
  val onHeroSecondary: Color,
  val heroButtonContainer: Color,
  val heroButtonContent: Color,
  val heroProgressIndicator: Color,
  val heroProgressTrack: Color,
)

val LightDolarapSemanticColors =
  DolarapSemanticColors(
    positive = Color(0xFF0A5C3B),
    positiveContainer = Color(0xFFD5F2E3),
    positiveBorder = Color(0xFF9ED5B9),
    negative = Color(0xFFB3261E),
    negativeContainer = Color(0xFFFCE8E6),
    warning = Color(0xFF8A4F00),
    warningContainer = Color(0xFFFFEECD),
    warningBorder = Color(0xFFE0BC7A),
    onWarningContainer = Color(0xFF3E2400),
    heroGradientStart = Color(0xFFE6F3EC),
    heroGradientEnd = Color(0xFFF3EFE2),
    heroBorder = Color(0xFFBBD7C8),
    onHeroBrand = Color(0xFF634600),
    onHeroTitle = Color(0xFF0A2E21),
    onHeroSecondary = Color(0xFF28473B),
    heroButtonContainer = Color(0xFF0B4F3A),
    heroButtonContent = Color(0xFFFFFFFF),
    heroProgressIndicator = Color(0xFF0B4F3A),
    heroProgressTrack = Color(0xFFCDE2D6),
  )

val DarkDolarapSemanticColors =
  DolarapSemanticColors(
    positive = Color(0xFF85E3B3),
    positiveContainer = Color(0xFF144632),
    positiveBorder = Color(0xFF2A6D50),
    negative = Color(0xFFF2B8B5),
    negativeContainer = Color(0xFF4A1A18),
    warning = Color(0xFFFFD07B),
    warningContainer = Color(0xFF422D05),
    warningBorder = Color(0xFF705018),
    onWarningContainer = Color(0xFFFFE6B8),
    heroGradientStart = Color(0xFF1A2D25),
    heroGradientEnd = Color(0xFF262A1F),
    heroBorder = Color(0xFF364D42),
    onHeroBrand = Color(0xFFF5D061),
    onHeroTitle = Color(0xFFF1F7F4),
    onHeroSecondary = Color(0xFFC7DBD0),
    heroButtonContainer = Color(0xFF7ADBA9),
    heroButtonContent = Color(0xFF003827),
    heroProgressIndicator = Color(0xFF7ADBA9),
    heroProgressTrack = Color(0xFF31473C),
  )

