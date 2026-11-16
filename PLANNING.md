# PLANNING — DOLARAP

> **Status do documento:** Planejamento técnico atualizado após as etapas E01–E06 (E00–E04 aprovadas; E05 e E06 implementadas e testadas, aguardando revisão externa; E07 planejada).

## 1. Arquitetura Proposta

O **DOLARAP** será estruturado em um único módulo Gradle (`:app`) seguindo os princípios de **Clean Architecture** combinados com o padrão **MVVM (Model-View-ViewModel)** na camada de apresentação.

### Regra de Dependência
- `presentation` depende apenas de `domain`.
- `data` implementa as abstrações (interfaces de repositório) definidas em `domain`.
- `di` conecta `data`, `domain` e `presentation` via Koin.
- **`domain` é uma camada Kotlin pura**: não possui imports de `android.*`, `androidx.*`, `retrofit2.*`, `androidx.room.*` ou `presentation.*`.

```text
presentation (Compose + ViewModels + StateFlow)
       │
       ▼
    domain (Models + Repository Interfaces + UseCases + BigDecimal Calculator)
       ▲
       │
     data (Room + Retrofit + AwesomeAPI DTOs + Mappers + WorkManager Scheduler)
```

---

## 2. Tecnologias e Justificativas

| Tecnologia | Papel no Projeto | Justificativa |
|---|---|---|
| **Kotlin** | Linguagem principal | Segurança de tipos, null-safety, coroutines e expressividade para Clean Architecture. |
| **Jetpack Compose + Material 3** | Interface declarativa | UI reativa alinhada a `StateFlow`, menor boilerplate e suporte nativo a estados de tela. |
| **Navigation Compose** | Roteamento entre telas | Navegação declarativa e integrada ao ciclo de vida do Compose. |
| **Koin** | Injeção de Dependência | Leve, idiomático em Kotlin, sem geração complexa de grafos multi-módulo, ideal para `:app` único. |
| **Kotlin Coroutines + Flow / StateFlow** | Assincronismo e reatividade | Observação contínua do Room via `Flow` e gerenciamento previsível de estado na UI com `StateFlow`. |
| **Retrofit + OkHttp** | Cliente HTTP REST | Consumo tipado dos endpoints da AwesomeAPI com timeouts, logging e tratamento consistente de HTTP status. |
| **Room** | Banco de dados local (SSOT) | Persistência robusta em SQLite com suporte nativo a `Flow`, consultas reativas e integridade offline. |
| **WorkManager** | Sincronização em segundo plano | Execução periódica (6h) e condicional à conectividade (`NetworkType.CONNECTED`) respeitando políticas do Android. |
| **java.math.BigDecimal** | Cálculos monetários | Evita erros de precisão binária inerentes a `Double`/`Float` em conversões financeiras. |
| **JUnit (e Coroutines Test / Robolectric se aplicável)** | Testes unitários | Verificação rápida e determinística de casos de uso, regras de negócio, mappers e repositório na JVM. |

> **Nota sobre o estado das dependências (após E06):** O projeto possui configurados e ativos no Gradle `Kotlin 2.2.10`, `AGP 9.1.1`, `KSP 2.3.5`, `Compose BOM 2024.09.00`, `Navigation Compose 2.8.9`, `Koin BOM 4.1.1` (incluindo `koin-androidx-workmanager`), `Room 2.7.0`, `Retrofit 2.12.0 + Moshi 1.15.2 (com codegen KSP) + OkHttp 4.10.0 (+ MockWebServer 4.10.0 em testes)`, `WorkManager 2.10.1` (`work-runtime-ktx` ativo na E06), `Coroutines 1.10.2`, `JUnit 4.13.2`, `Robolectric 4.16.1` e `Compose UI Test`. Até a E06, estão implementados a fundação Koin, a camada local Room (`E02`), a camada remota da AwesomeAPI com autenticação por API Key (`E03` + Auth), a camada pura de domínio e repositórios SSOT (`E04`), a interface completa em Jetpack Compose + Navigation Compose (`E05`) e a sincronização automática + Offline First completo com WorkManager (`E06`).

---

## 3. Estrutura de Pacotes (`:app`)

Dentro de `app/src/main/java/com/aistudio/dolarap/` (`namespace` e `applicationId` = `com.aistudio.dolarap`):

```text
com.aistudio.dolarap
├── DolarapApplication.kt               # Inicialização do Koin, Configuration.Provider (KoinWorkerFactory) e agendamento periódico 6h (E06)
├── MainActivity.kt                     # Entry point Compose com enableEdgeToEdge + DolarapTheme + DolarapApp
├── di/
│   ├── AppModule.kt                    # Lista de módulos Koin ativos (database, network, data, domain, presentation)
│   ├── DatabaseModule.kt               # (E02) Provisão singleton de DolarapDatabase e DAOs
│   ├── NetworkModule.kt                # (E03 + Auth) Moshi, OkHttpClient, AwesomeApiAuthInterceptor, Retrofit, AwesomeApiService e RemoteDataSource
│   ├── DataModule.kt                   # (E04 + E06) SystemTimeProvider, Mappers, Repositories SSOT, SyncScheduler e SyncQuotesWorker
│   ├── DomainModule.kt                 # (E04 + E06) CurrencyConverter, QuoteFreshnessEvaluator e 9 UseCases (RefreshQuotesUseCase integrado ao SyncScheduler)
│   └── PresentationModule.kt           # (E05 + E06) ViewModels (QuotesViewModel observando SyncScheduler, CurrencySearchViewModel, ConverterViewModel)
├── domain/                             # (E04) Camada Kotlin/JDK pura
│   ├── model/
│   │   ├── CurrencyModels.kt           # CurrencyDefaults, QuoteFreshnessStatus, QuoteRate, CurrencyQuote, SyncStatus, DashboardQuotesSnapshot
│   │   ├── AvailableCurrency.kt        # Item do catálogo de moedas disponíveis e seu par suportado
│   │   ├── ConversionResult.kt         # Resultado de conversão (taxas bid/ask, effectiveRate, casas decimais, isStale)
│   │   └── DomainResult.kt             # DomainResult<T> e DomainError (NoConnection, Timeout, UnsupportedPair, etc.)
│   ├── repository/
│   │   ├── QuoteRepository.kt          # Contrato para observar/sincronizar cotações e gerenciar moedas/favoritos
│   │   └── CatalogRepository.kt        # Contrato para catálogo de moedas disponíveis e validação de suporte
│   ├── service/
│   │   ├── CurrencyConverter.kt        # (E04) Regra pura de cálculo com BigDecimal e taxa bid
│   │   ├── QuoteFreshnessEvaluator.kt  # (E04) Regra de 15 minutos (FRESH / STALE / UNAVAILABLE)
│   │   ├── TimeProvider.kt             # (E04) Abstração pura de relógio para testabilidade
│   │   └── SyncScheduler.kt            # (E06) Abstração pura de agendamento (periódico 6h, retry com rede e observeSyncState)
│   └── usecase/
│       ├── ObserveDashboardQuotesUseCase.kt
│       ├── RefreshQuotesUseCase.kt
│       ├── ObserveAvailableCurrenciesUseCase.kt
│       ├── SearchAvailableCurrenciesUseCase.kt
│       ├── RefreshCatalogUseCase.kt
│       ├── AddCurrencyUseCase.kt
│       ├── RemoveCurrencyUseCase.kt
│       ├── ToggleFavoriteUseCase.kt
│       └── ConvertCurrencyUseCase.kt
├── data/
│   ├── local/
│   │   ├── DolarapDatabase.kt          # RoomDatabase + callback de seed das 10 moedas padrão
│   │   ├── dao/
│   │   │   ├── CurrencyDao.kt          # CRUD + Flow de moedas selecionadas/favoritas
│   │   │   ├── QuoteDao.kt             # Upsert + Flow de cotações atuais
│   │   │   ├── AvailablePairDao.kt     # Cache do catálogo de pares suportados
│   │   │   └── SyncMetadataDao.kt      # Registro da última sincronização bem-sucedida
│   │   ├── entity/
│   │   │   ├── SelectedCurrencyEntity.kt
│   │   │   ├── QuoteEntity.kt
│   │   │   ├── AvailablePairEntity.kt
│   │   │   ├── SyncMetadataEntity.kt
│   │   │   └── SelectedCurrencyWithQuote.kt
│   │   └── catalog/
│   │       └── ComplementaryCurrencyCatalog.kt # Nomes amigáveis PT-BR e símbolos complementares (sem burlar suporte da API)
│   ├── mapper/                         # (E04)
│   │   ├── QuoteEntityMapper.kt        # Entidades Room -> Modelos de Domínio (BigDecimal + frescor)
│   │   └── RemoteErrorMapper.kt        # RemoteError -> DomainError + tag persistida em sync_metadata
│   ├── remote/
│   │   ├── AwesomeApiConfig.kt         # (E03) URL base, endpoints, timeouts e constantes
│   │   ├── AwesomeApiService.kt        # (E03) Endpoints /json/last/{pairs}, /json/available e /xml/available
│   │   ├── AwesomeApiRemoteDataSource.kt # (E03) Orquestração HTTP, fallback XML e bloqueio de 404
│   │   ├── dto/
│   │   │   ├── QuoteDto.kt             # (E03) DTO defensivo da resposta da AwesomeAPI
│   │   │   └── ApiErrorDto.kt          # (E03) DTO de erro {"status", "code", "message"}
│   │   ├── mapper/
│   │   │   └── QuoteRemoteMapper.kt    # (E03) Mapeia QuoteDto -> QuoteEntity preservando decimais exatos
│   │   ├── model/
│   │   │   └── RemoteResult.kt         # (E03) RemoteResult<T> e RemoteError tipados na camada data
│   │   └── parser/
│   │       └── AvailableCatalogParser.kt # (E03) Parsing defensivo JSON e fallback XML (/xml/available)
│   ├── repository/                     # (E04)
│   │   ├── QuoteRepositoryImpl.kt      # SSOT Room + deduplicação de sync + lote 25 pares + proteção padrão
│   │   └── CatalogRepositoryImpl.kt    # SSOT Room + deduplicação de catálogo + validação *-BRL
│   ├── time/                           # (E04)
│   │   └── SystemTimeProvider.kt
│   └── worker/                         # (E06) Sincronização em segundo plano com WorkManager
│       ├── SyncQuotesWorker.kt         # CoroutineWorker com tratamento de HTTP 429/Retry-After, backoff limitado e falhas permanentes
│       └── WorkManagerSyncScheduler.kt # Agendamento único (periódico 6h + retry CONNECTED) e Flow<BackgroundSyncInfo>
└── presentation/                       # (E05) Interface Jetpack Compose + MVVM
    ├── navigation/
    │   └── DolarapNavGraph.kt          # DolarapApp, DolarapNavHost, rotas e NavigationBar/NavigationRail adaptativa
    ├── theme/                          # Color.kt, Theme.kt, Type.kt (Plus Jakarta Sans & JetBrains Mono locais)
    ├── format/
    │   ├── BrazilianFormatter.kt       # Formatação pt-BR direto de BigDecimal e datas/horas
    │   └── DomainErrorMessageMapper.kt # UiText e mapeamento de DomainError para strings localizadas
    ├── components/
    │   └── DolarapComponents.kt        # FreshnessBadge, VariationPill, StatusBannerCard, Loading/Empty/Error
    ├── quotes/
    │   ├── QuotesScreen.kt             # Tela inicial de cotações + filtro de favoritos
    │   ├── QuotesUiState.kt
    │   └── QuotesViewModel.kt
    ├── search/
    │   ├── CurrencySearchScreen.kt     # Tela de pesquisa e adição de moedas
    │   ├── CurrencySearchUiState.kt
    │   └── CurrencySearchViewModel.kt
    └── converter/
        ├── ConverterScreen.kt          # Tela de conversor de moedas
        ├── ConverterUiState.kt
        └── ConverterViewModel.kt
```

---

## 4. Fluxo de Dados

### 4.1. Fluxo Reativo de Leitura (Offline First)
1. O Room emite continuamente alterações das tabelas `selected_currencies`, `quotes` e `sync_metadata` por meio de `Flow`.
2. `QuoteRepositoryImpl` combina esses fluxos e mapeia para modelos de domínio (`CurrencyQuote` + `SyncStatus`), calculando se os dados excederam o limite de 15 minutos (`isStale`).
3. O caso de uso expõe o `Flow` para o `ViewModel`.
4. O `ViewModel` transforma o fluxo em `StateFlow<UiState>` usando `stateIn`.
5. A UI em Jetpack Compose coleta via `collectAsStateWithLifecycle()` e renderiza imediatamente os dados locais ao abrir o app.

### 4.2. Fluxo de Atualização / Sincronização
1. Trigger (abertura do app, ação manual do usuário ou `SyncQuotesWorker`).
2. `RefreshQuotesUseCase` obtém do Room a lista de códigos selecionados pelo usuário e valida os pares contra `available_pairs` (`{CODE}-BRL`).
3. `AwesomeApiService` consulta `/json/last/{pairs}` em lote (ex.: `USD-BRL,EUR-BRL,...`).
4. Em caso de sucesso:
   - Mapeia os campos válidos (`bid`, `ask`, `varBid`, `pctChange`, `timestamp`, `create_date`) para `QuoteEntity`.
   - Grava as cotações e o timestamp da atualização bem-sucedida em transação no Room (`SyncMetadataEntity`).
   - O Room notifica automaticamente os `Flow` ativos e a UI se atualiza sem necessidade de reload manual.
5. Em caso de falha de rede/API:
   - Os dados anteriores do Room **são preservados intactos**.
   - Se a falha for de conectividade (`IOException`), agenda-se trabalho único no WorkManager com restrição `NetworkType.CONNECTED`.
   - A UI exibe indicador discreto de falha/offline mantendo a lista visível.

---

## 5. Integração com a AwesomeAPI

Documentação oficial consultada: `https://docs.awesomeapi.com.br/api-de-moedas`.

### 5.1. Endpoint de Cotações
- **URL:** `GET https://economia.awesomeapi.com.br/json/last/{pairs}`
- **Exemplo:** `GET https://economia.awesomeapi.com.br/json/last/USD-BRL,EUR-BRL`
- **Formato real da resposta:** Objeto JSON dinâmico onde cada chave é a concatenação do par **sem hífen** (ex.: `"USDBRL"`, `"EURBRL"`).

```json
{
  "USDBRL": {
    "code": "USD",
    "codein": "BRL",
    "name": "Dólar Americano/Real Brasileiro",
    "high": "5.7340",
    "low": "5.7279",
    "varBid": "-0.0054",
    "pctChange": "-0.09",
    "bid": "5.7276",
    "ask": "5.7282",
    "timestamp": "1618315045",
    "create_date": "2021-04-13 08:57:27"
  }
}
```

- **Campos relevantes utilizados no DOLARAP:**
  - `code`: moeda de origem (ex.: `"USD"`).
  - `codein`: moeda de destino (ex.: `"BRL"`).
  - `name`: nome descritivo do par (ex.: `"Dólar Americano/Real Brasileiro"`).
  - `bid`: preço de compra (string decimal; taxa padrão para conversão).
  - `ask`: preço de venda (string decimal).
  - `varBid`: variação absoluta do bid (string decimal).
  - `pctChange`: variação percentual (string decimal).
  - `timestamp`: timestamp Unix em segundos (string numérica).
  - `create_date`: data/hora formatada `AAAA-MM-DD HH:MM:SS`.

### 5.2. Endpoint de Moedas Disponíveis (`/json/available` e achados da documentação oficial)
Ao investigar a documentação oficial da AwesomeAPI (`https://docs.awesomeapi.com.br/api-de-moedas`), foram identificadas características e **limitações importantes**:

1. **Formato documentado oficialmente:** Na documentação oficial da AwesomeAPI, os endpoints de catálogo listados explicitamente são em **XML**:
   - `https://economia.awesomeapi.com.br/xml/available` — lista completa de combinações/pares de moedas suportados.
   - `https://economia.awesomeapi.com.br/xml/available/uniq` — lista com códigos únicos e nomes das moedas.
   O caminho `/json/available` solicitado no escopo aparece em referências comunitárias, porém **não consta formalmente especificado com schema garantido na documentação oficial consultada** (ou pode variar entre mapa JSON plano `{"USD-BRL": "Dólar Americano/Real Brasileiro"}`, resposta XML ou indisponibilidade).
2. **Estratégia segura de consumo do catálogo (auditada na E03):**
   - O endpoint **`/xml/available` é a fonte oficial documentada** pela AwesomeAPI. O endpoint `/json/available` é tratado apenas como **tentativa experimental** (sem afirmar que possui schema JSON oficialmente garantido).
   - `AwesomeApiRemoteDataSource.syncAvailableBrlCatalog()` tenta `/json/available` e aciona o fallback oficial `/xml/available` quando o JSON retornar erro de endpoint (ex.: 404/400/415) ou formato inválido/incompatível/vazio. **Não** executa segunda requisição em HTTP `429` (rate limit), HTTP `5xx` (`ServerError`), `Timeout` ou `NoConnection`.
   - `AvailableCatalogParser` aceita estritamente pares no padrão `^[A-Z]{3}-BRL$` com descrição válida, rejeitando pares cruzados, turismo (`BRLT`), `BRLPTAX`, códigos fora de 3 letras e valores inválidos. Em caso de falha de ambos os endpoints, o cache existente no Room é preservado.
3. **Limitação de nomes amigáveis e identificação de pares:**
   - O catálogo `/available` traz pares combinados (nem todas as moedas do mundo possuem par direto com `BRL`, e algumas descrições vêm no formato `"Moeda Origem/Real Brasileiro"` ou apenas códigos abreviados).
   - Além disso, o catálogo não traz símbolos monetários (`$`, `€`, `£`) nem termos alternativos de busca sem acentuação.
4. **Estratégia de Catálogo Complementar (sem inventar suporte):**
   - O aplicativo manterá um **catálogo complementar local estático** (`ComplementaryCurrencyCatalog`) contendo nomes amigáveis em português, sinônimos de busca e símbolos para códigos ISO conhecidos (usando também `java.util.Currency` como apoio).
   - **Regra estrita:** O catálogo complementar **nunca** autoriza a adição ou consulta de uma moeda por conta própria. A elegibilidade de qualquer moeda continua **100% condicionada** à presença do par `{CODE}-BRL` na resposta oficial de `/available` da AwesomeAPI (persistida localmente no Room). Se um código existir no catálogo complementar mas não tiver `{CODE}-BRL` suportado na AwesomeAPI, ele não será exibido como adicionável para cotação direta.

### 5.3. Construção e Validação de Pares
- Para a tela de cotações, todo par consultado segue o padrão `{CODE}-BRL` (ex.: `USD-BRL`).
- Antes de adicionar uma moeda ou incluí-la em uma requisição `/json/last/`, o sistema verifica se `{CODE}-BRL` existe na tabela `available_pairs` obtida da AwesomeAPI.
- Em chamadas em lote (`/json/last/USD-BRL,EUR-BRL,...`), o tamanho do lote será limitado/particionado se necessário para evitar URLs excessivamente longas ou falha total caso um par deixe de ser suportado.

### 5.4. Limitações e Tratamento de Erros da API
- **Cache de 1 minuto:** Requisições não autenticadas na AwesomeAPI possuem cache padrão de 1 minuto no servidor.
- **Erro HTTP 404 (`CoinNotExists`):**
  ```json
  {
    "status": 404,
    "code": "CoinNotExists",
    "message": "moeda nao encontrada ABC-BRL"
  }
  ```
- **Bloqueio por 404 excessivos:** Conforme alerta oficial da AwesomeAPI, requisições repetidas que geram HTTP 404 podem acarretar **bloqueio temporário de IP por ~1 hora**. Por isso:
  - Pares inválidos são barrados antes do request via regex `^[A-Z]{3}-BRL$` e pela tabela local `available_pairs`.
  - Em resposta `404 CoinNotExists` (inclusive em consulta em lote), o sistema identifica o par inexistente **somente quando a resposta permitir** (ou quando houver apenas 1 par na requisição), marca apenas esse par como indisponível (`isAvailable = false`) e o bloqueia na sessão, preservando integralmente os demais pares válidos e evitando marcar todos os pares como inválidos sem evidência.
- **Erros HTTP 429 / 5xx / Timeout / Falha de Parsing:** Mapeados para `DomainError`, preservam os dados locais e, no caso de falha de rede, agendam retry condicionado à conectividade.

---

## 6. Estratégia Offline First

1. **Fonte Única da Verdade (SSOT):** A UI lê exclusivamente do Room via `Flow`. Nenhuma resposta Retrofit alimenta diretamente a interface sem passar pelo Room.
2. **Abertura instantânea:** Ao abrir o app, as 10 moedas padrão (garantidas no Room desde a criação/seed do banco) e suas últimas cotações salvas são renderizadas imediatamente.
3. **Regra dos 15 minutos (`isStale`):**
   - Cada sincronização bem-sucedida grava `lastSuccessfulSyncAtMillis` na tabela `sync_metadata`.
   - Se `lastSuccessfulSyncAtMillis == null` (ex.: primeiro uso sem internet), os dados aparecem com estado de cotação ainda não obtida (`"--"` / aguardando primeira sincronização).
   - Se `System.currentTimeMillis() - lastSuccessfulSyncAtMillis > 15 * 60 * 1000`, `SyncStatus.isStale = true`.
   - A interface exibe banner/chip explícito informando há quanto tempo os dados foram atualizados e destacando que estão **desatualizados (> 15 min)** ou **offline**.
4. **Resiliência:** Sem conexão, pesquisas no catálogo ainda funcionam sobre o último catálogo `available_pairs` salvo no Room; caso o catálogo nunca tenha sido baixado e o usuário esteja offline, a tela de pesquisa exibe estado específico informando necessidade de conexão inicial para carregar o catálogo oficial.

---

## 7. Persistência com Room

### Entidades Planejadas

1. **`SelectedCurrencyEntity`** (`selected_currencies`)
   - `code: String` (PK — ex.: `"USD"`)
   - `pairCode: String` (ex.: `"USD-BRL"`)
   - `displayName: String` (nome amigável resolvido)
   - `isDefault: Boolean` (`true` para as 10 padrão; `false` para adicionadas pelo usuário)
   - `isFavorite: Boolean` (`false` por padrão)
   - `sortOrder: Int`
   - `addedAtMillis: Long`

2. **`QuoteEntity`** (`quotes`)
   - `pairCode: String` (PK — ex.: `"USD-BRL"`)
   - `code: String`
   - `codeIn: String` (`"BRL"`)
   - `apiName: String`
   - `bid: String` (armazenado como texto decimal exato para conversão em `BigDecimal`)
   - `ask: String`
   - `varBid: String`
   - `pctChange: String`
   - `high: String`
   - `low: String`
   - `apiTimestampSeconds: Long`
   - `createDate: String`
   - `fetchedAtMillis: Long`

3. **`AvailablePairEntity`** (`available_pairs`)
   - `pairCode: String` (PK — ex.: `"CHF-BRL"`)
   - `baseCode: String` (ex.: `"CHF"`)
   - `quoteCode: String` (ex.: `"BRL"`)
   - `apiDescription: String`
   - `friendlyName: String`
   - `isAvailable: Boolean` (indica se o par segue suportado; padrão `true`)
   - `updatedAtMillis: Long`

4. **`SyncMetadataEntity`** (`sync_metadata`)
   - `id: Int` (PK fixa = `1`)
   - `lastSuccessfulSyncAtMillis: Long?`
   - `lastAttemptAtMillis: Long?`
   - `lastErrorType: String?`
   - `lastCatalogSyncAtMillis: Long?`

5. **Relação `SelectedCurrencyWithQuote` (`@Embedded` + `@Relation`)**
   - Associa `SelectedCurrencyEntity` à sua `QuoteEntity?` opcional pelo campo `pairCode`, garantindo exibição imediata offline mesmo antes da primeira cotação.

### Seed Inicial das 10 Moedas Padrão
No `RoomDatabase.Callback.onCreate` (e verificação idempotente na inicialização do repositório), inserem-se as 10 moedas padrão com `isDefault = true`:
`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY`, `MXN`.

---

## 8. Estratégia de Sincronização

| Gatilho | Mecanismo | Política / Restrição | Comportamento |
|---|---|---|---|
| **Abertura do app** | Coroutine no `QuotesViewModel` / `RefreshQuotesUseCase` | Executa em `viewModelScope` | Busca cotações atuais (e atualiza catálogo se expirado/vazio). Se falhar por rede, agenda worker condicional. |
| **Atualização manual** | Pull-to-refresh / Botão atualizar | Debounce básico / evita chamadas concorrentes (`Mutex` no repositório) | Atualiza cotações e dá feedback visual na UI sem apagar dados existentes. |
| **Periódico (6 horas)** | `PeriodicWorkRequestBuilder<SyncQuotesWorker>(6, TimeUnit.HOURS)` | `ExistingPeriodicWorkPolicy.KEEP`, `NetworkType.CONNECTED` | Atualiza cotações em background para as moedas selecionadas no Room. |
| **Retry após falha de rede** | `OneTimeWorkRequestBuilder<SyncQuotesWorker>()` | `ExistingWorkPolicy.KEEP`, `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL` | Dispara automaticamente quando o sistema recuperar conectividade, evitando trabalhos duplicados. |

> **Observação importante sobre WorkManager:** O WorkManager é uma ferramenta de execução em segundo plano sujeita às políticas de economia de energia do Android (como *Doze Mode*, *App Standby Buckets* e restrições de fabricantes). Portanto, o intervalo de 6 horas **não garante execução no minuto exato**, funcionando em janela gerenciada pelo sistema operacional.

---

## 9. Telas Planejadas e Estados de UI

1. **Tela Inicial de Cotações (`QuotesScreen`)**
   - Cabeçalho com resumo de status (última atualização bem-sucedida, badge de **Desatualizado (> 15 min)** e badge de **Offline** quando aplicável).
   - Alternância ou filtro integrado para **Todas** vs **Favoritas** (Área/Filtro de Favoritos).
   - Botão/ação de atualização manual e pull-to-refresh.
   - Cards com código, nome, `bid`, `ask`, variação (`varBid` / `pctChange`), botão de favoritar e ação de remover (visível apenas para moedas não padrão).
   - Estados tratados: `Loading` (apenas quando ainda não há dados locais carregados), `Success`, `Empty` (ex.: filtro de favoritos sem itens marcados), `Error` (banner não destrutivo sobre dados em cache ou tela de erro caso não haja cache algum).

2. **Tela de Pesquisa e Adição de Moedas (`CurrencySearchScreen`)**
   - Campo de busca por código ou nome amigável.
   - Lista de moedas com par `*-BRL` suportado no catálogo oficial.
   - Indicação clara de moedas já adicionadas / padrão (sem opção de remover as 10 padrão) e botão para adicionar novas moedas suportadas.
   - Aviso claro caso o catálogo ainda não tenha sido carregado offline ou caso o termo pesquisado não encontre moedas suportadas na AwesomeAPI.
   - Estados tratados: `Loading`, `Success`, `Empty` (nenhum resultado encontrado) e `Error`.

3. **Tela de Conversor (`ConverterScreen`)**
   - Campo de valor numérico validado para `BigDecimal`.
   - Seletores de moeda de origem e destino (incluindo `BRL` e moedas com par suportado), com botão de inverter origem/destino.
   - Exibição do resultado calculado, taxa `bid` aplicada e aviso quando os dados usados na conversão estiverem desatualizados (> 15 min).
   - Tratamento explícito quando origem ou destino não tiver cotação disponível localmente (desabilita resultado inconsistente e orienta sincronização).
   - Estados tratados: `Loading`, `Success`, `Empty` (valor vazio/zero), `Error` (cotação indisponível para o par escolhido).

4. **Área ou Filtro de Favoritos**
   - Integrado à navegação/filtro principal da tela de cotações (chips/abas `Todas` | `Favoritas`) permitindo acesso em um toque às moedas marcadas, mantendo estado vazio amigável quando não houver favoritos.

---

## 10. Estratégia de Testes

- **Testes Unitários (JUnit 4 + Kotlin Coroutines Test):**
  - `CurrencyConverterTest`: precisão com `BigDecimal`, conversão `X -> BRL`, `BRL -> Y`, `X -> Y` cruzada, mesma moeda, entrada zero/negativa/inválida, divisão segura (`RoundingMode.HALF_EVEN`/`HALF_UP`), moeda sem cotação disponível e uso exclusivo de `bid`.
  - `StaleStatusEvaluatorTest`: regra exata de 15 minutos (`14m59s` atual vs `15m01s` desatualizado, ausência de sync).
  - `RemoveCurrencyUseCaseTest`: bloqueio de remoção das 10 moedas padrão e permissão para moedas adicionadas pelo usuário.
  - `AddCurrencyUseCaseTest`: bloqueio de adição de par não suportado no catálogo e sucesso para par suportado.
  - `SearchAvailableCurrenciesUseCaseTest`: busca por código e nome (case-insensitive e sem acentos).
  - `QuoteRemoteMapperTest` e `AvailableCatalogParserTest` (implementados na E03) + `AwesomeApiRemoteDataSourceTest` (com `MockWebServer`): parsing de strings decimais da AwesomeAPI, resposta válida, JSON/XML inválido ou campos ausentes, fallback automático `/json/available` -> `/xml/available` e tratamento de erro HTTP 404 `CoinNotExists` / timeout / offline.
  - `QuoteRepositoryImplTest` (planejado E04, com fakes/in-memory): comportamento Offline First, preservação de dados locais em erro HTTP/rede, persistência de favoritos e timestamp de sync.
- **Validações Manuais Planejadas:**
  - Documentadas em `TEST_RESULTS.md` cobrindo fluxo completo no app (primeira carga, pesquisa/adição/remoção, favoritos após kill/reopen, conversor, modo avião, alerta > 15 min, retorno de conexão e execução do Worker).

---

## 11. Decisões Técnicas e Limitações Conhecidas

1. **Conversão entre duas moedas estrangeiras:** Como a consulta principal foca pares em relação ao `BRL`, a conversão `X -> Y` (ambas != `BRL`) será calculada por triangulação via `BRL` utilizando os respectivos `bid` (`bid(X-BRL) / bid(Y-BRL)`). Isso garante consistência com a lista exibida sem exigir N² consultas na API, mas deve ser documentado como taxa derivada de `bid` em relação ao BRL.
2. **Catálogo oficial XML vs. tentativa experimental JSON:** Como a documentação oficial da AwesomeAPI define `/xml/available`, a camada `data/remote` (E03) trata `/xml/available` como fonte oficial e `/json/available` apenas como tentativa experimental com fallback XML (bloqueando segunda chamada em 429, 5xx, timeout e offline, sem comprometer o cache salvo no Room).
3. **Cache de 1 minuto da AwesomeAPI:** Atualizações manuais em intervalos menores que 1 minuto podem retornar a mesma cotação em cache do provedor gratuito.
4. **WorkManager não exato:** O intervalo de 6 horas representa periodicidade mínima/aproximada gerenciada pelo sistema Android.
5. **Dependência Koin vs template inicial:** O template inicial ainda não possui Koin, WorkManager ou Navigation Compose descomentados; essa adequação ocorrerá somente na Etapa E01.
