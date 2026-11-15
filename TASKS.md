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

## E03 — Integração com a API (AwesomeAPI) *(Implementação e testes concluídos — aguardando revisão externa)*

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E03-T01** | Configurar Retrofit + OkHttpClient + Moshi com timeouts e logging | Cliente HTTP configurado no `networkModule` (`AppModule`) apontando para `AwesomeApiConfig.BASE_URL` (`https://economia.awesomeapi.com.br/`). | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E03-T02** | Implementar DTOs e endpoint `/json/last/{pairs}` (`AwesomeApiService`) | Chamada dinâmica para múltiplos pares (ex.: `USD-BRL,EUR-BRL`) desserializando o mapa de cotações (`QuoteDto`) preservando decimais em string. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E03-T03** | Implementar consulta e parser do catálogo oficial `/xml/available` com tentativa experimental `/json/available` | `/xml/available` é a fonte oficial documentada; `/json/available` atua apenas como tentativa experimental com fallback XML (sem fallback em 429/5xx/timeout/offline). Parser aceita estritamente `^[A-Z]{3}-BRL$` e persiste no `AvailablePairDao` sem apagar cache em falha. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E03-T04** | Implementar tratamento de erros HTTP e `CoinNotExists` (404) em lote | Erros de rede, timeout, HTTP 404 (`CoinNotExists` — identificando e bloqueando somente o par inexistente comprovado, preservando os demais pares válidos), 429, 5xx e payloads inválidos convertidos em `RemoteError` sem crash. | IMPLEMENTADA (AGUARDANDO REVISÃO) |
| **E03-T05** | Criar `QuoteRemoteMapper` e testes unitários/MockWebServer de parsing e erro da API | 18 testes cobrem mapeamento multi-par, preservação decimal exata, regex `^[A-Z]{3}-BRL$`, fallback XML, bloqueio de fallback em 429/500, erro 404 em lote, preservação de pares válidos/cache e resolução Koin. | IMPLEMENTADA (AGUARDANDO REVISÃO) |

---

## E04 — Camada de domínio e repositórios

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E04-T01** | Criar modelos e erros de domínio puros (`CurrencyQuote`, `AvailableCurrency`, `ConversionResult`, `SyncStatus`, `DomainError`) | Nenhum arquivo da camada `domain` importa Android, Room, Retrofit ou Compose. | PENDENTE |
| **E04-T02** | Definir contratos `QuoteRepository`, `CatalogRepository` e `SyncScheduler` | Interfaces claras e desacopladas definidas no pacote `domain`. | PENDENTE |
| **E04-T03** | Implementar `CurrencyConverter` com `BigDecimal` e taxa `bid` | Cálculos `X -> BRL`, `BRL -> Y`, `X -> Y` (via BRL) e tratamento de cotação ausente implementados com `BigDecimal`. | PENDENTE |
| **E04-T04** | Implementar casos de uso de cotações, favoritos e regra de 15 minutos | `ObserveDashboardQuotesUseCase`, `RefreshQuotesUseCase` e `ToggleFavoriteUseCase` implementados com cálculo de `isStale` (> 15 min). | PENDENTE |
| **E04-T05** | Implementar casos de uso do catálogo, pesquisa, validação de par, adição e remoção | `SearchAvailableCurrenciesUseCase` (nome e código), `AddCurrencyUseCase` (valida par suportado) e `RemoveCurrencyUseCase` (preserva as 10 padrão). | PENDENTE |
| **E04-T06** | Implementar `QuoteRepositoryImpl` e `CatalogRepositoryImpl` (SSOT Room) | Fluxo Offline First implementado garantindo que falhas de API não apaguem dados locais e registrem timestamp de sucesso. | PENDENTE |
| **E04-T07** | Criar testes unitários JUnit para toda a camada de domínio e repositórios | Testes unitários para conversor `BigDecimal`, regra de 15 min, preservação das 10 padrão, validação de pares, pesquisa e favoritos. | PENDENTE |

---

## E05 — Interface (Jetpack Compose)

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E05-T01** | Configurar `DolarapNavGraph` com Navigation Compose | Navegação funcional entre Tela Inicial de Cotações, Pesquisa/Adição de Moedas e Conversor. | PENDENTE |
| **E05-T02** | Implementar `QuotesViewModel` e `QuotesScreen` (Tela inicial + Favoritos) | Exibe lista com `bid`, `ask`, variação, data/hora da última atualização, filtro de favoritos, atualização manual e ação de remoção apenas para moedas do usuário. | PENDENTE |
| **E05-T03** | Implementar banners/indicadores de estado Offline e Desatualizado (> 15 min) | UI indica de forma clara quando exibe dados em cache offline ou com mais de 15 minutos desde o último sync bem-sucedido. | PENDENTE |
| **E05-T04** | Implementar `CurrencySearchViewModel` e `CurrencySearchScreen` | Pesquisa em tempo real por nome e código no catálogo validado, exibição de status (padrão/adicionada/disponível) e ações de adicionar/remover. | PENDENTE |
| **E05-T05** | Implementar `ConverterViewModel` e `ConverterScreen` | Entrada numérica, seleção de origem/destino, inversão de moedas, cálculo em `BigDecimal` com `bid` e mensagem amigável quando não houver cotação disponível. | PENDENTE |
| **E05-T06** | Garantir estados `Loading`, `Success`, `Empty` e `Error` em todas as telas | Todas as telas renderizam estados de carregamento, vazio, erro recuperável e sucesso mantendo acessibilidade e `testTag`s. | PENDENTE |

---

## E06 — Offline First, sincronização e validação

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E06-T01** | Implementar `SyncQuotesWorker` e `WorkManagerSyncScheduler` | Worker executa atualização em background usando os repositórios sem duplicar lógica. | PENDENTE |
| **E06-T02** | Configurar agendamento periódico de 6 horas sem duplicação | `enqueueUniquePeriodicWork` com `ExistingPeriodicWorkPolicy.KEEP` agendado na inicialização do app. | PENDENTE |
| **E06-T03** | Configurar re-sincronização condicionada à rede após falha de conexão | `enqueueUniqueWork` com `NetworkType.CONNECTED` agendado ao falhar por falta de internet, evitando duplicidade. | PENDENTE |
| **E06-T04** | Validar fluxo completo Offline First e expiração de 15 minutos | Verificado que dados aparecem imediatamente offline, não são apagados em falhas e exibem aviso após 15 minutos. | PENDENTE |
| **E06-T05** | Executar bateria completa de testes unitários e validações manuais | Todos os casos de `TEST_RESULTS.md` executados e registrados com resultado real, data e observações. | PENDENTE |

---

## E07 — Entrega e documentação

| ID | Tarefa | Critério de Conclusão | Status |
|---|---|---|---|
| **E07-T01** | Atualizar `README.md`, `PLANNING.md` e `TASKS.md` para refletir o estado real implementado | Documentação sem divergências em relação ao código implementado e sem funcionalidades inventadas. | PENDENTE |
| **E07-T02** | Consolidar `TEST_RESULTS.md` com resultados finais e evidências reais | Todos os testes executados documentados com status, data e observações. | PENDENTE |
| **E07-T03** | Validar build final de APK (`compile_applet` / `assembleDebug` ou release conforme projeto) | Projeto compila sem erros e gera APK instalável. | PENDENTE |
| **E07-T04** | Preparar checklist final de entrega e versionamento GitHub | Repositório organizado, histórico limpo, sem segredos vazados e pronto para revisão final. | PENDENTE |
