# TEST_RESULTS — DOLARAP

> **Status geral:** **Etapa E07 concluída em `2026-11-17` (aguardando revisão final externa).** Etapas **E00–E06** revisadas e encerradas. Revalidação final executada via `gradle :app:testDebugUnitTest :app:assembleDebug` (**92/92 testes aprovados em 24 suítes**, 0 falhas, 0 erros, 0 ignorados) seguida de build final da plataforma (`compile_applet`) gerando `app/build/outputs/apk/debug/app-debug.apk` (`22.590.182` bytes) com `BuildConfig.AWESOME_API_KEY` real embutida no DEX. Os testes manuais `MT-01`–`MT-11` e testes instrumentados (`androidTest`) foram **dispensados pelo responsável pelo projeto** e permanecem registrados como `NÃO EXECUTADO`.

## Legenda de Status
- `NÃO EXECUTADO` — Caso de teste planejado, aguardando implementação da funcionalidade correspondente.
- `APROVADO` — Executado e validado conforme critério esperado.
- `FALHOU` — Executado com divergência em relação ao critério esperado.
- `BLOQUEADO` — Impedido de executar por dependência ou limitação externa.

---

## 1. Registro de Execuções (Resumo por Etapa)

| Etapa | Data | Executor | Ambiente / Build | Observações | Status |
|---|---|---|---|---|---|
| E02 — Banco de Dados Local | 2026-11-15 | Agente (JVM + Robolectric) | `gradle :app:testDebugUnitTest` (SDK 36) | 12/12 testes da E02 aprovados (4 unitários puros + 8 integração Room/Koin via Robolectric); etapa aprovada | APROVADO |
| E03 — Integração AwesomeAPI | 2026-11-15 | Agente (JVM + Robolectric + MockWebServer) | `gradle :app:testDebugUnitTest` (SDK 36) | 18/18 testes da E03 aprovados (7 unitários puros + 11 integração MockWebServer/Room/Koin via Robolectric); etapa aprovada | APROVADO |
| E04 — Camada de Domínio e Repositórios | 2026-11-15 | Agente (JVM + Robolectric + MockWebServer) | `gradle :app:testDebugUnitTest` (SDK 36) | 21/21 testes da E04 aprovados (16 unitários puros + 5 integração Repository/Room/MockWebServer/Koin via Robolectric); etapa aprovada | APROVADO |
| E05 — Interface Jetpack Compose | 2026-11-15 | Agente (JVM + Robolectric + Compose UI Test) | `gradle :app:testDebugUnitTest` (SDK 36) | 22/22 testes da E05 aprovados (15 unitários ViewModels/Formatter + 1 integração DI Koin + 6 UI/Navegação/Contraste Compose via Robolectric); etapa revisada e aprovada | APROVADO |
| E06 — Offline First (WorkManager) | 2026-11-16 | Agente (JVM + Robolectric + WorkManager + MockWebServer) | `gradle :app:testDebugUnitTest :app:assembleDebug` (SDK 36) | 14/14 novos testes da E06 aprovados (92/92 no total em 24 suítes); etapa revisada e encerrada | APROVADO |
| E07 — Validação Final de Entrega | 2026-11-17 | Agente (JVM + Robolectric + Build Gradle/AI Studio) | `gradle :app:testDebugUnitTest` + `compile_applet` (SDK 36) | Revalidação 92/92 aprovada; APK debug gerado (`22.590.182` bytes) com `AWESOME_API_KEY` embutida; `MT-01`–`MT-11` e `androidTest` dispensados (`NÃO EXECUTADO`) | APROVADO |

---

## 2. Testes Unitários Planejados (JUnit)

| ID | Caso de Teste | Camada / Alvo | Critério Esperado | Resultado | Data | Observações | Status |
|---|---|---|---|---|---|---|---|
| **UT-01** | Conversão direta `Moeda -> BRL` usando `bid` | `domain` / `CurrencyConverter` | Multiplica valor por `bid` com `BigDecimal` mantendo precisão e arredondamento esperado | Multiplica com `MathContext.DECIMAL128` e arredonda (`HALF_UP`) para 2 casas mantendo `ask` informativo | 2026-11-15 | Validado via `CurrencyConverterTest` | APROVADO |
| **UT-02** | Conversão inversa `BRL -> Moeda` usando `bid` | `domain` / `CurrencyConverter` | Divide valor por `bid` com `BigDecimal` sem erro de divisão ou ponto flutuante | Divide usando `bid` com precisão `DECIMAL128`, escala por moeda e taxa efetiva em 8 casas | 2026-11-15 | Validado via `CurrencyConverterTest` | APROVADO |
| **UT-03** | Conversão cruzada `Moeda A -> Moeda B` via `BRL` | `domain` / `CurrencyConverter` | Converte `A -> BRL -> B` usando os respectivos `bid` em `BigDecimal` | Triangula via BRL (`valor * bid(A) / bid(B)`) com precisão `DECIMAL128` e escala final do destino | 2026-11-15 | Validado via `CurrencyConverterTest` | APROVADO |
| **UT-04** | Conversão com mesma moeda ou valor zero | `domain` / `CurrencyConverter` | Retorna mesmo valor para moedas iguais e zero para entrada zero | Retorna taxa `1` para mesma moeda, escala zero para entrada `"0"` e suporta vírgula decimal | 2026-11-15 | Validado via `CurrencyConverterTest` | APROVADO |
| **UT-05** | Conversão com moeda sem cotação ou `bid <= 0` | `domain` / `CurrencyConverter` | Retorna estado/erro específico de cotação indisponível sem crash | Retorna `DomainError.QuoteUnavailable` (e `InvalidAmount` para valor negativo/inválido) sem crash | 2026-11-15 | Validado via `CurrencyConverterTest` e `DomainUseCasesTest` | APROVADO |
| **UT-06** | Cálculo de dados desatualizados (> 15 minutos) | `domain` / `QuoteFreshnessEvaluator` | `<= 15 min` retorna atualizado (`isStale = false`); `> 15 min` retorna `isStale = true` | `UNAVAILABLE` para timestamp ausente, `FRESH` até `900_000 ms`, `STALE` acima de 15 min ou relógio inválido | 2026-11-15 | Validado via `QuoteFreshnessEvaluatorTest` | APROVADO |
| **UT-07** | Bloqueio de remoção das 10 moedas padrão | `domain` / `RemoveCurrencyUseCase` | Recusa remoção para `USD, EUR, GBP, CAD, AUD, CHF, JPY, ARS, CNY, MXN` | Bloqueia todas as 10 moedas padrão retornando `DomainError.DefaultCurrencyProtected` | 2026-11-15 | Validado via `DomainUseCasesTest` | APROVADO |
| **UT-08** | Remoção permitida para moeda adicionada pelo usuário | `domain` / `RemoveCurrencyUseCase` | Remove moeda não padrão da persistência mantendo as demais | Remove moeda não padrão via `QuoteRepository` preservando demais selecionadas | 2026-11-15 | Validado via `DomainUseCasesTest` | APROVADO |
| **UT-09** | Validação de par suportado antes de adicionar moeda | `domain` / `AddCurrencyUseCase` e `CatalogRepositoryImpl` | Permite apenas moedas com par `*-BRL` presente no catálogo validado | Valida `^[A-Z]{3}$`, presença ativa em `available_pairs` e preserva adição se cotação falhar | 2026-11-15 | Validado via `DomainUseCasesTest` e `QuoteAndCatalogRepositoryIntegrationTest` | APROVADO |
| **UT-10** | Pesquisa de moedas por código e nome amigável | `domain` / `SearchAvailableCurrenciesUseCase` | Filtra corretamente por código (ex.: `CHF`) e nome (ex.: `Franco`), ignorando maiúsculas/acentos | Pesquisa normalizada sem acentos/maiúsculas por código, nome amigável, descrição e termos de busca | 2026-11-15 | Validado via `DomainUseCasesTest` e `QuoteAndCatalogRepositoryIntegrationTest` | APROVADO |
| **UT-11** | Alternância e persistência de favorito | `domain` / `ToggleFavoriteUseCase` e `QuoteRepositoryImpl` | Atualiza estado `isFavorite` no repositório sem alterar `isDefault` | Alterna `isFavorite` no Room preservando `isDefault` e reflete nos fluxos reativos | 2026-11-15 | Validado via `QuoteAndCatalogRepositoryIntegrationTest` | APROVADO |
| **UT-12** | Mapeamento de DTO da AwesomeAPI (`/json/last`) | `data.remote` / `QuoteRemoteMapper` | Preserva strings `bid`, `ask`, `varBid`, `pctChange`, `high`, `low` e `timestamp` sem perda decimal | Mapeia múltiplos pares mantendo decimais exatos e rejeitando DTOs inválidos | 2026-11-15 | Validado via `QuoteRemoteMapperTest` e `AwesomeApiRemoteDataSourceTest` | APROVADO |
| **UT-13** | Parser do catálogo disponível (`/available`) | `data.remote` / `AvailableCatalogParser` | Extrai corretamente pares `*-BRL` suportados e ignora entradas inválidas | Extrai apenas `*-BRL` em JSON e XML (fallback oficial) ignorando pares inválidos/não-BRL | 2026-11-15 | Validado via `AvailableCatalogParserTest` e `AwesomeApiRemoteDataSourceTest` | APROVADO |
| **UT-14** | Tratamento de erro HTTP 404 (`CoinNotExists`) e falha de rede | `data` / `QuoteRepositoryImpl` | Não apaga cotações existentes no Room e retorna erro de domínio apropriado | Preserva cotações em cache no Room, registra metadados de tentativa e retorna `DomainError` tipado | 2026-11-15 | Validado na E03 (`AwesomeApiRemoteDataSourceTest`) e E04 (`QuoteAndCatalogRepositoryIntegrationTest`) | APROVADO |
| **UT-15** | Seed inicial das 10 moedas padrão no banco | `data` / `DolarapDatabase` / DAO | Banco recém-criado contém exatamente as 10 moedas padrão com `isDefault = true` | 10 moedas padrão semeados na criação (`onCreate`) | 2026-11-15 | Validado via `DolarapDatabaseIntegrationTest` (Robolectric + Room em memória) | APROVADO |

---

## 3. Validações Manuais Planejadas

> **Registro da Etapa E07:** Os cenários manuais `MT-01` a `MT-11` abaixo (bem como os testes instrumentados `androidTest`) foram **formalmente dispensados pelo responsável pelo projeto** na etapa final (`E07`), permanecendo registrados como `NÃO EXECUTADO` sem fabricação de evidências.

| ID | Cenário Manual | Fluxo / Tela | Critério Esperado | Resultado | Data | Observações | Status |
|---|---|---|---|---|---|---|---|
| **MT-01** | Primeira abertura com internet | Tela Inicial (`QuotesScreen`) | Exibe as 10 moedas padrão com `bid`, `ask`, variação e horário da última atualização | - | - | - | NÃO EXECUTADO |
| **MT-02** | Atualização manual | Tela Inicial (`QuotesScreen`) | Aciona busca na API, atualiza valores e renova data/hora de última atualização | - | - | - | NÃO EXECUTADO |
| **MT-03** | Pesquisa e adição de moeda suportada | Tela de Pesquisa (`CurrencySearchScreen`) | Pesquisa por nome ou código, adiciona moeda válida e ela passa a aparecer na Tela Inicial | - | - | - | NÃO EXECUTADO |
| **MT-04** | Bloqueio de remoção de moeda padrão vs remoção personalizada | Tela Inicial / Pesquisa | Moedas adicionadas podem ser removidas; as 10 padrão permanecem protegidas | - | - | - | NÃO EXECUTADO |
| **MT-05** | Favoritar e reiniciar o app | Tela Inicial e Filtro Favoritos | Moedas favoritadas aparecem no filtro de favoritos e permanecem marcadas após fechar e reabrir o app | - | - | - | NÃO EXECUTADO |
| **MT-06** | Conversor de moedas (`X->BRL`, `BRL->Y`, `X->Y`) | Tela de Conversor (`ConverterScreen`) | Calcula corretamente usando `bid` e `BigDecimal` com inversão rápida de moedas | - | - | - | NÃO EXECUTADO |
| **MT-07** | Conversor sem cotação disponível | Tela de Conversor (`ConverterScreen`) | Exibe mensagem orientativa clara sem travar ou mostrar cálculo inválido | - | - | - | NÃO EXECUTADO |
| **MT-08** | Abertura do app em modo avião (Offline First) | Todas as telas | Exibe imediatamente os últimos dados salvos no Room com indicador de modo offline | - | - | - | NÃO EXECUTADO |
| **MT-09** | Sinalização de cotação desatualizada (> 15 min) | Tela Inicial / Conversor | Após 15 min sem sync bem-sucedido, exibe aviso explícito de dados desatualizados | - | - | - | NÃO EXECUTADO |
| **MT-10** | Retorno da conexão após falha de rede | Background / Tela Inicial | WorkManager executa sincronização condicionada à rede sem criar trabalhos duplicados e atualiza UI | - | - | - | NÃO EXECUTADO |
| **MT-11** | Estados `Loading`, `Success`, `Empty` e `Error` | Todas as telas | Cada estado é exibido adequadamente sem perda de dados locais | - | - | - | NÃO EXECUTADO |

---

## 4. Execução da Etapa E02 — Banco de Dados Local (Room)

Execução realizada em **2026-11-15** via `gradle :app:assembleDebug :app:testDebugUnitTest` (JUnit 4 + Coroutines + Robolectric SDK 36).

| ID | Descrição | Tipo de Teste | Arquivo de Teste | Resultado | Observações |
|---|---|---|---|---|---|
| **E02-UT-01** | Verifica as exatas 10 moedas padrão em ordem e pares `*-BRL` | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/local/ComplementaryCurrencyCatalogTest.kt` | APROVADO | Sem falhas |
| **E02-UT-02** | Verifica identificação case-insensitive das moedas padrão (`isDefaultCurrency`) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/local/ComplementaryCurrencyCatalogTest.kt` | APROVADO | Sem falhas |
| **E02-UT-03** | Verifica resolução de nome amigável PT-BR e fallbacks (`resolveFriendlyName`) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/local/ComplementaryCurrencyCatalogTest.kt` | APROVADO | Sem falhas |
| **E02-UT-04** | Verifica normalização de busca removendo acentos e convertendo para minúsculas | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/local/ComplementaryCurrencyCatalogTest.kt` | APROVADO | Sem falhas |
| **E02-IT-01** | Seed automático das 10 moedas padrão no `onCreate` do `DolarapDatabase` em memória | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Sem falhas |
| **E02-IT-02** | Idempotência de `ensureDefaultCurrenciesSeeded` preservando favoritos e moedas adicionadas | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Sem falhas |
| **E02-IT-03** | Proteção das 10 moedas padrão contra remoção e exclusão de moeda do usuário (`deleteNonDefaultByCode`) | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Sem falhas |
| **E02-IT-04** | Reatividade via `Flow` em `SelectedCurrencyWithQuote` (inserção/atualização de cotação) e favoritos | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Primeira execução apresentou corrida de coleta assíncrona no teste; ajustado para coleta sequencial via `Channel` e reexecutado com sucesso |
| **E02-IT-05** | Persistência de favoritos e moeda personalizada após fechar e reabrir o banco em arquivo | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Sem falhas |
| **E02-IT-06** | Filtro de pares `*-BRL`, busca por nome/código e alternância de disponibilidade em `AvailablePairDao` | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Sem falhas |
| **E02-IT-07** | Inserção, atualização, observação via `Flow` e limpeza em `SyncMetadataDao` | Integração Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/DolarapDatabaseIntegrationTest.kt` | APROVADO | Sem falhas |
| **E02-IT-08** | Resolução singleton de `DolarapDatabase` e dos 4 DAOs via Koin (`DolarapApplication`) | Integração DI + Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/local/KoinDatabaseModuleTest.kt` | APROVADO | Primeira execução revelou `KoinApplicationAlreadyStartedException` em múltiplos runners Robolectric; corrigido com guarda `GlobalContext.getOrNull() == null` em `DolarapApplication` e reexecutado com sucesso |
| **E02-INST-01** | Testes instrumentados em dispositivo/emulador Android (`ExampleInstrumentedTest`) | Instrumentado (`androidTest`) | `app/src/androidTest/java/com/aistudio/dolarap/ExampleInstrumentedTest.kt` | NÃO EXECUTADO | Ambiente de execução sem emulador Android ou ADB disponível |

---

## 5. Execução da Etapa E03 — Integração com a AwesomeAPI (Pós-Auditoria)

Execução realizada em **2026-11-15** via `gradle :app:assembleDebug :app:testDebugUnitTest` (JUnit 4 + Moshi + OkHttp `MockWebServer` + Coroutines + Room em memória + Robolectric SDK 36). Todos os 18 testes da E03 passaram, totalizando 32 testes aprovados no projeto (18 E03 + 12 E02 + 2 E01).

| ID | Descrição | Tipo de Teste | Arquivo de Teste | Resultado | Observações |
|---|---|---|---|---|---|
| **E03-UT-01** | Desserializa JSON de múltiplos pares (`/json/last`) preservando strings decimais exatas em `QuoteEntity` | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/QuoteRemoteMapperTest.kt` | APROVADO | Sem falhas |
| **E03-UT-02** | Rejeita com segurança `QuoteDto` incompleto, `bid <= 0`, não numérico, `codein != BRL` ou código fora de 3 letras | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/QuoteRemoteMapperTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-UT-03** | Desserializa erro estruturado `ApiErrorDto` (`404 CoinNotExists`) via Moshi | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/QuoteRemoteMapperTest.kt` | APROVADO | Sem falhas |
| **E03-UT-04** | `parseJsonToBrlPairs` aceita estritamente `^[A-Z]{3}-BRL$` com valor válido, rejeitando 2/4 letras, dígitos, turismo (`BRLT`), `BRLPTAX`, cruzados e vazios/nulos | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/AvailableCatalogParserTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-UT-05** | `parseJsonToBrlPairs` rejeita payload XML, string em branco ou JSON malformado retornando `RemoteError` tipado | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/AvailableCatalogParserTest.kt` | APROVADO | Sem falhas |
| **E03-UT-06** | `parseXmlToBrlPairs` processa fonte oficial tag-por-par (`<xml><USD-BRL>...</USD-BRL></xml>`) filtrando apenas `^[A-Z]{3}-BRL$` com descrição válida | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/AvailableCatalogParserTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-UT-07** | `parseXmlToBrlPairs` suporta `<item>` (`code`/`codein`), rejeita códigos fora de 3 letras e rejeita XML malformado | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/data/remote/AvailableCatalogParserTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-IT-01** | `fetchLastQuotes` consulta `/json/last/USD-BRL,EUR-BRL` no `MockWebServer` e retorna entidades mapeadas com decimais exatos | Integração HTTP + Parser (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Sem falhas |
| **E03-IT-02** | `fetchLastQuotes` rejeita localmente pares fora do padrão `^[A-Z]{3}-BRL$` sem disparar chamada HTTP (`requestCount == 0`) | Integração Validação (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Novo na auditoria |
| **E03-IT-03** | `fetchLastQuotes` trata HTTP 404 `CoinNotExists` em consulta em lote (`USD-BRL,XYZ-BRL,EUR-BRL`), bloqueia apenas `XYZ-BRL`, preserva pares válidos e exclui `XYZ-BRL` na chamada seguinte | Integração HTTP + Room (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-IT-04** | `fetchLastQuotes` não marca nenhum par como inválido quando 404 `CoinNotExists` em lote omite código identificável | Integração HTTP + Room (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Novo na auditoria |
| **E03-IT-05** | `fetchLastQuotes` trata HTTP 429, HTTP 500, JSON vazio `{}` e JSON malformado mapeando para `RemoteError` sem exceções | Integração HTTP (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Sem falhas |
| **E03-IT-06** | `fetchLastQuotes` mapeia timeout de socket (`SocketPolicy.NO_RESPONSE`) para `RemoteError.Timeout` e falha de conexão para `RemoteError.NoConnection` | Integração HTTP (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Sem falhas |
| **E03-IT-07** | `syncAvailableBrlCatalog` persiste tentativa experimental `/json/available` quando válida | Integração HTTP + Room (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Sem falhas |
| **E03-IT-08** | `syncAvailableBrlCatalog` aciona fallback oficial `/xml/available` quando `/json/available` retorna JSON inválido e persiste no Room | Integração HTTP + Room (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-IT-09** | `syncAvailableBrlCatalog` **não** executa fallback XML em HTTP 429 ou HTTP 500 e preserva o cache anterior no Room | Integração HTTP + Room (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Novo na auditoria |
| **E03-IT-10** | `syncAvailableBrlCatalog` preserva catálogo cacheado existente no Room quando JSON e XML retornam payloads inválidos | Integração HTTP + Room (Robolectric + MockWebServer) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Atualizado na auditoria |
| **E03-IT-11** | Koin resolve `Moshi`, `OkHttpClient`, `Retrofit` (`AwesomeApiConfig.BASE_URL`), `AwesomeApiService`, `QuoteRemoteMapper`, `AvailableCatalogParser` e singleton `AwesomeApiRemoteDataSource` | Integração DI + Rede (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/remote/KoinNetworkModuleTest.kt` | APROVADO | Sem falhas |
| **E03-INST-01** | Testes instrumentados em dispositivo/emulador Android (`ExampleInstrumentedTest`) | Instrumentado (`androidTest`) | `app/src/androidTest/java/com/aistudio/dolarap/ExampleInstrumentedTest.kt` | NÃO EXECUTADO | Ambiente de execução sem emulador Android ou ADB disponível |

---

## 6. Execução da Etapa E04 — Domain e Repositórios

Execução realizada em **2026-11-15** via `gradle :app:assembleDebug :app:testDebugUnitTest` (JUnit 4 + Coroutines Test + Room em memória + OkHttp `MockWebServer` + Koin + Robolectric SDK 36). Todos os 21 novos testes da E04 passaram, totalizando **53 testes aprovados** no projeto (21 E04 + 18 E03 + 12 E02 + 2 E01), com 0 falhas, 0 erros e 0 ignorados.

| ID | Descrição | Tipo de Teste | Arquivo de Teste | Resultado | Observações |
|---|---|---|---|---|---|
| **E04-UT-01** | Converte moeda estrangeira para `BRL` usando `bid` e preserva `ask` complementar | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-02** | Converte `BRL` para moeda estrangeira usando `bid` com arredondamento `HALF_UP` | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-03** | Converte entre duas moedas estrangeiras via `BRL` usando `bid` em `BigDecimal` | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-04** | Trata mesma moeda, entrada zero e entrada decimal com vírgula (`"100,50"`) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-05** | Aplica casas decimais por moeda de destino (`JPY` = 0 casas, `BTC` = 8 casas) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-06** | Rejeita quantias inválidas, em branco ou negativas com `DomainError.InvalidAmount` | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-07** | Rejeita cotação ausente ou `bid <= 0` com `DomainError.QuoteUnavailable` sem inventar taxa | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/CurrencyConverterTest.kt` | APROVADO | Sem falhas |
| **E04-UT-08** | `QuoteFreshnessEvaluator` retorna `UNAVAILABLE` quando não há `fetchedAtMillis` local | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/QuoteFreshnessEvaluatorTest.kt` | APROVADO | Sem falhas |
| **E04-UT-09** | `QuoteFreshnessEvaluator` retorna `FRESH` até exatamente 15 minutos (`900_000 ms`) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/QuoteFreshnessEvaluatorTest.kt` | APROVADO | Sem falhas |
| **E04-UT-10** | `QuoteFreshnessEvaluator` retorna `STALE` imediatamente após 15 minutos (`900_001 ms`) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/QuoteFreshnessEvaluatorTest.kt` | APROVADO | Sem falhas |
| **E04-UT-11** | `QuoteFreshnessEvaluator` trata relógio inválido ou timestamp futuro defensivamente como `STALE` | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/domain/service/QuoteFreshnessEvaluatorTest.kt` | APROVADO | Sem falhas |
| **E04-UT-12** | `ObserveDashboardQuotesUseCase` alterna corretamente entre todas as moedas e apenas favoritas | Unitário (JUnit + Fake) | `app/src/test/java/com/aistudio/dolarap/domain/usecase/DomainUseCasesTest.kt` | APROVADO | Sem falhas |
| **E04-UT-13** | `SearchAvailableCurrenciesUseCase` filtra ignorando maiúsculas e acentuação | Unitário (JUnit + Fake) | `app/src/test/java/com/aistudio/dolarap/domain/usecase/DomainUseCasesTest.kt` | APROVADO | Sem falhas |
| **E04-UT-14** | `RemoveCurrencyUseCase` protege todas as 10 moedas padrão e permite remover adicionais | Unitário (JUnit + Fake) | `app/src/test/java/com/aistudio/dolarap/domain/usecase/DomainUseCasesTest.kt` | APROVADO | Sem falhas |
| **E04-UT-15** | `AddCurrencyUseCase` valida par suportado, adiciona moeda e preserva adição se cotação inicial falhar | Unitário (JUnit + Fake) | `app/src/test/java/com/aistudio/dolarap/domain/usecase/DomainUseCasesTest.kt` | APROVADO | Sem falhas |
| **E04-UT-16** | `ConvertCurrencyUseCase` sinaliza `isStale = true` após 15 min e falha se faltar cotação | Unitário (JUnit + Fake) | `app/src/test/java/com/aistudio/dolarap/domain/usecase/DomainUseCasesTest.kt` | APROVADO | Sem falhas |
| **E04-IT-01** | `QuoteRepositoryImpl` observa as 10 moedas padrão via Room `Flow` mesmo antes da primeira cotação | Integração Repo + Room (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/repository/QuoteAndCatalogRepositoryIntegrationTest.kt` | APROVADO | Sem falhas |
| **E04-IT-02** | `refreshQuotes` grava resposta da API no Room, emite via `Flow` e preserva cache local em erro HTTP | Integração Repo + Room + `MockWebServer` | `app/src/test/java/com/aistudio/dolarap/data/repository/QuoteAndCatalogRepositoryIntegrationTest.kt` | APROVADO | Sem falhas |
| **E04-IT-03** | `refreshQuotes` coalescência/deduplicação garante apenas 1 requisição HTTP para chamadas concorrentes | Integração Repo + Coroutines + `MockWebServer` | `app/src/test/java/com/aistudio/dolarap/data/repository/QuoteAndCatalogRepositoryIntegrationTest.kt` | APROVADO | Sem falhas |
| **E04-IT-04** | `CatalogRepositoryImpl` sincroniza catálogo, pesquisa, valida par suportado ao adicionar e persiste favorito | Integração Repo + Room + `MockWebServer` | `app/src/test/java/com/aistudio/dolarap/data/repository/QuoteAndCatalogRepositoryIntegrationTest.kt` | APROVADO | Sem falhas |
| **E04-IT-05** | Koin resolve `dataModule` e `domainModule` (repositórios, serviços e 9 UseCases) sem dependências circulares | Integração DI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/di/KoinDomainModuleTest.kt` | APROVADO | Sem falhas |
| **E04-INST-01** | Testes instrumentados em dispositivo/emulador Android (`ExampleInstrumentedTest`) | Instrumentado (`androidTest`) | `app/src/androidTest/java/com/aistudio/dolarap/ExampleInstrumentedTest.kt` | NÃO EXECUTADO | Ambiente de execução sem emulador Android ou ADB disponível |

---

## 7. Execução da Etapa E05 — Interface com Jetpack Compose

Execução realizada em **2026-11-15** via `gradle :app:assembleDebug :app:testDebugUnitTest` (JUnit 4 + Coroutines Test + Compose UI Test + Navigation Compose + Koin + Robolectric SDK 36). Todos os 22 novos testes da E05 passaram, totalizando **75 testes aprovados** no projeto (22 E05 + 21 E04 + 18 E03 + 12 E02 + 2 E01), com 0 falhas, 0 erros e 0 ignorados.

| ID | Descrição | Tipo de Teste | Arquivo de Teste | Resultado | Observações |
|---|---|---|---|---|---|
| **E05-UT-01** | Formata taxa BRL (`bid`/`ask`) com separador decimal brasileiro e agrupamento de milhar | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/presentation/format/BrazilianFormatterTest.kt` | APROVADO | Sem falhas |
| **E05-UT-02** | Formata valores convertidos respeitando casas decimais do destino (`BRL` = 2, `JPY` = 0) | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/presentation/format/BrazilianFormatterTest.kt` | APROVADO | Sem falhas |
| **E05-UT-03** | Formata variação percentual positiva, negativa e zero | Unitário (JUnit) | `app/src/test/java/com/aistudio/dolarap/presentation/format/BrazilianFormatterTest.kt` | APROVADO | Sem falhas |
| **E05-UT-04** | `QuotesViewModel` carrega as 10 moedas padrão em estado `Success` | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-05** | `QuotesViewModel` emite estado `Empty` quando filtro `Favoritas` está ativo sem favoritos | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-06** | `QuotesViewModel` emite estado `Error` recuperável se o fluxo reativo falhar | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-07** | `QuotesViewModel` preserva lista local e exibe banner não destrutivo ao falhar atualização manual | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-08** | `QuotesViewModel` alterna favorito e popula corretamente o filtro `Favoritas` | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-09** | `QuotesViewModel` protege as 10 moedas padrão e remove moedas adicionais | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-10** | `CurrencySearchViewModel` filtra por nome/código ignorando acentos e maiúsculas e restaura lista ao limpar busca | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/search/CurrencySearchViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-11** | `CurrencySearchViewModel` adiciona moeda suportada, evita duplicidade e protege moeda padrão | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/search/CurrencySearchViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-12** | `CurrencySearchViewModel` exibe `Empty` offline para catálogo vazio e `Error` após falha de refresh | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/search/CurrencySearchViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-13** | `ConverterViewModel` inclui BRL + 10 padrão + adicionais e delega cálculo ao `ConvertCurrencyUseCase` (direta e cruzada) | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/converter/ConverterViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-14** | `ConverterViewModel` inverte origem/destino (`swapCurrencies`) e recalcula corretamente | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/converter/ConverterViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-UT-15** | `ConverterViewModel` trata entrada vazia, valor inválido/negativo, cotação indisponível e sinalização `isStale` (> 15 min) | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/converter/ConverterViewModelTest.kt` | APROVADO | Sem falhas |
| **E05-IT-01** | Koin resolve `QuotesViewModel`, `CurrencySearchViewModel` e `ConverterViewModel` via `presentationModule` | Integração DI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/di/KoinPresentationModuleTest.kt` | APROVADO | Sem falhas |
| **E05-UI-01** | `DolarapApp` navega entre Cotações, Pesquisa e Conversor sem duplicar destinos na backstack | Compose UI + Navigation (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Sem falhas |
| **E05-UI-02** | `QuotesScreen` exibe banner `STALE`, `bid`/`ask`, oculta remoção para moedas padrão, alterna favoritos e exibe `Empty` state | Compose UI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Ajustado scroll em `EmptyStateView` e teste para janelas compactas no Robolectric |
| **E05-UI-03** | `QuotesScreen` renderiza estados explícitos `Loading` e `Error` com ação de tentar novamente | Compose UI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Sem falhas |
| **E05-UI-04** | `CurrencySearchScreen` filtra em tempo real, oculta remoção para padrão e adiciona moeda adicional | Compose UI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Sem falhas |
| **E05-UI-05** | `ConverterScreen` calcula `USD -> BRL`, inverte para `BRL -> USD` e exibe aviso quando falta cotação local | Compose UI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Sem falhas |
| **E05-UI-06** | `QuotesHeaderCard` exibe textos, badge e botão com cores sólidas e contraste WCAG >= 4.5:1 / 7.0:1 nos temas claro e escuro | Compose UI + Contraste (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Adicionado na correção visual E05 |
| **E05-INST-01** | Testes instrumentados em dispositivo/emulador Android (`ExampleInstrumentedTest`) | Instrumentado (`androidTest`) | `app/src/androidTest/java/com/aistudio/dolarap/ExampleInstrumentedTest.kt` | NÃO EXECUTADO | Ambiente de execução sem emulador Android ou ADB disponível |

---

## 8. Execução da Etapa E06 — Offline First, Sincronização Automática e WorkManager

Execução realizada em **2026-11-16** via `gradle :app:testDebugUnitTest :app:assembleDebug` (JUnit 4 + Coroutines Test + Room + OkHttp `MockWebServer` + WorkManager `2.10.1` + `KoinWorkerFactory` + Robolectric SDK 36). Todos os **14 novos testes da E06** passaram, elevando o total da suíte para **92 testes aprovados em 24 suítes** (14 E06 + 3 `AwesomeApiAuthInterceptorTest` + 22 E05 + 21 E04 + 18 E03 + 12 E02 + 2 E01), com **0 falhas**, **0 erros** e **0 ignorados**.

| ID | Descrição | Tipo de Teste | Arquivo de Teste | Resultado | Observações |
|---|---|---|---|---|---|
| **E06-UT-01** | `AwesomeApiRemoteDataSource` extrai `Retry-After` em segundos inteiros e HTTP-date (compatível com API 24) no HTTP 429 | Integração HTTP (`MockWebServer` + Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/remote/AwesomeApiRemoteDataSourceTest.kt` | APROVADO | Sem falhas |
| **E06-UT-02** | `RefreshQuotesUseCase` agenda retry em falhas temporárias/rate limit (repassando `retryAfterSeconds`) e cancela retry pendente em sucesso | Unitário (JUnit + Coroutines) | `app/src/test/java/com/aistudio/dolarap/domain/usecase/DomainUseCasesTest.kt` | APROVADO | Sem falhas |
| **E06-UT-03** | `SyncQuotesWorker` delega a `QuoteRepository.refreshQuotes()`, retorna `Result.success` com timestamp e cancela retry no periódico | Unitário / Robolectric | `app/src/test/java/com/aistudio/dolarap/data/worker/SyncQuotesWorkerTest.kt` | APROVADO | Sem falhas |
| **E06-UT-04** | `SyncQuotesWorker` retorna `Result.retry()` em `NoConnection`, `Timeout` e HTTP 5xx até `MAX_RETRY_ATTEMPTS = 3`, falhando ao esgotar tentativas | Unitário / Robolectric | `app/src/test/java/com/aistudio/dolarap/data/worker/SyncQuotesWorkerTest.kt` | APROVADO | Sem falhas |
| **E06-UT-05** | `SyncQuotesWorker` falha imediatamente sem retry infinito em erros permanentes (HTTP 4xx, `UnsupportedPair`, `InvalidApiResponse`) | Unitário / Robolectric | `app/src/test/java/com/aistudio/dolarap/data/worker/SyncQuotesWorkerTest.kt` | APROVADO | Sem falhas |
| **E06-UT-06** | `SyncQuotesWorker` trata HTTP 429 com `Retry-After` agendando retry atrasado e retorna `Result.retry()` limitado quando o cabeçalho está ausente | Unitário / Robolectric | `app/src/test/java/com/aistudio/dolarap/data/worker/SyncQuotesWorkerTest.kt` | APROVADO | Sem falhas |
| **E06-UT-07** | `SyncQuotesWorker` trata lock transitório de SQLite (`SQLiteDatabaseLockedException`) com retry limitado e erro estrutural SQLite com `Result.failure()` | Unitário / Robolectric | `app/src/test/java/com/aistudio/dolarap/data/worker/SyncQuotesWorkerTest.kt` | APROVADO | Sem falhas |
| **E06-UT-08** | `QuotesViewModel` consolida sem contradições todos os 7 estados de sincronização (aguardando 1ª sync, sincronizando foreground/worker, concluído, offline, falha, local e `STALE` > 15 min) | Unitário (JUnit + Fake + Coroutines) | `app/src/test/java/com/aistudio/dolarap/presentation/quotes/QuotesViewModelTest.kt` | APROVADO | Sem falhas |
| **E06-IT-01** | `WorkManagerSyncScheduler` agenda trabalho periódico único de 6h (`KEEP`, `NetworkType.CONNECTED`) sem duplicação | Integração WorkManager (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/worker/WorkManagerSyncSchedulerTest.kt` | APROVADO | Sem falhas |
| **E06-IT-02** | `WorkManagerSyncScheduler` agenda retry único `OneTimeWork` (`KEEP`, `REPLACE` com `initialDelay` de `Retry-After`, `NetworkType.CONNECTED`) e realiza cancelamento | Integração WorkManager (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/worker/WorkManagerSyncSchedulerTest.kt` | APROVADO | Sem falhas |
| **E06-IT-03** | `WorkManagerSyncScheduler` mapeia corretamente `IDLE`, `ENQUEUED`, `RUNNING`, `BLOCKED`, `SUCCEEDED` e `FAILED` sem mascarar erros/conclusões do retry | Integração WorkManager (Robolectric) | `app/src/test/java/com/aistudio/dolarap/data/worker/WorkManagerSyncSchedulerTest.kt` | APROVADO | Sem falhas |
| **E06-IT-04** | Koin resolve `SyncScheduler` e `KoinWorkerFactory` instancia `SyncQuotesWorker` corretamente no grafo de DI | Integração DI + WorkManager (Robolectric) | `app/src/test/java/com/aistudio/dolarap/di/KoinPresentationModuleTest.kt` | APROVADO | Sem falhas |
| **E06-IT-05** | Fluxo Offline First ponta a ponta: abertura offline via Room, sync do `SyncQuotesWorker` contra `MockWebServer` atualizando a UI via `Flow`, preservação de cache em falha e sinalização `STALE` > 15 min | Integração Ponta a Ponta (Room + `MockWebServer` + Worker + `QuotesViewModel`) | `app/src/test/java/com/aistudio/dolarap/data/worker/OfflineFirstAndSyncFlowIntegrationTest.kt` | APROVADO | Sem falhas |
| **E06-UI-01** | `QuotesScreen` exibe banners não destrutivos `quotes_offline_banner` e `quotes_sync_error_banner` mantendo visíveis os cards locais do Room | Compose UI (Robolectric) | `app/src/test/java/com/aistudio/dolarap/presentation/DolarapUiAndNavigationTest.kt` | APROVADO | Sem falhas |
| **E06-INST-01** | Testes instrumentados em dispositivo/emulador Android (`ExampleInstrumentedTest`) e testes manuais `MT-01`–`MT-11` em emulador | Instrumentado (`androidTest`) / Manual | `app/src/androidTest/java/com/aistudio/dolarap/ExampleInstrumentedTest.kt` | NÃO EXECUTADO | Ambiente de container sem emulador Android interativo ou ADB acessível |

---

## 9. Execução da Etapa E07 — Validação Final de Entrega, APK e Segurança

Validação final executada em **2026-11-17** após encerramento das etapas E00–E06 e inspeção completa de arquitetura, autenticação, `.gitignore` e geração do APK de entrega.

| ID | Verificação / Tarefa | Método / Comando | Resultado Observado | Status |
|---|---|---|---|---|
| **E07-VER-01** | Reexecução integral da suíte unitária/integração/UI Compose | `gradle :app:testDebugUnitTest` | **92/92 testes aprovados** em 24 suítes XML (`0` falhas, `0` erros, `0` ignorados) | APROVADO |
| **E07-VER-02** | Compilação Gradle Debug + build da plataforma com injeção de Secrets | `gradle :app:assembleDebug` + `compile_applet` | Build verde gerando `app/build/outputs/apk/debug/app-debug.apk` (`22.590.182` bytes / `21,54 MiB`) e `.build-outputs/app-debug.apk` (`22.590.182` bytes) | APROVADO |
| **E07-VER-03** | Verificação de incorporação da `AWESOME_API_KEY` no `BuildConfig` e DEX do APK (sem expor a chave) | Inspeção automatizada em `BuildConfig.java` e nos arquivos `.dex` do APK | `AWESOME_API_KEY` com 64 caracteres (não placeholder) embutida no `BuildConfig` e presente no DEX do APK; `.env` removido automaticamente após o build da plataforma; `AwesomeApiAuthInterceptor` integrado ao `OkHttpClient` | APROVADO |
| **E07-VER-04** | Verificação de isolamento da camada `domain` | `grep` por imports de Android/Room/Retrofit/OkHttp/Moshi em `domain/` | `CLEAN_DOMAIN` (0 violações) | APROVADO |
| **E07-INST-01** | Testes instrumentados (`androidTest`) e manuais `MT-01`–`MT-11` | - | **Dispensados pelo responsável pelo projeto** (e sem emulador/ADB no container) | NÃO EXECUTADO |



