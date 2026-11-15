# DOLARAP

> **Estado atual do projeto:** **Etapa E02 concluída (persistência local Room + testes automatizados) e aguardando revisão externa.**
> As funcionalidades de rede/API (AwesomeAPI), camada de domínio (`BigDecimal`/UseCases), interface funcional em Compose e sincronização com WorkManager permanecem **planejadas** para as etapas E03–E07.

Aplicativo Android nativo para consulta e conversão de cotações de moedas estrangeiras em relação ao Real brasileiro (**BRL**), projetado com foco em **Offline First**, precisão monetária e arquitetura limpa em módulo único (`:app`).

---

## Progresso do Desenvolvimento (E00–E07)

| Etapa | Descrição | Status |
|---|---|---|
| **E00** | Especificação e planejamento (`SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `README.md`, `TEST_RESULTS.md`) | **Aprovada** |
| **E01** | Preparação do projeto Android (`:app`, `com.aistudio.dolarap`, dependências base, ícone, Koin bootstrap) | **Aprovada** |
| **E02** | Banco de dados local Room (entidades, relação, DAOs com `Flow`, seed das 10 moedas padrão, Koin e testes) | **Implementação e testes concluídos — aguardando revisão externa** |
| **E03** | Integração com a AwesomeAPI (Retrofit/Moshi, endpoints `/json/last` e `/available`, tratamento de erros) | Pendente |
| **E04** | Camada de domínio e repositórios (modelos puros, `BigDecimal`, UseCases, regra de 15 min, SSOT) | Pendente |
| **E05** | Interface Jetpack Compose (Cotações, Pesquisa/Adição, Conversor, Filtro de Favoritos, Navigation Compose) | Pendente |
| **E06** | Offline First, sincronização com WorkManager (6h e retry `CONNECTED`) e validação completa | Pendente |
| **E07** | Entrega final, documentação consolidada, APK e versionamento | Pendente |

---

## Funcionalidades Implementadas vs. Planejadas

### Já implementado (até a Etapa E02)
- **Estrutura base Android (`E01`):** Módulo único `:app` com `namespace` e `applicationId` configurados para `com.aistudio.dolarap`, permissões `INTERNET` e `ACCESS_NETWORK_STATE`, ícone adaptativo customizado, tema Material 3 (`DolarapTheme`) e tela provisória de inicialização (`BootstrapPlaceholder` na `MainActivity`).
- **Inicialização de DI com Koin (`E01`–`E02`):** `DolarapApplication` inicializando Koin de forma segura (`appModules`), com `databaseModule` provendo `DolarapDatabase` e os quatro DAOs como singletons.
- **Persistência local com Room (`E02`):**
  - **4 entidades Room:**
    - `SelectedCurrencyEntity` (`selected_currencies`): moedas monitoradas pelo usuário, marcação `isDefault`, estado de favorito (`isFavorite`) e ordenação.
    - `QuoteEntity` (`quotes`): cotações armazenadas com valores decimais exatos em texto (`bid`, `ask`, `varBid`, `pctChange`, `high`, `low`) para futuro uso em `BigDecimal`.
    - `AvailablePairEntity` (`available_pairs`): cache local dos pares validados na API e flag `isAvailable`.
    - `SyncMetadataEntity` (`sync_metadata`): registro único de timestamps da última sincronização bem-sucedida, tentativas, erros e catálogo.
  - **Relação Room:** `SelectedCurrencyWithQuote` (`@Embedded` + `@Relation`) unindo cada moeda selecionada à sua cotação opcional (`0..1`).
  - **4 DAOs com consultas reativas via `Flow`:** `CurrencyDao` (incluindo favoritos e proteção `AND isDefault = 0` na exclusão), `QuoteDao`, `AvailablePairDao` (filtro `*-BRL` e busca por código/nome) e `SyncMetadataDao`.
  - **Seed inicial e catálogo complementar (`E02`):** `DolarapDatabase` semeia de forma atômica e idempotente as **10 moedas padrão** (`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY`, `MXN`) na criação do banco; `ComplementaryCurrencyCatalog` enriquece nomes amigáveis em PT-BR, símbolos e normalização de busca sem autorizar moedas não suportadas pela API.
- **Testes da fundação e da camada Room (`E01`–`E02`):** 14 testes unitários e de integração local via JUnit 4 + Robolectric aprovados em `gradle :app:testDebugUnitTest`.

### Planejado para etapas futuras (ainda NÃO implementado)
- **Integração com a AwesomeAPI (`E03`):** Cliente Retrofit/Moshi, consulta a `/json/last/{pairs}`, validação do catálogo `/available`, parser e tratamento de erro HTTP 404 (`CoinNotExists`).
- **Camada `domain` e Repositórios (`E04`):** Modelos de domínio puros, calculadora monetária em `BigDecimal` utilizando `bid`, casos de uso de cotações, pesquisa/adição/remoção, favoritos e regra de obsolescência de 15 minutos.
- **Interface Funcional (`E05`):** Telas definitivas de Cotações (com filtro de favoritos), Pesquisa/Adição de Moedas e Conversor, com Navigation Compose e estados `Loading`, `Success`, `Empty` e `Error`.
- **Sincronização em Segundo Plano (`E06`):** `SyncQuotesWorker` com WorkManager (trabalho periódico de 6 horas e re-sincronização condicionada a `NetworkType.CONNECTED` sem duplicação).

---

## Arquitetura e Tecnologias

### Arquitetura Atual e Planejada
O projeto adota **Clean Architecture + MVVM** em módulo único `:app` (`com.aistudio.dolarap`), dividido em quatro pacotes principais:
- `presentation`: atualmente contém o tema Material 3 (`DolarapTheme`) e será expandido na E05 com telas Jetpack Compose, navegação e `ViewModel`s (`StateFlow`).
- `domain`: reservado para modelos puros, contratos de repositório, calculadora em `BigDecimal` e casos de uso (sem dependências de Android/Room/Retrofit) — será implementado na E04.
- `data`: atualmente contém toda a camada `data.local` (`DolarapDatabase`, entidades, DAOs e `ComplementaryCurrencyCatalog`); receberá `data.remote` (E03), `data.repository` (E04) e `data.worker` (E06).
- `di`: contém `AppModule.kt` e `DatabaseModule.kt` (Koin) já ativos; receberá os módulos de rede, domínio e apresentação nas próximas etapas.

### Stack Tecnológica Configurada no Gradle
- **Linguagem / Build:** Kotlin `2.2.10`, Android Gradle Plugin `9.1.1`, KSP `2.3.5`, `compileSdk 36`, `minSdk 24`, `targetSdk 36`.
- **Ativamente utilizados até a E02:**
  - **Jetpack Compose + Material 3** (BOM `2024.09.00`)
  - **Koin** (BOM `4.1.1`: `koin-core`, `koin-android`, `koin-androidx-compose`, `koin-androidx-workmanager`)
  - **Room** (`2.7.0`: `room-runtime`, `room-ktx`, `room-compiler` via KSP)
  - **Kotlin Coroutines & Flow** (`1.10.2`)
  - **Testes:** JUnit `4.13.2`, Kotlinx Coroutines Test `1.10.2`, Robolectric `4.16.1`
- **Declarados no Gradle para as próximas etapas (ainda sem implementação funcional):**
  - **Navigation Compose** (`2.8.9`) — E05
  - **Retrofit `2.12.0` + OkHttp `4.10.0` + Moshi `1.15.2`** — E03
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
    │   │   ├── AppModule.kt              # Lista de módulos Koin ativos
    │   │   └── DatabaseModule.kt         # Provisão singleton de DolarapDatabase e DAOs
    │   ├── data/local/
    │   │   ├── DolarapDatabase.kt        # Banco Room v1 + seed das 10 moedas padrão
    │   │   ├── catalog/ComplementaryCurrencyCatalog.kt
    │   │   ├── dao/                      # CurrencyDao, QuoteDao, AvailablePairDao, SyncMetadataDao
    │   │   └── entity/                   # 4 entidades Room + SelectedCurrencyWithQuote
    │   ├── domain/                       # Reservado para Etapa E04
    │   └── presentation/theme/           # Color.kt, Theme.kt, Type.kt
    ├── test/java/com/aistudio/dolarap/
    │   ├── ExampleUnitTest.kt
    │   ├── ExampleRobolectricTest.kt
    │   └── data/local/
    │       ├── ComplementaryCurrencyCatalogTest.kt
    │       ├── DolarapDatabaseIntegrationTest.kt
    │       └── KoinDatabaseModuleTest.kt
    └── androidTest/java/com/aistudio/dolarap/
        └── ExampleInstrumentedTest.kt
```

---

## API Prevista (Etapa E03 — Ainda Não Integrada)

- **Provedor:** AwesomeAPI — API de Cotações de Moedas (`https://docs.awesomeapi.com.br/api-de-moedas`)
- **Endpoint de cotações planejado:** `GET https://economia.awesomeapi.com.br/json/last/{pairs}` (ex.: `USD-BRL,EUR-BRL`)
- **Endpoint de disponibilidade a validar na E03:** `https://economia.awesomeapi.com.br/json/available` (com fallback oficial documentado para `https://economia.awesomeapi.com.br/xml/available`)

---

## Configuração, Compilação e Execução (Estado Atual)

1. **Requisitos do ambiente:** Android SDK 36, JDK 11+ e Gradle configurado no ambiente (ou Android Studio recente).
2. **Compilar o APK Debug:**
   ```sh
   gradle :app:assembleDebug
   ```
   *(Ao executar o APK atual no dispositivo/emulador, será exibida apenas a tela provisória de inicialização criada na E01; as telas de cotações, pesquisa e conversor serão implementadas na E05.)*
3. **Executar os testes unitários e de integração local (Room/Koin via Robolectric):**
   ```sh
   gradle :app:testDebugUnitTest
   ```
4. **Credenciais:** Não requer chaves pagas, login ou arquivo `google-services.json` para compilação e execução dos testes atuais.

---

## Testes Implementados e Executados

Os resultados detalhados estão registrados em [`TEST_RESULTS.md`](TEST_RESULTS.md):
- **Etapa E01:** `ExampleUnitTest` (1 teste) e `ExampleRobolectricTest` (1 teste validando `app_name = "DOLARAP"`).
- **Etapa E02:**
  - `ComplementaryCurrencyCatalogTest` (4 testes unitários JUnit): validação das 10 moedas padrão, identificação case-insensitive, resolução de nomes amigáveis e normalização de busca.
  - `DolarapDatabaseIntegrationTest` (7 testes de integração Room via Robolectric): seed automático no `onCreate`, idempotência de seed, proteção contra remoção das 10 moedas padrão, reatividade com `Flow` (`SelectedCurrencyWithQuote` e favoritos), persistência após reabertura de arquivo SQLite, busca/disponibilidade em `AvailablePairDao` e CRUD/observação em `SyncMetadataDao`.
  - `KoinDatabaseModuleTest` (1 teste de integração DI + Room via Robolectric): resolução singleton de `DolarapDatabase` e dos 4 DAOs via `DolarapApplication`.
- **Total executado na última bateria (`2026-11-15`):** 14 testes executados em `:app:testDebugUnitTest` (14 aprovados, 0 falhas).
- **Testes instrumentados (`androidTest`):** Não executados até o momento por ausência de emulador Android/ADB no ambiente de build.

---

## Limitações Conhecidas

- **Escopo atual (E02):** Ainda não existem integração de rede com a AwesomeAPI, casos de uso, conversor `BigDecimal`, telas funcionais nem sincronização com WorkManager.
- **WorkManager (planejado para E06):** Tarefas periódicas de 6 horas dependem das políticas de energia do Android (Doze/App Standby) e não garantem horário exato.
- **AwesomeAPI (planejado para E03):** Cache de 1 minuto para requisições não autenticadas, risco de bloqueio temporário por erros HTTP 404 recorrentes (`CoinNotExists`) e necessidade de validação empírica de `/json/available` frente aos endpoints XML documentados (`/xml/available`).
- **Fora de escopo:** Sem login, integração bancária, pagamentos, compra/venda real, carteira, gráficos avançados, alertas/notificações ou backend próprio.

---

## Evidências e Entrega

- **Status atual:** Aguardando revisão externa da **Etapa E02** para aprovação e liberação da **Etapa E03**.
- **Evidências finais (APK definitivo, capturas de tela e consolidação final):** Serão adicionadas na **Etapa E07**.
