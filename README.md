# DOLARAP

> **Estado atual do projeto:** **Etapa E04 concluída (camada `domain` pura, conversor `BigDecimal`, regra de 15 minutos, repositórios SSOT Room, Koin e 21 novos testes) e aguardando revisão externa.**
> A interface funcional em Jetpack Compose (`E05`) e a sincronização em segundo plano com WorkManager (`E06`) permanecem **planejadas**.

Aplicativo Android nativo para consulta e conversão de cotações de moedas estrangeiras em relação ao Real brasileiro (**BRL**), projetado com foco em **Offline First**, precisão monetária (`BigDecimal`) e arquitetura limpa em módulo único (`:app`).

---

## Progresso do Desenvolvimento (E00–E07)

| Etapa | Descrição | Status |
|---|---|---|
| **E00** | Especificação e planejamento (`SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `README.md`, `TEST_RESULTS.md`) | **Aprovada** |
| **E01** | Preparação do projeto Android (`:app`, `com.aistudio.dolarap`, dependências base, ícone, Koin bootstrap) | **Aprovada** |
| **E02** | Banco de dados local Room (entidades, relação, DAOs com `Flow`, seed das 10 moedas padrão, Koin e testes) | **Aprovada** |
| **E03** | Integração com a AwesomeAPI (Retrofit/OkHttp/Moshi, `/json/last/{pairs}`, `/json/available` + fallback `/xml/available`, tratamento de erros e `CoinNotExists`) | **Aprovada** |
| **E04** | Camada de domínio e repositórios (modelos puros, `BigDecimal`, UseCases, regra de 15 min, SSOT Room, `dataModule`/`domainModule`) | **Implementação e testes concluídos — aguardando revisão externa** |
| **E05** | Interface Jetpack Compose (Cotações, Pesquisa/Adição, Conversor, Filtro de Favoritos, Navigation Compose) | Pendente |
| **E06** | Offline First, sincronização com WorkManager (6h e retry `CONNECTED`) e validação completa | Pendente |
| **E07** | Entrega final, documentação consolidada, APK e versionamento | Pendente |

---

## Funcionalidades Implementadas vs. Planejadas

### Já implementado (até a Etapa E04)
- **Estrutura base Android (`E01`):** Módulo único `:app` com `namespace` e `applicationId` configurados para `com.aistudio.dolarap`, permissões `INTERNET` e `ACCESS_NETWORK_STATE`, ícone adaptativo customizado, tema Material 3 (`DolarapTheme`) e tela provisória de inicialização (`BootstrapPlaceholder` na `MainActivity`).
- **Inicialização de DI com Koin (`E01`–`E04`):** `DolarapApplication` inicializando Koin de forma segura (`appModules`), reunindo `databaseModule` (Room + DAOs), `networkModule` (Retrofit/OkHttp/Moshi/AwesomeAPI), `dataModule` (mappers, `TimeProvider` e repositórios SSOT) e `domainModule` (`CurrencyConverter`, `QuoteFreshnessEvaluator` e os 9 UseCases).
- **Persistência local com Room (`E02`):**
  - **4 entidades Room:** `SelectedCurrencyEntity` (`selected_currencies`), `QuoteEntity` (`quotes`), `AvailablePairEntity` (`available_pairs`) e `SyncMetadataEntity` (`sync_metadata`).
  - **Relação Room:** `SelectedCurrencyWithQuote` (`@Embedded` + `@Relation`) unindo cada moeda selecionada à sua cotação opcional (`0..1`).
  - **4 DAOs com consultas reativas via `Flow`:** `CurrencyDao`, `QuoteDao`, `AvailablePairDao` e `SyncMetadataDao`.
  - **Seed inicial e catálogo complementar (`E02`):** `DolarapDatabase` semeia de forma atômica e idempotente as **10 moedas padrão** (`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY`, `MXN`); `ComplementaryCurrencyCatalog` enriquece nomes amigáveis em PT-BR e normalização de busca.
- **Integração remota com a AwesomeAPI (`E03`):**
  - **Configuração HTTP centralizada (`AwesomeApiConfig`, `NetworkModule`):** Retrofit + OkHttpClient (timeouts de 15s e `HttpLoggingInterceptor` nível `BASIC` em debug) + Moshi (`KotlinJsonAdapterFactory` e codegen KSP).
  - **Consulta de cotações (`AwesomeApiService.getLastQuotes`, `QuoteRemoteMapper`):** Consome `/json/last/{pairs}` para múltiplos pares válidos no padrão `^[A-Z]{3}-BRL$`, preservando decimais exatos como string em `QuoteEntity`.
  - **Catálogo oficial com tentativa experimental JSON (`AvailableCatalogParser`, `AwesomeApiRemoteDataSource`):** `/xml/available` é a fonte oficialmente documentada; `/json/available` atua apenas como tentativa experimental com fallback XML (bloqueando segunda chamada em HTTP 429, 5xx, timeout ou offline).
  - **Tratamento defensivo de erros (`RemoteResult`, `RemoteError`, `ApiErrorDto`):** Trata desconexão, timeout, HTTP 404 (`CoinNotExists` em consulta simples ou em lote), 429, 5xx e payloads malformados sem derrubar o app nem apagar dados locais.
- **Camada de Domínio pura e Repositórios SSOT (`E04`):**
  - **Camada `domain` pura (sem imports Android/Room/Retrofit/Moshi/Compose):** Modelos (`CurrencyQuote`, `QuoteRate`, `DashboardQuotesSnapshot`, `SyncStatus`, `QuoteFreshnessStatus`, `AvailableCurrency`, `ConversionResult`, `CurrencyDefaults`, `DomainResult`/`DomainError`), contratos (`QuoteRepository`, `CatalogRepository`, `TimeProvider`), serviços (`CurrencyConverter` em `BigDecimal`, `QuoteFreshnessEvaluator` com regra de 15 minutos) e 9 UseCases (`ObserveDashboardQuotesUseCase`, `RefreshQuotesUseCase`, `ObserveAvailableCurrenciesUseCase`, `SearchAvailableCurrenciesUseCase`, `RefreshCatalogUseCase`, `AddCurrencyUseCase`, `RemoveCurrencyUseCase`, `ToggleFavoriteUseCase`, `ConvertCurrencyUseCase`).
  - **Conversor monetário em `BigDecimal`:** Suporta `X -> BRL`, `BRL -> Y`, `X -> Y` (triangulada via BRL) e `X -> X` utilizando exclusivamente `bid` como taxa padrão (`ask` mantido como dado complementar), intermediários em `MathContext.DECIMAL128`, escala da moeda de destino via ISO 4217 (ex.: `JPY` 0 casas, `BRL`/`USD`/`EUR` 2 casas, `BTC`/`ETH`/`LTC` 8 casas) e `RoundingMode.HALF_UP`.
  - **Regra de 15 minutos (`QuoteFreshnessEvaluator`):** Trata ausência de timestamp como `UNAVAILABLE`, até `900_000 ms` como `FRESH`, acima de 15 minutos como `STALE` e timestamps inválidos/futuros defensivamente como `STALE`.
  - **Repositórios SSOT (`QuoteRepositoryImpl`, `CatalogRepositoryImpl`):** Conectam a AwesomeAPI ao Room usando o Room como Fonte Única da Verdade via `Flow`, deduplicam sincronizações concorrentes (`Mutex` + `CompletableDeferred`), particionam lotes de cotações (`MAX_BATCH_SIZE = 25`), protegem as 10 moedas padrão e preservam o cache local intacto em falhas de rede/API.
- **Testes automatizados (`E01`–`E04`):** 53 testes unitários e de integração local via JUnit 4 + `MockWebServer` + Robolectric aprovados em `gradle :app:testDebugUnitTest`.

### Planejado para etapas futuras (ainda NÃO implementado)
- **Interface Funcional (`E05`):** Telas definitivas de Cotações (com filtro de favoritos), Pesquisa/Adição de Moedas e Conversor, com Navigation Compose, `ViewModel`s e estados `Loading`, `Success`, `Empty` e `Error`.
- **Sincronização em Segundo Plano (`E06`):** `SyncQuotesWorker` e `WorkManagerSyncScheduler` (`SyncScheduler` com trabalho periódico de 6 horas e retry condicionado a `NetworkType.CONNECTED` sem duplicação).

---

## Arquitetura e Tecnologias

### Arquitetura Atual e Planejada
O projeto adota **Clean Architecture + MVVM** em módulo único `:app` (`com.aistudio.dolarap`), dividido em quatro pacotes principais:
- `presentation`: atualmente contém o tema Material 3 (`DolarapTheme`) e será expandido na E05 com telas Jetpack Compose, navegação e `ViewModel`s (`StateFlow`).
- `domain`: implementado na E04 como camada Kotlin/JDK pura (modelos, erros tipados, contratos `QuoteRepository`/`CatalogRepository`, `CurrencyConverter`, `QuoteFreshnessEvaluator`, `TimeProvider` e 9 UseCases — zero dependências de Android/Room/Retrofit/Moshi).
- `data`: contém `data.local` (E02: `DolarapDatabase`, entidades, DAOs e `ComplementaryCurrencyCatalog`), `data.remote` (E03: `AwesomeApiConfig`, `AwesomeApiService`, `AwesomeApiRemoteDataSource`, DTOs Moshi, `QuoteRemoteMapper`, `AvailableCatalogParser` e `RemoteResult`), `data.mapper` (`QuoteEntityMapper`, `RemoteErrorMapper`), `data.time` (`SystemTimeProvider`) e `data.repository` (E04: `QuoteRepositoryImpl`, `CatalogRepositoryImpl`); receberá `data.worker` na E06.
- `di`: contém `AppModule.kt`, `DatabaseModule.kt` (E02), `NetworkModule.kt` (E03), `DataModule.kt` (E04) e `DomainModule.kt` (E04) ativos; receberá `PresentationModule.kt` na E05 e `SyncScheduler`/Worker na E06.

### Stack Tecnológica Configurada no Gradle
- **Linguagem / Build:** Kotlin `2.2.10`, Android Gradle Plugin `9.1.1`, KSP `2.3.5`, `compileSdk 36`, `minSdk 24`, `targetSdk 36`.
- **Ativamente utilizados até a E04:**
  - **Jetpack Compose + Material 3** (BOM `2024.09.00`)
  - **Koin** (BOM `4.1.1`: `koin-core`, `koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`)
  - **Room** (`2.7.0`: `room-runtime`, `room-ktx`, `room-compiler` via KSP)
  - **Retrofit `2.12.0` + OkHttp `4.10.0` (`logging-interceptor`) + Moshi `1.15.2` (`moshi-kotlin`, `converter-moshi`, `moshi-kotlin-codegen` via KSP)**
  - **Kotlin Coroutines & Flow** (`1.10.2`)
  - **Cálculos monetários:** `java.math.BigDecimal` + `java.math.MathContext.DECIMAL128` (`HALF_UP`)
  - **Testes:** JUnit `4.13.2`, Kotlinx Coroutines Test `1.10.2`, OkHttp MockWebServer `4.10.0`, Robolectric `4.16.1`
- **Declarados no Gradle para as próximas etapas (ainda sem implementação funcional):**
  - **Navigation Compose** (`2.8.9`) — E05
  - **WorkManager** (`2.10.1`: `work-runtime-ktx`) — E06

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
    │   ├── DolarapApplication.kt         # Inicialização do Koin
    │   ├── MainActivity.kt               # Tela provisória da E01 (placeholder até E05)
    │   ├── di/
    │   │   ├── AppModule.kt              # Lista de módulos Koin ativos (database, network, data, domain)
    │   │   ├── DatabaseModule.kt         # (E02) Provisão singleton de DolarapDatabase e DAOs
    │   │   ├── NetworkModule.kt          # (E03) Moshi, OkHttpClient, Retrofit, Service e RemoteDataSource
    │   │   ├── DataModule.kt             # (E04) TimeProvider, Mappers e Repositories SSOT
    │   │   └── DomainModule.kt           # (E04) CurrencyConverter, QuoteFreshnessEvaluator e 9 UseCases
    │   ├── domain/                       # (E04) Camada Kotlin/JDK pura
    │   │   ├── model/                    # CurrencyModels, AvailableCurrency, ConversionResult, DomainResult
    │   │   ├── repository/               # QuoteRepository, CatalogRepository
    │   │   ├── service/                  # CurrencyConverter, QuoteFreshnessEvaluator, TimeProvider
    │   │   └── usecase/                  # 9 UseCases (cotações, catálogo, busca, adição, remoção, favorito, conversão)
    │   ├── data/
    │   │   ├── local/                    # (E02) DolarapDatabase, entidades, DAOs e ComplementaryCurrencyCatalog
    │   │   ├── mapper/                   # (E04) QuoteEntityMapper, RemoteErrorMapper
    │   │   ├── remote/                   # (E03) AwesomeApiConfig, Service, RemoteDataSource, DTOs, Mapper e Parser
    │   │   ├── repository/               # (E04) QuoteRepositoryImpl, CatalogRepositoryImpl (SSOT Room)
    │   │   └── time/                     # (E04) SystemTimeProvider
    │   └── presentation/theme/           # Color.kt, Theme.kt, Type.kt (telas Compose na E05)
    ├── test/java/com/aistudio/dolarap/
    │   ├── ExampleUnitTest.kt
    │   ├── ExampleRobolectricTest.kt
    │   ├── data/
    │   │   ├── local/                    # ComplementaryCurrencyCatalogTest, DolarapDatabaseIntegrationTest, KoinDatabaseModuleTest
    │   │   ├── remote/                   # QuoteRemoteMapperTest, AvailableCatalogParserTest, AwesomeApiRemoteDataSourceTest, KoinNetworkModuleTest
    │   │   └── repository/               # QuoteAndCatalogRepositoryIntegrationTest
    │   ├── di/                           # KoinDomainModuleTest
    │   └── domain/
    │       ├── service/                  # CurrencyConverterTest, QuoteFreshnessEvaluatorTest
    │       └── usecase/                  # DomainUseCasesTest
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

## Configuração, Compilação e Execução (Estado Atual)

1. **Requisitos do ambiente:** Android SDK 36, JDK 11+ e Gradle configurado no ambiente (ou Android Studio recente).
2. **Compilar o APK Debug:**
   ```sh
   gradle :app:assembleDebug
   ```
   *(Ao executar o APK atual no dispositivo/emulador, será exibida apenas a tela provisória de inicialização criada na E01; as telas definitivas de cotações, pesquisa e conversor serão implementadas na Etapa E05.)*
3. **Executar os testes unitários e de integração local (Domínio/Room/AwesomeAPI/Repositórios/Koin via JUnit + Robolectric + MockWebServer):**
   ```sh
   gradle :app:testDebugUnitTest
   ```
4. **Credenciais:** Não requer chaves pagas, login ou arquivo `google-services.json` para compilação e execução dos testes atuais.

---

## Testes Implementados e Executados

Os resultados detalhados estão registrados em [`TEST_RESULTS.md`](TEST_RESULTS.md):
- **Etapa E01:** `ExampleUnitTest` (1 teste) e `ExampleRobolectricTest` (1 teste validando `app_name = "DOLARAP"`).
- **Etapa E02 (12 testes):**
  - `ComplementaryCurrencyCatalogTest` (4 testes unitários JUnit)
  - `DolarapDatabaseIntegrationTest` (7 testes de integração Room via Robolectric)
  - `KoinDatabaseModuleTest` (1 teste de integração DI + Room via Robolectric)
- **Etapa E03 (18 testes):**
  - `QuoteRemoteMapperTest` (3 testes unitários JUnit)
  - `AvailableCatalogParserTest` (4 testes unitários JUnit)
  - `AwesomeApiRemoteDataSourceTest` (10 testes de integração HTTP + Room via `MockWebServer` + Robolectric)
  - `KoinNetworkModuleTest` (1 teste de integração DI via Robolectric)
- **Etapa E04 (21 testes):**
  - `CurrencyConverterTest` (7 testes unitários JUnit): conversões `X -> BRL`, `BRL -> Y`, `X -> Y` (via BRL) com `bid` em `BigDecimal`, mesma moeda, zero, vírgula decimal, escalas por moeda de destino (`JPY` = 0, `BTC` = 8), rejeição de entrada inválida/negativa e rejeição de cotação ausente/`bid <= 0`.
  - `QuoteFreshnessEvaluatorTest` (4 testes unitários JUnit): estados `UNAVAILABLE`, `FRESH` (até `900_000 ms`), `STALE` (> 15 min) e proteção defensiva para relógio inválido/futuro.
  - `DomainUseCasesTest` (5 testes unitários JUnit com fakes): proteção das 10 moedas padrão, busca sem acentos/maiúsculas, adição validada com preservação se cotação inicial falhar, filtro de favoritos e conversão com sinalização de `isStale`.
  - `QuoteAndCatalogRepositoryIntegrationTest` (4 testes de integração Room + `MockWebServer` + Robolectric): observação inicial das 10 moedas padrão via `Flow`, persistência API -> Room com preservação de cache em falha, coalescência de `refreshQuotes()` concorrentes (`requestCount == 1`), sync do catálogo, pesquisa, adição validada e persistência de favoritos.
  - `KoinDomainModuleTest` (1 teste de integração DI via Robolectric): resolução completa do grafo Koin (`databaseModule`, `networkModule`, `dataModule`, `domainModule`) sem dependências circulares.
- **Total executado na última bateria (`2026-11-15`):** **53 testes executados** em `:app:testDebugUnitTest` (53 aprovados, 0 falhas, 0 ignorados).
- **Testes instrumentados (`androidTest`):** Não executados até o momento por ausência de emulador Android/ADB no ambiente de build.

---

## Limitações Conhecidas

- **Escopo atual (E04):** Ainda não existem telas funcionais em Jetpack Compose nem `ViewModel`s (`E05`), tampouco sincronização em background via WorkManager (`E06`).
- **WorkManager (planejado para E06):** Tarefas periódicas de 6 horas dependem das políticas de energia do Android (Doze/App Standby) e não garantem horário exato.
- **AwesomeAPI:** Cache de 1 minuto para requisições não autenticadas e risco de bloqueio temporário de IP por erros HTTP 404 recorrentes (`CoinNotExists`), mitigado via validação local e bloqueio pontual de pares inválidos.
- **Fora de escopo:** Sem login, integração bancária, pagamentos, compra/venda real, carteira, gráficos avançados, alertas/notificações ou backend próprio.

---

## Evidências e Entrega

- **Status atual:** Aguardando revisão externa da **Etapa E04** para aprovação e liberação da **Etapa E05**.
- **Evidências finais (APK definitivo, capturas de tela e consolidação final):** Serão adicionadas na **Etapa E07**.
