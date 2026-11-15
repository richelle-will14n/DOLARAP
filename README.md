# DOLARAP

> **Estado atual do projeto:** **Etapa E03 concluída (integração com a AwesomeAPI + testes automatizados com MockWebServer) e aguardando revisão externa.**
> A camada de domínio (`BigDecimal`/UseCases/Repositories), a interface funcional em Jetpack Compose e a sincronização com WorkManager permanecem **planejadas** para as etapas E04–E07.

Aplicativo Android nativo para consulta e conversão de cotações de moedas estrangeiras em relação ao Real brasileiro (**BRL**), projetado com foco em **Offline First**, precisão monetária e arquitetura limpa em módulo único (`:app`).

---

## Progresso do Desenvolvimento (E00–E07)

| Etapa | Descrição | Status |
|---|---|---|
| **E00** | Especificação e planejamento (`SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `README.md`, `TEST_RESULTS.md`) | **Aprovada** |
| **E01** | Preparação do projeto Android (`:app`, `com.aistudio.dolarap`, dependências base, ícone, Koin bootstrap) | **Aprovada** |
| **E02** | Banco de dados local Room (entidades, relação, DAOs com `Flow`, seed das 10 moedas padrão, Koin e testes) | **Aprovada** |
| **E03** | Integração com a AwesomeAPI (Retrofit/OkHttp/Moshi, `/json/last/{pairs}`, `/json/available` + fallback `/xml/available`, tratamento de erros e `CoinNotExists`) | **Implementação e testes concluídos — aguardando revisão externa** |
| **E04** | Camada de domínio e repositórios (modelos puros, `BigDecimal`, UseCases, regra de 15 min, SSOT) | Pendente |
| **E05** | Interface Jetpack Compose (Cotações, Pesquisa/Adição, Conversor, Filtro de Favoritos, Navigation Compose) | Pendente |
| **E06** | Offline First, sincronização com WorkManager (6h e retry `CONNECTED`) e validação completa | Pendente |
| **E07** | Entrega final, documentação consolidada, APK e versionamento | Pendente |

---

## Funcionalidades Implementadas vs. Planejadas

### Já implementado (até a Etapa E03)
- **Estrutura base Android (`E01`):** Módulo único `:app` com `namespace` e `applicationId` configurados para `com.aistudio.dolarap`, permissões `INTERNET` e `ACCESS_NETWORK_STATE`, ícone adaptativo customizado, tema Material 3 (`DolarapTheme`) e tela provisória de inicialização (`BootstrapPlaceholder` na `MainActivity`).
- **Inicialização de DI com Koin (`E01`–`E03`):** `DolarapApplication` inicializando Koin de forma segura (`appModules`), reunindo `databaseModule` (Room + DAOs) e `networkModule` (`Moshi`, `OkHttpClient`, `Retrofit`, `AwesomeApiService`, `QuoteRemoteMapper`, `AvailableCatalogParser` e `AwesomeApiRemoteDataSource`).
- **Persistência local com Room (`E02`):**
  - **4 entidades Room:** `SelectedCurrencyEntity` (`selected_currencies`), `QuoteEntity` (`quotes`), `AvailablePairEntity` (`available_pairs`) e `SyncMetadataEntity` (`sync_metadata`).
  - **Relação Room:** `SelectedCurrencyWithQuote` (`@Embedded` + `@Relation`) unindo cada moeda selecionada à sua cotação opcional (`0..1`).
  - **4 DAOs com consultas reativas via `Flow`:** `CurrencyDao`, `QuoteDao`, `AvailablePairDao` e `SyncMetadataDao`.
  - **Seed inicial e catálogo complementar (`E02`):** `DolarapDatabase` semeia de forma atômica e idempotente as **10 moedas padrão** (`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY`, `MXN`); `ComplementaryCurrencyCatalog` enriquece nomes amigáveis em PT-BR e normalização de busca.
- **Integração remota com a AwesomeAPI (`E03`):**
  - **Configuração HTTP centralizada (`AwesomeApiConfig`, `NetworkModule`):** Retrofit + OkHttpClient (timeouts de 15s e `HttpLoggingInterceptor` nível `BASIC` em debug) + Moshi (`KotlinJsonAdapterFactory` e codegen KSP).
  - **Consulta de cotações (`AwesomeApiService.getLastQuotes`, `QuoteRemoteMapper`):** Consome `/json/last/{pairs}` para múltiplos pares válidos no padrão `^[A-Z]{3}-BRL$`, validando `bid`/`ask` positivos via `toBigDecimalOrNull()` e preservando `bid`, `ask`, `varBid`, `pctChange`, `high` e `low` como strings exatas em `QuoteEntity` (sem conversão para `Double`/`Float`).
  - **Catálogo oficial com tentativa experimental JSON (`AvailableCatalogParser`, `AwesomeApiRemoteDataSource`):** Trata `/xml/available` como fonte oficialmente documentada pela AwesomeAPI e `/json/available` apenas como tentativa experimental. Se o JSON falhar por erro de endpoint ou formato inválido/incompatível, aciona o fallback oficial `/xml/available`; **não** dispara segunda requisição em HTTP `429`, HTTP `5xx`, timeout ou ausência de conexão, preservando o cache existente no Room. Aceita estritamente `^[A-Z]{3}-BRL$` com descrição válida (rejeitando pares cruzados, turismo, PTAX e códigos fora de 3 letras).
  - **Tratamento defensivo de erros (`RemoteResult`, `RemoteError`, `ApiErrorDto`):** Trata desconexão, timeout, HTTP 404 (`CoinNotExists` em consulta simples ou em lote — identificando e bloqueando apenas o par comprovadamente inexistente quando a resposta permitir, preservando todos os pares válidos), HTTP 429, 5xx e payloads vazios/malformados sem derrubar o app nem apagar dados locais.
- **Testes automatizados (`E01`–`E03`):** 32 testes unitários e de integração local via JUnit 4 + `MockWebServer` + Robolectric aprovados em `gradle :app:testDebugUnitTest`.

### Planejado para etapas futuras (ainda NÃO implementado)
- **Camada `domain` e Repositórios (`E04`):** Modelos de domínio puros, calculadora monetária em `BigDecimal` utilizando `bid`, `QuoteRepositoryImpl` / `CatalogRepositoryImpl` (SSOT Room), casos de uso de cotações, pesquisa/adição/remoção, favoritos e regra de obsolescência de 15 minutos.
- **Interface Funcional (`E05`):** Telas definitivas de Cotações (com filtro de favoritos), Pesquisa/Adição de Moedas e Conversor, com Navigation Compose e estados `Loading`, `Success`, `Empty` e `Error`.
- **Sincronização em Segundo Plano (`E06`):** `SyncQuotesWorker` com WorkManager (trabalho periódico de 6 horas e re-sincronização condicionada a `NetworkType.CONNECTED` sem duplicação).

---

## Arquitetura e Tecnologias

### Arquitetura Atual e Planejada
O projeto adota **Clean Architecture + MVVM** em módulo único `:app` (`com.aistudio.dolarap`), dividido em quatro pacotes principais:
- `presentation`: atualmente contém o tema Material 3 (`DolarapTheme`) e será expandido na E05 com telas Jetpack Compose, navegação e `ViewModel`s (`StateFlow`).
- `domain`: reservado para modelos puros, contratos de repositório, calculadora em `BigDecimal` e casos de uso (sem dependências de Android/Room/Retrofit) — será implementado na E04.
- `data`: contém `data.local` (E02: `DolarapDatabase`, entidades, DAOs e `ComplementaryCurrencyCatalog`) e `data.remote` (E03: `AwesomeApiConfig`, `AwesomeApiService`, `AwesomeApiRemoteDataSource`, DTOs Moshi, `QuoteRemoteMapper`, `AvailableCatalogParser` e `RemoteResult`); receberá `data.repository` (E04) e `data.worker` (E06).
- `di`: contém `AppModule.kt`, `DatabaseModule.kt` (E02) e `NetworkModule.kt` (E03) ativos; receberá os módulos de repositórios/domínio e apresentação nas próximas etapas.

### Stack Tecnológica Configurada no Gradle
- **Linguagem / Build:** Kotlin `2.2.10`, Android Gradle Plugin `9.1.1`, KSP `2.3.5`, `compileSdk 36`, `minSdk 24`, `targetSdk 36`.
- **Ativamente utilizados até a E03:**
  - **Jetpack Compose + Material 3** (BOM `2024.09.00`)
  - **Koin** (BOM `4.1.1`: `koin-core`, `koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`)
  - **Room** (`2.7.0`: `room-runtime`, `room-ktx`, `room-compiler` via KSP)
  - **Retrofit `2.12.0` + OkHttp `4.10.0` (`logging-interceptor`) + Moshi `1.15.2` (`moshi-kotlin`, `converter-moshi`, `moshi-kotlin-codegen` via KSP)**
  - **Kotlin Coroutines & Flow** (`1.10.2`)
  - **Testes:** JUnit `4.13.2`, Kotlinx Coroutines Test `1.10.2`, OkHttp MockWebServer `4.10.0`, Robolectric `4.16.1`
- **Declarados no Gradle para as próximas etapas (ainda sem implementação funcional):**
  - **Navigation Compose** (`2.8.9`) — E05
  - **WorkManager** (`2.10.1`: `work-runtime-ktx`) — E06
  - **Cálculos monetários:** `java.math.BigDecimal` (JDK) — E04

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
    │   ├── MainActivity.kt               # Tela provisória da E01 (placeholder)
    │   ├── di/
    │   │   ├── AppModule.kt              # Lista de módulos Koin ativos (databaseModule, networkModule)
    │   │   ├── DatabaseModule.kt         # Provisão singleton de DolarapDatabase e DAOs
    │   │   └── NetworkModule.kt          # Provisão de Moshi, OkHttpClient, Retrofit, Service e DataSource
    │   ├── data/
    │   │   ├── local/
    │   │   │   ├── DolarapDatabase.kt    # Banco Room v1 + seed das 10 moedas padrão
    │   │   │   ├── catalog/ComplementaryCurrencyCatalog.kt
    │   │   │   ├── dao/                  # CurrencyDao, QuoteDao, AvailablePairDao, SyncMetadataDao
    │   │   │   └── entity/               # 4 entidades Room + SelectedCurrencyWithQuote
    │   │   └── remote/
    │   │       ├── AwesomeApiConfig.kt   # URL base, endpoints e constantes da AwesomeAPI
    │   │       ├── AwesomeApiService.kt  # Interface Retrofit (/json/last, /json/available, /xml/available)
    │   │       ├── AwesomeApiRemoteDataSource.kt # Orquestração HTTP, fallback XML e bloqueio de 404
    │   │       ├── dto/                  # QuoteDto, ApiErrorDto
    │   │       ├── mapper/               # QuoteRemoteMapper
    │   │       ├── model/                # RemoteResult, RemoteError
    │   │       └── parser/               # AvailableCatalogParser (JSON + XML)
    │   ├── domain/                       # Reservado para Etapa E04
    │   └── presentation/theme/           # Color.kt, Theme.kt, Type.kt
    ├── test/java/com/aistudio/dolarap/
    │   ├── ExampleUnitTest.kt
    │   ├── ExampleRobolectricTest.kt
    │   └── data/
    │       ├── local/
    │       │   ├── ComplementaryCurrencyCatalogTest.kt
    │       │   ├── DolarapDatabaseIntegrationTest.kt
    │       │   └── KoinDatabaseModuleTest.kt
    │       └── remote/
    │           ├── QuoteRemoteMapperTest.kt
    │           ├── AvailableCatalogParserTest.kt
    │           ├── AwesomeApiRemoteDataSourceTest.kt
    │           └── KoinNetworkModuleTest.kt
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
   *(Ao executar o APK atual no dispositivo/emulador, será exibida apenas a tela provisória de inicialização criada na E01; os repositórios de domínio e as telas definitivas de cotações, pesquisa e conversor serão implementados nas etapas E04 e E05.)*
3. **Executar os testes unitários e de integração local (Room/AwesomeAPI/Koin via Robolectric + MockWebServer):**
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
  - `QuoteRemoteMapperTest` (3 testes unitários JUnit): mapeamento multi-par com preservação exata de decimais em string, rejeição de DTOs inválidos/não-BRL/fora de 3 letras/preço não positivo e desserialização de `ApiErrorDto`.
  - `AvailableCatalogParserTest` (4 testes unitários JUnit): filtragem estrita de `^[A-Z]{3}-BRL$` em JSON e XML (`<xml><USD-BRL>...</USD-BRL></xml>` e `<item>`), rejeição de códigos fora de 3 letras/turismo/PTAX/valores vazios e enriquecimento de nomes.
  - `AwesomeApiRemoteDataSourceTest` (10 testes de integração HTTP + Room via `MockWebServer` + Robolectric): busca multi-par, rejeição local de pares inválidos sem chamada HTTP, erro `404 CoinNotExists` em lote preservando pares válidos e bloqueando apenas o par identificado (e sem marcar nada se a mensagem omitir o código), HTTP `429`/`500`/JSON vazio/malformado, timeout e offline, catálogo JSON experimental, fallback para `/xml/available` em JSON inválido com persistência Room, ausência de fallback em HTTP `429`/`500` e preservação do cache local em payloads inválidos.
  - `KoinNetworkModuleTest` (1 teste de integração DI via Robolectric): resolução de `Moshi`, `OkHttpClient`, `Retrofit`, `AwesomeApiService`, mapper, parser e singleton `AwesomeApiRemoteDataSource`.
- **Total executado na última bateria (`2026-11-15`):** 32 testes executados em `:app:testDebugUnitTest` (32 aprovados, 0 falhas).
- **Testes instrumentados (`androidTest`):** Não executados até o momento por ausência de emulador Android/ADB no ambiente de build.

---

## Limitações Conhecidas

- **Escopo atual (E03):** Ainda não existem camada `domain` (modelos puros, `BigDecimal` converter e UseCases), `QuoteRepositoryImpl`/`CatalogRepositoryImpl`, telas funcionais nem sincronização em background com WorkManager.
- **WorkManager (planejado para E06):** Tarefas periódicas de 6 horas dependem das políticas de energia do Android (Doze/App Standby) e não garantem horário exato.
- **AwesomeAPI:** Cache de 1 minuto para requisições não autenticadas e risco de bloqueio temporário de IP por erros HTTP 404 recorrentes (`CoinNotExists`), mitigado na E03 via validação local e bloqueio de pares inválidos.
- **Fora de escopo:** Sem login, integração bancária, pagamentos, compra/venda real, carteira, gráficos avançados, alertas/notificações ou backend próprio.

---

## Evidências e Entrega

- **Status atual:** Aguardando revisão externa da **Etapa E03** para aprovação e liberação da **Etapa E04**.
- **Evidências finais (APK definitivo, capturas de tela e consolidação final):** Serão adicionadas na **Etapa E07**.
