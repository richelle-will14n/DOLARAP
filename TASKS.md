# TASKS — DOLARAP

> **Regra de status:** Nenhuma tarefa deve ser marcada como aprovada sem revisão externa. Diferenciamos conclusão técnica da implementação (`IMPLEMENTADA (AGUARDANDO REVISÃO)`) da aprovação formal da etapa (`APROVADA`).

Legenda de status:
- `PENDENTE`: ainda não iniciada ou aguardando implementação/validação.
- `EM ANDAMENTO`: em execução na etapa corrente.
- `IMPLEMENTADA (AGUARDANDO REVISÃO)`: código e testes concluídos e validados, aguardando revisão externa para aprovação da etapa.
- `APROVADA`: etapa e tarefas revisadas e aprovadas externamente.

---

## E00 — Especificação e planejamento *(Aprovada)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E00-T01** | Investigar documentação oficial da AwesomeAPI (`/json/last` e `/available`) | Endpoints, formato real das respostas, erros (404 `CoinNotExists`) e limitações registrados em `PLANNING.md`. | APROVADA |
| **E00-T02** | Elaborar especificação completa (`SPECIFICATION.md`) | Todos os RF, RNF, regras de negócio, fora de escopo, critérios de aceitação e rastreabilidade documentados. | APROVADA |
| **E00-T03** | Elaborar planejamento técnico (`PLANNING.md`) | Arquitetura Clean + MVVM, pacotes, fluxo Offline First, Room, WorkManager e estratégia de testes documentados. | APROVADA |
| **E00-T04** | Estruturar backlog incremental (`TASKS.md`) | Todas as 8 etapas (E00–E07) quebradas em tarefas pequenas, ordenadas e com critérios de conclusão. | APROVADA |
| **E00-T05** | Criar documentação inicial (`README.md` e `TEST_RESULTS.md`) | `README.md` alinhado ao escopo planejado e `TEST_RESULTS.md` com plano inicial de testes marcado como `NÃO EXECUTADO`. | APROVADA |
| **E00-T06** | Revisão e aprovação do planejamento pelo usuário | Usuário valida a documentação da Etapa 0 antes de iniciar qualquer código na Etapa E01. | APROVADA |

---

## E01 — Preparação do projeto *(Aprovada)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E01-T01** | Definir `applicationId` único e atualizar nome do app (`strings.xml`, `settings.gradle.kts`, `metadata.json`) | Projeto identifica-se como `DOLARAP` com `applicationId` único e `namespace` preservado. | APROVADA |
| **E01-T02** | Configurar dependências no `libs.versions.toml` e `app/build.gradle.kts` | Koin, Navigation Compose, WorkManager, Room, Retrofit, OkHttp, Coroutines e JUnit configurados e compilando sem erros. | APROVADA |
| **E01-T03** | Declarar permissões no `AndroidManifest.xml` | `INTERNET` e `ACCESS_NETWORK_STATE` declaradas corretamente no manifesto. | APROVADA |
| **E01-T04** | Criar estrutura de pacotes (`presentation`, `domain`, `data`, `di`) e classe `DolarapApplication` | Pacotes criados no módulo `:app`, `Application` registrada no manifesto com Koin inicializado e build verde. | APROVADA |
| **E01-T05** | Configurar tema Material 3 base e ícone adaptativo do DOLARAP | Tema base do DOLARAP configurado em `presentation/theme` e build executando sem erros. | APROVADA |

---

## E02 — Banco de dados local (Room) *(Aprovada)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E02-T01** | Criar entidades Room (`SelectedCurrencyEntity`, `QuoteEntity`, `AvailablePairEntity`, `SyncMetadataEntity`) | Entidades mapeadas com chaves primárias, tipos corretos e armazenamento de decimais em string. | APROVADA |
| **E02-T02** | Implementar DAOs (`CurrencyDao`, `QuoteDao`, `AvailablePairDao`, `SyncMetadataDao`) | Consultas reativas via `Flow` para moedas selecionadas, cotações, catálogo disponível, favoritos e metadados de sync. | APROVADA |
| **E02-T03** | Implementar `DolarapDatabase` com seed das 10 moedas padrão | Seed inicial insere `USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY` e `MXN` com `isDefault = true` de forma idempotente. | APROVADA |
| **E02-T04** | Criar catálogo complementar local (`ComplementaryCurrencyCatalog`) | Nomes amigáveis em PT-BR e termos de busca estruturados sem habilitar moedas não suportadas pela API. | APROVADA |
| **E02-T05** | Escrever testes locais para DAOs e seed das 10 moedas padrão | Testes verificam inserção inicial das 10 moedas, persistência de favoritos e atualização sem perda de estado. | APROVADA |

---

## E03 — Integração com a API (AwesomeAPI) *(Aprovada)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E03-T01** | Configurar Retrofit + OkHttpClient + Moshi com timeouts e logging | Cliente HTTP configurado no `networkModule` (`AppModule`) apontando para `AwesomeApiConfig.BASE_URL` (`https://economia.awesomeapi.com.br/`). | APROVADA |
| **E03-T02** | Implementar DTOs e endpoint `/json/last/{pairs}` (`AwesomeApiService`) | Chamada dinâmica para múltiplos pares (ex.: `USD-BRL,EUR-BRL`) desserializando o mapa de cotações (`QuoteDto`) preservando decimais em string. | APROVADA |
| **E03-T03** | Implementar consulta e parser do catálogo oficial `/xml/available` com tentativa experimental `/json/available` | `/xml/available` é a fonte oficial documentada; `/json/available` atua apenas como tentativa experimental com fallback XML (sem fallback em 429/5xx/timeout/offline). Parser aceita estritamente `^[A-Z]{3}-BRL$` e persiste no `AvailablePairDao` sem apagar cache em falha. | APROVADA |
| **E03-T04** | Implementar tratamento de erros HTTP e `CoinNotExists` (404) em lote | Erros de rede, timeout, HTTP 404 (`CoinNotExists` — identificando e bloqueando somente o par inexistente comprovado, preservando os demais pares válidos), 429, 5xx e payloads inválidos convertidos em `RemoteError` sem crash. | APROVADA |
| **E03-T05** | Criar `QuoteRemoteMapper` e testes unitários/MockWebServer de parsing e erro da API | 18 testes cobrem mapeamento multi-par, preservação decimal exata, regex `^[A-Z]{3}-BRL$`, fallback XML, bloqueio de fallback em 429/500, erro 404 em lote, preservação de pares válidos/cache e resolução Koin. | APROVADA |

---

## E04 — Camada de domínio e repositórios *(Aprovada)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E04-T01** | Criar modelos e erros de domínio puros (`CurrencyQuote`, `AvailableCurrency`, `ConversionResult`, `SyncStatus`, `DomainError`) | Todos os modelos em `domain/model` (`CurrencyModels.kt`, `AvailableCurrency.kt`, `ConversionResult.kt`, `DomainResult.kt`) sem imports de Android, Room, Retrofit, Moshi ou Compose. | APROVADA |
| **E04-T02** | Definir contratos `QuoteRepository`, `CatalogRepository` (e abstração de tempo `TimeProvider`) | Contratos `QuoteRepository` e `CatalogRepository` definidos no pacote `domain/repository`; `SyncScheduler` permanece reservado à Etapa E06 (WorkManager). | APROVADA |
| **E04-T03** | Implementar `CurrencyConverter` com `BigDecimal` e taxa `bid` | Cálculos `X -> BRL`, `BRL -> Y`, `X -> Y` (via BRL), mesma moeda, entrada com vírgula/ponto, escalas por moeda de destino (`HALF_UP`), `MathContext.DECIMAL128` e rejeição segura de cotação ausente/`bid <= 0` ou valor inválido. | APROVADA |
| **E04-T04** | Implementar `QuoteFreshnessEvaluator` (regra de 15 minutos) e casos de uso de cotações e favoritos | `ObserveDashboardQuotesUseCase`, `RefreshQuotesUseCase`, `ToggleFavoriteUseCase` e `ConvertCurrencyUseCase` implementados com avaliação de `QuoteFreshnessStatus` (`FRESH`, `STALE` > 15 min, `UNAVAILABLE`). | APROVADA |
| **E04-T05** | Implementar casos de uso do catálogo, pesquisa, validação de par, adição e remoção | `ObserveAvailableCurrenciesUseCase`, `SearchAvailableCurrenciesUseCase` (código/nome sem acentos), `RefreshCatalogUseCase`, `AddCurrencyUseCase` (valida `*-BRL` + melhor esforço de cotação) e `RemoveCurrencyUseCase` (protege as 10 padrão). | APROVADA |
| **E04-T06** | Implementar `QuoteRepositoryImpl` e `CatalogRepositoryImpl` (SSOT Room) + `DataModule`/`DomainModule` | Fluxo Offline First via `Flow` do Room, deduplicação de sync concorrente (`Mutex` + `CompletableDeferred`), preservação de cache em falha e registro completo no Koin (`appModules`) sem ciclos. | APROVADA |
| **E04-T07** | Criar testes unitários JUnit e de integração para domínio, repositórios e Koin | 21 testes da E04 (16 unitários puros + 4 integração Room/MockWebServer/Robolectric + 1 integração Koin) executados e aprovados em `gradle :app:testDebugUnitTest`. | APROVADA |

---

## E05 — Interface (Jetpack Compose) *(Implementação e testes concluídos — aguardando revisão externa)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E05-T01** | Configurar `DolarapNavGraph` com Navigation Compose | Navegação funcional (`DolarapApp` + `DolarapNavHost` + `navigateSingleTopTo`) com `NavigationBar`/`NavigationRail` adaptativa, preservação de estado entre abas e sem duplicação na pilha. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E05-T02** | Implementar `QuotesViewModel` e `QuotesScreen` (Tela inicial + Favoritos) | Exibe 10 padrão + adicionais com `bid`, `ask`, variação (`pctChange`/`varBid`), timestamp, filtro `Todas`/`Favoritas`, pull-to-refresh/atualização manual e remoção restrita às moedas adicionadas. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E05-T03** | Implementar banners/indicadores de estado Offline e Desatualizado (> 15 min) | `FreshnessBadge` e `StatusBannerCard` sinalizam dados `FRESH`, `STALE` (> 15 min) e `UNAVAILABLE` na Tela Inicial e no Conversor sem apagar cache local. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E05-T04** | Implementar `CurrencySearchViewModel` e `CurrencySearchScreen` | Pesquisa em tempo real por código/nome via `SearchAvailableCurrenciesUseCase`, status (`Padrão (protegida)`/`Selecionada`/`Disponível`), proteção contra duplicidade/em voo e atualização de catálogo. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E05-T05** | Implementar `ConverterViewModel` e `ConverterScreen` | Seletor com BRL + 10 padrão + adicionais, inversão rápida, entrada numérica, cálculo 100% via `ConvertCurrencyUseCase` (`BigDecimal`/`bid`), taxa complementar `ask`/cruzada e aviso de indisponibilidade/desatualização. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E05-T06** | Garantir estados `Loading`, `Success`, `Empty` e `Error` em todas as telas + testes | Todas as telas implementam estados explícitos, `testTag`s, acessibilidade, `PresentationModule` no Koin e 22 testes da E05 (ViewModels, Formatter, DI e Robolectric Compose UI/contraste) aprovados. | IMPLEMENTADA (AGUARDANDO REVISÃO) |

---

## E06 — Offline First, sincronização e validação *(Implementação e testes concluídos — aguardando revisão externa)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E06-T01** | Implementar `SyncQuotesWorker` e `WorkManagerSyncScheduler` | Worker executa atualização em background via `QuoteRepository`, trata falhas transitórias/permanentes e HTTP 429 (`Retry-After`) com `KoinWorkerFactory` e `Configuration.Provider`. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E06-T02** | Configurar agendamento periódico de 6 horas sem duplicação | `enqueueUniquePeriodicWork` (`dolarap_periodic_quotes_sync`) com `ExistingPeriodicWorkPolicy.KEEP`, `NetworkType.CONNECTED` e `BackoffPolicy.EXPONENTIAL` agendado na inicialização (`DolarapApplication`). | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E06-T03** | Configurar re-sincronização condicionada à rede após falha de conexão | `enqueueUniqueWork` (`dolarap_connectivity_retry_quotes_sync`) com `NetworkType.CONNECTED`, `BackoffPolicy.EXPONENTIAL`, política `KEEP` (ou `REPLACE` com `initialDelay` em `Retry-After`) e cancelamento em sucesso no foreground. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E06-T04** | Validar fluxo completo Offline First e expiração de 15 minutos | `QuotesViewModel`/`QuotesScreen` consolidam os 7 estados não contraditórios de sync sem ocultar dados do Room; fluxo ponta a ponta validado com atualização automática via `Flow` e aviso após 15 minutos. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E06-T05** | Executar bateria completa de testes unitários e validações manuais | 14 novos testes da E06 (92/92 no total em `:app:testDebugUnitTest`) executados e registrados em `TEST_RESULTS.md`; validações manuais em dispositivo permanecem marcadas como não executadas por ausência de ADB/emulador no container. | IMPLEMENTADA (AGUARDANDO REVISÃO) |

---

## E07 — Entrega e documentação

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E07-T01** | Atualizar `README.md`, `PLANNING.md` e `TASKS.md` para refletir o estado real implementado | Documentação sem divergências em relação ao código implementado e sem funcionalidades inventadas. | PENDENTE |
| **E07-T02** | Consolidar `TEST_RESULTS.md` com resultados finais e evidências reais | Todos os testes executados documentados com status, data e observações. | PENDENTE |
| **E07-T03** | Validar build final de APK (`compile_applet` / `assembleDebug` ou release conforme projeto) | Projeto compila sem erros e gera APK instalável. | PENDENTE |
| **E07-T04** | Preparar checklist final de entrega e versionamento GitHub | Repositório organizado, histórico limpo, sem segredos vazados e pronto para revisão final. | PENDENTE |
