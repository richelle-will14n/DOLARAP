# DOLARAP

> **Estado atual do projeto:** **Etapas E00–E06 aprovadas e Etapa E07 (Finalização, APK final e documentação consolidada) concluída — aguardando revisão final externa.**
> Suíte automatizada verificada com **92/92 testes aprovados** em 24 suítes (`gradle :app:testDebugUnitTest`) e APK Debug final gerado com `BuildConfig.AWESOME_API_KEY` embutida via Secrets Gradle Plugin (`22.590.182` bytes / `21,54 MiB`). Testes manuais (`MT-01`–`MT-11`) e instrumentados (`androidTest`) foram **dispensados / não executados**.

Aplicativo Android nativo para consulta e conversão de cotações de moedas estrangeiras em relação ao Real brasileiro (**BRL**), projetado com foco em **Offline First**, precisão monetária (`BigDecimal`) e arquitetura limpa em módulo único (`:app`).

---

## Progresso do Desenvolvimento (E00–E07)

| Etapa | Descrição | Status |
|---|---|---|
| **E00** | Especificação e planejamento (`SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `README.md`, `TEST_RESULTS.md`) | **Aprovada** |
| **E01** | Preparação do projeto Android (`:app`, `com.aistudio.dolarap`, dependências base, ícone, Koin bootstrap) | **Aprovada** |
| **E02** | Banco de dados local Room (entidades, relação, DAOs com `Flow`, seed das 10 moedas padrão, Koin e testes) | **Aprovada** |
| **E03** | Integração com a AwesomeAPI (Retrofit/OkHttp/Moshi, `/json/last/{pairs}`, `/json/available` + fallback `/xml/available`, tratamento de erros e `CoinNotExists`) | **Aprovada** |
| **E04** | Camada de domínio e repositórios (modelos puros, `BigDecimal`, UseCases, regra de 15 min, SSOT Room, `dataModule`/`domainModule`) | **Aprovada** |
| **E05** | Interface Jetpack Compose (Cotações, Pesquisa/Adição, Conversor, Filtro de Favoritos, Navigation Compose, `PresentationModule`) | **Aprovada** |
| **E06** | Offline First, sincronização com WorkManager (`SyncQuotesWorker`, periódico 6h, retry `CONNECTED`, `Retry-After` e estados unificados) | **Aprovada** |
| **E07** | Entrega final, documentação consolidada, APK debug (`AWESOME_API_KEY` via Secrets) e organização para GitHub | **Implementada — aguardando revisão final** |

---

## Funcionalidades Implementadas vs. Planejadas

### Já implementado (até a Etapa E06)
- **Estrutura base Android (`E01`):** Módulo único `:app` com `namespace` e `applicationId` configurados para `com.aistudio.dolarap`, permissões `INTERNET` e `ACCESS_NETWORK_STATE`, ícone adaptativo customizado e `MainActivity` com `enableEdgeToEdge()` e `DolarapTheme`.
- **Inicialização de DI com Koin e WorkManager (`E01`–`E06`):** `DolarapApplication` inicializando Koin (`appModules`), implementando `Configuration.Provider` com `KoinWorkerFactory` (com `WorkManagerInitializer` padrão removido no `AndroidManifest.xml`) e agendando o trabalho periódico único de 6 horas na inicialização. Reúne `databaseModule` (Room + DAOs), `networkModule` (Retrofit/OkHttp/Moshi/AwesomeAPI com `AwesomeApiAuthInterceptor`), `dataModule` (mappers, `TimeProvider`, repositórios SSOT, `SyncScheduler` e `SyncQuotesWorker`), `domainModule` (`CurrencyConverter`, `QuoteFreshnessEvaluator` e os 9 UseCases) e `presentationModule` (`QuotesViewModel`, `CurrencySearchViewModel`, `ConverterViewModel`).
- **Persistência local com Room (`E02`):**
  - **4 entidades Room:** `SelectedCurrencyEntity` (`selected_currencies`), `QuoteEntity` (`quotes`), `AvailablePairEntity` (`available_pairs`) e `SyncMetadataEntity` (`sync_metadata`).
  - **Relação Room:** `SelectedCurrencyWithQuote` (`@Embedded` + `@Relation`) unindo cada moeda selecionada à sua cotação opcional (`0..1`).
  - **4 DAOs com consultas reativas via `Flow`:** `CurrencyDao`, `QuoteDao`, `AvailablePairDao` e `SyncMetadataDao`.
  - **Seed inicial e catálogo complementar (`E02`):** `DolarapDatabase` semeia de forma atômica e idempotente as **10 moedas padrão** (`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY`, `MXN`); `ComplementaryCurrencyCatalog` enriquece nomes amigáveis em PT-BR e normalização de busca.
- **Integração remota com a AwesomeAPI (`E03` + API Key):**
  - **Configuração HTTP centralizada (`AwesomeApiConfig`, `NetworkModule`):** Retrofit + OkHttpClient (timeouts de 15s, `AwesomeApiAuthInterceptor` enviando `x-api-key` via `BuildConfig.AWESOME_API_KEY` quando configurada e `HttpLoggingInterceptor` com ocultação do cabeçalho sensível) + Moshi (`KotlinJsonAdapterFactory` e codegen KSP).
  - **Consulta de cotações (`AwesomeApiService.getLastQuotes`, `QuoteRemoteMapper`):** Consome `/json/last/{pairs}` para múltiplos pares válidos no padrão `^[A-Z]{3}-BRL$`, preservando decimais exatos como string em `QuoteEntity`.
  - **Catálogo oficial com tentativa experimental JSON (`AvailableCatalogParser`, `AwesomeApiRemoteDataSource`):** `/xml/available` é a fonte oficialmente documentada; `/json/available` atua apenas como tentativa experimental com fallback XML (bloqueando segunda chamada em HTTP 429, 5xx, timeout ou offline).
  - **Tratamento defensivo de erros (`RemoteResult`, `RemoteError`, `ApiErrorDto`):** Trata desconexão, timeout, HTTP 404 (`CoinNotExists` em consulta simples ou em lote), HTTP 429 (incluindo extração do cabeçalho `Retry-After` em segundos ou HTTP-date), 5xx e payloads malformados sem derrubar o app nem apagar dados locais.
- **Camada de Domínio pura e Repositórios SSOT (`E04`):**
  - **Camada `domain` pura (sem imports Android/Room/Retrofit/Moshi/Compose):** Modelos (`CurrencyQuote`, `QuoteRate`, `DashboardQuotesSnapshot`, `SyncStatus`, `QuoteFreshnessStatus`, `AvailableCurrency`, `ConversionResult`, `CurrencyDefaults`, `DomainResult`/`DomainError`), contratos (`QuoteRepository`, `CatalogRepository`, `TimeProvider`, `SyncScheduler`/`BackgroundSyncInfo`/`BackgroundSyncState`), serviços (`CurrencyConverter` em `BigDecimal`, `QuoteFreshnessEvaluator` com regra de 15 minutos) e 9 UseCases (`ObserveDashboardQuotesUseCase`, `RefreshQuotesUseCase`, `ObserveAvailableCurrenciesUseCase`, `SearchAvailableCurrenciesUseCase`, `RefreshCatalogUseCase`, `AddCurrencyUseCase`, `RemoveCurrencyUseCase`, `ToggleFavoriteUseCase`, `ConvertCurrencyUseCase`).
  - **Conversor monetário em `BigDecimal`:** Suporta `X -> BRL`, `BRL -> Y`, `X -> Y` (triangulada via BRL) e `X -> X` utilizando exclusivamente `bid` como taxa padrão (`ask` mantido como dado complementar), intermediários em `MathContext.DECIMAL128`, escala da moeda de destino via ISO 4217 (ex.: `JPY` 0 casas, `BRL`/`USD`/`EUR` 2 casas, `BTC`/`ETH`/`LTC` 8 casas) e `RoundingMode.HALF_UP`.
  - **Regra de 15 minutos (`QuoteFreshnessEvaluator`):** Trata ausência de timestamp como `UNAVAILABLE`, até `900_000 ms` como `FRESH`, acima de 15 minutos como `STALE` e timestamps inválidos/futuros defensivamente como `STALE`.
  - **Repositórios SSOT (`QuoteRepositoryImpl`, `CatalogRepositoryImpl`):** Conectam a AwesomeAPI ao Room usando o Room como Fonte Única da Verdade via `Flow`, deduplicam sincronizações concorrentes (`Mutex` + `CompletableDeferred`), particionam lotes de cotações (`MAX_BATCH_SIZE = 25`), protegem as 10 moedas padrão e preservam o cache local intacto em falhas de rede/API.
- **Interface com Jetpack Compose, Material 3 e Navigation Compose (`E05`):**
  - **Identidade visual e formatação (`presentation/theme`, `presentation/format`, `presentation/components`):** Paleta Material 3 esmeralda/dourado (`Color.kt`/`Theme.kt`), tipografia local embarcada (`Plus Jakarta Sans` e `JetBrains Mono` com algarismos tabulares `tnum` em `Type.kt`), `BrazilianFormatter` (formatação `pt-BR` direto de `BigDecimal`, sem conversão para `Double`/`Float`), `DomainErrorMapper` + `UiText` e componentes reutilizáveis (`FreshnessBadge`, `VariationPill`, `StatusBannerCard`, `LoadingStateView`, `EmptyStateView`, `ErrorStateView`).
  - **Navegação (`DolarapNavGraph.kt`):** `DolarapApp` e `DolarapNavHost` com `NavigationBar` em telas compactas e `NavigationRail` em telas `>= 600dp`, preservação de estado entre abas (`saveState`/`restoreState`) e prevenção de duplicação de destinos na backstack (`navigateSingleTopTo`).
  - **Telas MVVM (`QuotesScreen`, `CurrencySearchScreen`, `ConverterScreen`):** Exibição imediata das 10 moedas padrão + adicionais via Room `Flow`, `bid`/`ask`, variação, favoritos, pesquisa em tempo real, conversor em `BigDecimal`, banners não destrutivos de desatualização (> 15 min) e estados explícitos `Loading`, `Success`, `Empty` e `Error`.
- **Sincronização em Segundo Plano e Offline First Completo (`E06`):**
  - **Contrato puro `SyncScheduler` + `WorkManagerSyncScheduler`:** Agenda trabalho periódico único de 6 horas (`dolarap_periodic_quotes_sync`, `ExistingPeriodicWorkPolicy.KEEP`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL`) na inicialização e trabalho único de retry condicionado à rede (`dolarap_connectivity_retry_quotes_sync`, `ExistingWorkPolicy.KEEP` ou `REPLACE` com `initialDelay` quando há `Retry-After`) após falhas transitórias ou rate limit, expondo `Flow<BackgroundSyncInfo>`.
  - **`SyncQuotesWorker`:** Executa `QuoteRepository.refreshQuotes()` sem duplicar lógica HTTP/Room; aplica retry limitado (`MAX_RETRY_ATTEMPTS = 3`) para falhas temporárias (`NoConnection`, `Timeout`, 5xx, lock transitório de SQLite), respeita `Retry-After` no HTTP 429 agendando retry atrasado, falha imediatamente em erros permanentes sem loop infinito e preserva integralmente os dados e timestamps do Room em qualquer falha.
  - **Estado unificado na UI (`QuotesViewModel` + `QuotesUiState.syncPhase` + `QuotesScreen`):** Distingue sem contradições aguardando primeira sincronização, sincronizando (foreground ou worker em execução), concluído, offline, falha de sincronização, dados locais disponíveis e dados desatualizados (> 15 min), exibindo banners não destrutivos sem esconder cotações locais.
- **Testes automatizados (`E01`–`E06`, revalidados na `E07`):** **92 testes** em 24 suítes (unitários, integração local Room/MockWebServer/WorkManager, DI Koin e Compose UI) aprovados em `gradle :app:testDebugUnitTest`.
- **Entrega final (`E07`):** Revisão final da arquitetura, build de APK Debug com `AWESOME_API_KEY` incorporada pelo Secrets Gradle Plugin no ambiente do Google AI Studio (`app/build/outputs/apk/debug/app-debug.apk`), atualização completa da documentação e verificação do `.gitignore` para versionamento seguro no Android Studio/GitHub.

### Itens dispensados / fora do escopo desta entrega
- **Testes manuais (`MT-01`–`MT-11`) e instrumentados (`androidTest`):** Dispensados pelo responsável pelo projeto e não executados (ambiente em container sem ADB/emulador acessível ao agente).
- **Fora de escopo:** Login, integração bancária, pagamentos, gráficos históricos, alertas/notificações e backend próprio.

---

## Arquitetura e Tecnologias

### Arquitetura Atual e Planejada
O projeto adota **Clean Architecture + MVVM** em módulo único `:app` (`com.aistudio.dolarap`), dividido em quatro pacotes principais:
- `presentation`: implementada na E05 e integrada na E06 com tema Material 3 (`DolarapTheme` + fontes locais `Plus Jakarta Sans` e `JetBrains Mono`), `BrazilianFormatter`, `DomainErrorMapper`, componentes compartilhados, `DolarapNavGraph` e as três telas MVVM (`Quotes`, `CurrencySearch`, `Converter`) consumindo exclusivamente os UseCases da E04 e `SyncScheduler` (sem acesso direto a Room, DAOs ou Retrofit).
- `domain`: implementado na E04 e estendido na E06 como camada Kotlin/JDK pura (modelos, erros tipados, contratos `QuoteRepository`/`CatalogRepository`/`SyncScheduler`, `CurrencyConverter`, `QuoteFreshnessEvaluator`, `TimeProvider` e 9 UseCases — zero dependências de Android/Room/Retrofit/Moshi/Compose/WorkManager).
- `data`: contém `data.local` (E02: `DolarapDatabase`, entidades, DAOs e `ComplementaryCurrencyCatalog`), `data.remote` (E03: `AwesomeApiConfig`, `AwesomeApiService`, `AwesomeApiRemoteDataSource`, `AwesomeApiAuthInterceptor`, DTOs Moshi, `QuoteRemoteMapper`, `AvailableCatalogParser` e `RemoteResult`), `data.mapper` (`QuoteEntityMapper`, `RemoteErrorMapper`), `data.time` (`SystemTimeProvider`), `data.repository` (E04: `QuoteRepositoryImpl`, `CatalogRepositoryImpl`) e `data.worker` (E06: `SyncQuotesWorker`, `WorkManagerSyncScheduler`).
- `di`: contém `AppModule.kt`, `DatabaseModule.kt` (E02), `NetworkModule.kt` (E03), `DataModule.kt` (E04 + E06), `DomainModule.kt` (E04 + E06) e `PresentationModule.kt` (E05 + E06).

### Stack Tecnológica Configurada no Gradle
- **Linguagem / Build:** Kotlin `2.2.10`, Android Gradle Plugin `9.1.1`, KSP `2.3.5`, `compileSdk 36`, `minSdk 24`, `targetSdk 36`.
- **Ativamente utilizados até a E06:**
  - **Jetpack Compose + Material 3** (BOM `2024.09.00`) + **Material Icons Extended** + **Navigation Compose** (`2.8.9`)
  - **Koin** (BOM `4.1.1`: `koin-core`, `koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`)
  - **WorkManager** (`2.10.1`: `work-runtime-ktx`)
  - **Room** (`2.7.0`: `room-runtime`, `room-ktx`, `room-compiler` via KSP)
  - **Retrofit `2.12.0` + OkHttp `4.10.0` (`logging-interceptor`) + Moshi `1.15.2` (`moshi-kotlin`, `converter-moshi`, `moshi-kotlin-codegen` via KSP)**
  - **Kotlin Coroutines & Flow** (`1.10.2`)
  - **Cálculos monetários:** `java.math.BigDecimal` + `java.math.MathContext.DECIMAL128` (`HALF_UP`)
  - **Testes:** JUnit `4.13.2`, Kotlinx Coroutines Test `1.10.2`, OkHttp MockWebServer `4.10.0`, Robolectric `4.16.1`, Compose UI Test (`ui-test-junit4`)

---

## Estrutura Atual do Projeto

```text
/
├── SPECIFICATION.md                      # Requisitos funcionais, não funcionais, regras e rastreabilidade
├── PLANNING.md                           # Arquitetura, fluxo de dados, Room, API e sincronização
├── TASKS.md                              # Backlog incremental E00–E07
├── TEST_RESULTS.md                       # Registro histórico dos testes planejados e executados
├── README.md                             # Visão geral e estado real do projeto
└── app/src/
    ├── main/java/com/aistudio/dolarap/
    │   ├── DolarapApplication.kt         # Inicialização do Koin, Configuration.Provider (KoinWorkerFactory) e agendamento 6h (E06)
    │   ├── MainActivity.kt               # Entry point Compose com enableEdgeToEdge + DolarapApp
    │   ├── di/
    │   │   ├── AppModule.kt              # Lista de módulos Koin ativos (database, network, data, domain, presentation)
    │   │   ├── DatabaseModule.kt         # (E02) Provisão singleton de DolarapDatabase e DAOs
    │   │   ├── NetworkModule.kt          # (E03 + Auth) Moshi, OkHttpClient, AwesomeApiAuthInterceptor, Retrofit e RemoteDataSource
    │   │   ├── DataModule.kt             # (E04 + E06) TimeProvider, Mappers, Repositories SSOT, SyncScheduler e SyncQuotesWorker
    │   │   ├── DomainModule.kt           # (E04 + E06) CurrencyConverter, QuoteFreshnessEvaluator e 9 UseCases
    │   │   └── PresentationModule.kt     # (E05 + E06) ViewModels (Quotes, CurrencySearch, Converter)
    │   ├── domain/                       # (E04 + E06) Camada Kotlin/JDK pura
    │   │   ├── model/                    # CurrencyModels, AvailableCurrency, ConversionResult, DomainResult (inclui retryAfterSeconds)
    │   │   ├── repository/               # QuoteRepository, CatalogRepository
    │   │   ├── service/                  # CurrencyConverter, QuoteFreshnessEvaluator, TimeProvider, SyncScheduler (E06)
    │   │   └── usecase/                  # 9 UseCases (cotações, catálogo, busca, adição, remoção, favorito, conversão)
    │   ├── data/
    │   │   ├── local/                    # (E02) DolarapDatabase, entidades, DAOs e ComplementaryCurrencyCatalog
    │   │   ├── mapper/                   # (E04 + E06) QuoteEntityMapper, RemoteErrorMapper
    │   │   ├── remote/                   # (E03 + Auth) AwesomeApiConfig, Service, RemoteDataSource, AuthInterceptor, DTOs, Mapper e Parser
    │   │   ├── repository/               # (E04) QuoteRepositoryImpl, CatalogRepositoryImpl (SSOT Room)
    │   │   ├── time/                     # (E04) SystemTimeProvider
    │   │   └── worker/                   # (E06) SyncQuotesWorker, WorkManagerSyncScheduler
    │   └── presentation/                 # (E05 + E06) Interface Jetpack Compose + MVVM
    │       ├── theme/                    # Color.kt, Theme.kt, Type.kt (fontes locais Plus Jakarta Sans & JetBrains Mono)
    │       ├── format/                   # BrazilianFormatter.kt, DomainErrorMessageMapper.kt
    │       ├── components/               # DolarapComponents.kt (badges, pills, banners, Loading/Empty/Error)
    │       ├── navigation/               # DolarapNavGraph.kt (DolarapApp, DolarapNavHost, BottomBar/Rail)
    │       ├── quotes/                   # QuotesScreen.kt, QuotesUiState.kt (QuotesSyncPhase), QuotesViewModel.kt
    │       ├── search/                   # CurrencySearchScreen.kt, CurrencySearchUiState.kt, CurrencySearchViewModel.kt
    │       └── converter/                # ConverterScreen.kt, ConverterUiState.kt, ConverterViewModel.kt
    ├── test/java/com/aistudio/dolarap/
    │   ├── ExampleUnitTest.kt
    │   ├── ExampleRobolectricTest.kt
    │   ├── data/
    │   │   ├── local/                    # ComplementaryCurrencyCatalogTest, DolarapDatabaseIntegrationTest, KoinDatabaseModuleTest
    │   │   ├── remote/                   # QuoteRemoteMapperTest, AvailableCatalogParserTest, AwesomeApiAuthInterceptorTest, AwesomeApiRemoteDataSourceTest, KoinNetworkModuleTest
    │   │   ├── repository/               # QuoteAndCatalogRepositoryIntegrationTest
    │   │   └── worker/                   # (E06) SyncQuotesWorkerTest, WorkManagerSyncSchedulerTest, OfflineFirstAndSyncFlowIntegrationTest
    │   ├── di/                           # KoinDomainModuleTest, KoinPresentationModuleTest (inclui SyncScheduler + KoinWorkerFactory)
    │   ├── domain/
    │   │   ├── service/                  # CurrencyConverterTest, QuoteFreshnessEvaluatorTest
    │   │   └── usecase/                  # DomainUseCasesTest
    │   └── presentation/
    │       ├── TestFixtures.kt           # MainDispatcherRule, FakeQuoteRepository, FakeCatalogRepository
    │       ├── DolarapUiAndNavigationTest.kt
    │       ├── format/                   # BrazilianFormatterTest
    │       ├── quotes/                   # QuotesViewModelTest
    │       ├── search/                   # CurrencySearchViewModelTest
    │       └── converter/                # ConverterViewModelTest
    └── androidTest/java/com/aistudio/dolarap/
        └── ExampleInstrumentedTest.kt
```

---

## Integração com a AwesomeAPI (Implementada na E03)

- **Provedor:** AwesomeAPI — API de Cotações de Moedas (`https://docs.awesomeapi.com.br/api-de-moedas`)
- **Cotações em lote:** `GET https://economia.awesomeapi.com.br/json/last/{pairs}` (ex.: `USD-BRL,EUR-BRL`), aceitando somente pares no padrão `^[A-Z]{3}-BRL$`.
- **Catálogo de pares suportados:** `/xml/available` é a **fonte oficialmente documentada**; `/json/available` é executado apenas como **tentativa experimental**. Caso a tentativa experimental retorne erro de endpoint ou payload inválido/incompatível, ocorre fallback automático para `GET https://economia.awesomeapi.com.br/xml/available`. Em HTTP `429`, HTTP `5xx`, timeout ou ausência de conexão, não há segunda requisição e o cache do Room permanece preservado.
- **Proteção contra bloqueio por HTTP 404 (`CoinNotExists`):** Em respostas 404 (inclusive em lote), somente o par comprovadamente identificado pela resposta é marcado como indisponível no Room (`AvailablePairDao`) e bloqueado na sessão; os demais pares válidos são preservados integralmente e não há marcação em massa sem evidência.

---

## Configuração, Compilação, APK e Execução

1. **Requisitos do ambiente:** Android SDK 36 (`minSdk 24`, `targetSdk 36`), JDK 11+ (ambiente validado com JDK 21) e Gradle (`9.3.1`).
2. **Configuração da API Key (`AWESOME_API_KEY`):**
   - No **Google AI Studio**, a variável `AWESOME_API_KEY` é gerenciada pelo **Secrets panel** e injetada temporariamente em `.env` durante os builds da plataforma (removida automaticamente após o build para não persistir credenciais no workspace).
   - Localmente no **Android Studio**, crie o arquivo `.env` na raiz do projeto (copiando `.env.example` e preenchendo `AWESOME_API_KEY=<sua_chave>`) antes de gerar o APK. O arquivo `.env` está protegido no `.gitignore` e **nunca** deve ser versionado.
   - O `Secrets Gradle Plugin` injeta o valor em `BuildConfig.AWESOME_API_KEY`, consumido pelo `AwesomeApiAuthInterceptor` para enviar o cabeçalho `x-api-key` nas chamadas da AwesomeAPI sem exigir que o usuário final insira qualquer chave no celular.
3. **Executar os testes unitários, de integração local e de UI Compose (92 testes / 24 suítes):**
   ```sh
   gradle :app:testDebugUnitTest
   ```
4. **Compilar o APK Debug final:**
   ```sh
   gradle :app:assembleDebug
   ```
   *(No Google AI Studio, utilize o build da plataforma / menu **Settings → Generate APK/AAB** ou `compile_applet` para garantir a injeção automática de `.env` a partir do Secrets panel. Um `gradle :app:assembleDebug` avulso no shell após a limpeza automática de `.env` utilizaria o placeholder de `.env.example`.)*
5. **Localização e obtenção do APK gerado:**
   - **Caminho no projeto:** `app/build/outputs/apk/debug/app-debug.apk` (e espelho da plataforma em `.build-outputs/app-debug.apk`, `22.590.182` bytes / `21,54 MiB`).
   - **Como obter no Google AI Studio:** Pelo menu de configurações (**Settings → Generate APK/AAB**) para download direto do APK assinado em debug, ou via **Export project as ZIP** (que inclui o projeto pronto e/ou abertura direta no Android Studio).
   - **Aviso de segurança:** Como o APK de entrega acadêmica/protótipo embute a `AWESOME_API_KEY` em `BuildConfig`, ele pode ser descompilado; **não compartilhe o arquivo `.apk` publicamente** nem o suba ao GitHub (protegido por `*.apk` e `/.build-outputs/` no `.gitignore`).

---

## Testes Implementados e Executados

Os resultados detalhados estão registrados em [`TEST_RESULTS.md`](TEST_RESULTS.md):
- **Etapa E01:** `ExampleUnitTest` (1 teste) e `ExampleRobolectricTest` (1 teste validando `app_name = "DOLARAP"`).
- **Etapa E02 (12 testes):**
  - `ComplementaryCurrencyCatalogTest` (4 testes unitários JUnit)
  - `DolarapDatabaseIntegrationTest` (7 testes de integração Room via Robolectric)
  - `KoinDatabaseModuleTest` (1 teste de integração DI + Room via Robolectric)
- **Etapa E03 + API Key (21 testes):**
  - `QuoteRemoteMapperTest` (3 testes unitários JUnit)
  - `AvailableCatalogParserTest` (4 testes unitários JUnit)
  - `AwesomeApiAuthInterceptorTest` (3 testes unitários/MockWebServer garantindo envio e mascaramento de `x-api-key`)
  - `AwesomeApiRemoteDataSourceTest` (11 testes de integração HTTP + Room via `MockWebServer` + Robolectric, incluindo `Retry-After`)
  - `KoinNetworkModuleTest` (1 teste de integração DI via Robolectric — contabilizado junto à suíte de rede)
  *(Nota: 18 testes da E03 original + 3 testes de `AwesomeApiAuthInterceptorTest` + 1 teste de `Retry-After` adicionado na E06 = 22 testes na suíte `data.remote`.)*
- **Etapa E04 (21 testes):**
  - `CurrencyConverterTest` (7 testes unitários JUnit)
  - `QuoteFreshnessEvaluatorTest` (4 testes unitários JUnit)
  - `DomainUseCasesTest` (5 testes da E04 + 1 teste E06 de integração `RefreshQuotesUseCase` + `SyncScheduler`)
  - `QuoteAndCatalogRepositoryIntegrationTest` (4 testes de integração Room + `MockWebServer` + Robolectric)
  - `KoinDomainModuleTest` (1 teste de integração DI via Robolectric)
- **Etapa E05 (22 testes):**
  - `BrazilianFormatterTest` (3 testes unitários JUnit)
  - `QuotesViewModelTest` (6 testes E05 + 1 teste E06 cobrindo os 7 estados unificados de sync)
  - `CurrencySearchViewModelTest` (3 testes unitários)
  - `ConverterViewModelTest` (3 testes unitários)
  - `KoinPresentationModuleTest` (1 teste E05 + 1 teste E06 validando `SyncScheduler` e `KoinWorkerFactory`)
  - `DolarapUiAndNavigationTest` (6 testes E05 + 1 teste E06 validando banners não destrutivos offline/falha de sync)
- **Etapa E06 (14 novos testes cobrindo Offline First e WorkManager):**
  - `SyncQuotesWorkerTest` (5 testes via Robolectric): sucesso e cancelamento de retry periódico, falhas temporárias com limite `MAX_RETRY_ATTEMPTS = 3`, falhas permanentes sem loop, HTTP 429 com `Retry-After` vs sem header e exceções SQLite transitórias/fatais.
  - `WorkManagerSyncSchedulerTest` (3 testes via Robolectric + WorkManager real): trabalho periódico único de 6h (`KEEP`, `NetworkType.CONNECTED`), retry único `OneTimeWork` (`KEEP`, `REPLACE` com `Retry-After`, `NetworkType.CONNECTED`) e mapeamento completo dos estados `IDLE`/`ENQUEUED`/`RUNNING`/`BLOCKED`/`SUCCEEDED`/`FAILED`.
  - `OfflineFirstAndSyncFlowIntegrationTest` (1 teste ponta a ponta Room + `MockWebServer` + `SyncQuotesWorker` + `QuotesViewModel`): abertura offline imediata via Room, atualização reativa automática do `Flow` na UI quando o worker persiste novas cotações, preservação do cache em falha e aviso após > 15 minutos.
  - Testes adicionais em `AwesomeApiRemoteDataSourceTest` (1 teste `Retry-After`), `DomainUseCasesTest` (1 teste `RefreshQuotesUseCase`), `KoinPresentationModuleTest` (1 teste `KoinWorkerFactory`), `QuotesViewModelTest` (1 teste dos 7 estados de sync) e `DolarapUiAndNavigationTest` (1 teste dos banners offline/erro sem ocultar cards do Room).
- **Total revalidado na Etapa E07 (`2026-11-17`):** **92 testes executados** em 24 suítes via `gradle :app:testDebugUnitTest` (92 aprovados, 0 falhas, 0 erros, 0 ignorados).
- **Testes instrumentados (`androidTest`) e manuais (`MT-01`–`MT-11`):** **Não executados / dispensados pelo responsável pelo projeto** (e ambiente em container sem emulador/ADB acessível ao agente).

---

## Limitações Conhecidas

- **WorkManager (`E06`):** Tarefas periódicas de 6 horas e retries condicionados à rede respeitam as políticas de economia de energia do Android (Doze Mode / App Standby Buckets) e não garantem execução no segundo exato.
- **AwesomeAPI:** Recomenda-se respeitar cabeçalhos `Retry-After` em HTTP 429 e manter a validação local de pares `^[A-Z]{3}-BRL$` para evitar erros HTTP 404 recorrentes (`CoinNotExists`).
- **Fora de escopo:** Sem login, integração bancária, pagamentos, compra/venda real, carteira, gráficos avançados, alertas/notificações ou backend próprio.

---

## Evidências e Entrega

- **Status atual:** **Etapa E07 concluída — aguardando revisão final externa.**
- **APK final verificado:** `app/build/outputs/apk/debug/app-debug.apk` (`22.590.182` bytes / `21,54 MiB`), com `BuildConfig.AWESOME_API_KEY` real verificada no DEX e `AwesomeApiAuthInterceptor` integrado ao `OkHttpClient`.
- **Versionamento GitHub:** Configurado via `.gitignore` (protegendo `.env`, `debug.keystore`, `/.build-outputs/`, `*.apk`, `*.aab`, `/build`, `.gradle` e `local.properties`); sem commit/push automático no ambiente para realização pelo responsável no Android Studio.
