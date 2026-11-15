# TEST_RESULTS — DOLARAP

> **Status geral:** Etapa E02 executada em `2026-11-15` (`gradle :app:assembleDebug :app:testDebugUnitTest`). Os registros planejados da Etapa E00 (`UT-01`–`UT-14` e `MT-01`–`MT-11`) permanecem preservados como `NÃO EXECUTADO`. Testes instrumentados em dispositivo/emulador (`androidTest`) não foram executados por ausência de emulador/ADB neste ambiente.

## Legenda de Status
- `NÃO EXECUTADO` — Caso de teste planejado, aguardando implementação da funcionalidade correspondente.
- `APROVADO` — Executado e validado conforme critério esperado.
- `FALHOU` — Executado com divergência em relação ao critério esperado.
- `BLOQUEADO` — Impedido de executar por dependência ou limitação externa.

---

## 1. Registro de Execuções (Resumo por Etapa)

| Etapa | Data | Executor | Ambiente / Build | Observações | Status |
|---|---|---|---|---|---|
| E02 — Banco de Dados Local | 2026-11-15 | Agente (JVM + Robolectric) | `gradle :app:testDebugUnitTest` (SDK 36) | 12/12 testes da E02 aprovados (4 unitários puros + 8 integração Room/Koin via Robolectric) | APROVADO |
| E03 — Integração AwesomeAPI | - | - | - | Aguardando implementação | NÃO EXECUTADO |
| E04 — Camada de Domínio | - | - | - | Aguardando implementação | NÃO EXECUTADO |
| E05/E06 — Interface e Offline First | - | - | - | Aguardando implementação | NÃO EXECUTADO |
| E07 — Validação Final de Entrega | - | - | - | Aguardando implementação | NÃO EXECUTADO |

---

## 2. Testes Unitários Planejados (JUnit)

| ID | Caso de Teste | Camada / Alvo | Critério Esperado | Resultado | Data | Observações | Status |
|---|---|---|---|---|---|---|---|
| **UT-01** | Conversão direta `Moeda -> BRL` usando `bid` | `domain` / `CurrencyConverter` | Multiplica valor por `bid` com `BigDecimal` mantendo precisão e arredondamento esperado | - | - | - | NÃO EXECUTADO |
| **UT-02** | Conversão inversa `BRL -> Moeda` usando `bid` | `domain` / `CurrencyConverter` | Divide valor por `bid` com `BigDecimal` sem erro de divisão ou ponto flutuante | - | - | - | NÃO EXECUTADO |
| **UT-03** | Conversão cruzada `Moeda A -> Moeda B` via `BRL` | `domain` / `CurrencyConverter` | Converte `A -> BRL -> B` usando os respectivos `bid` em `BigDecimal` | - | - | - | NÃO EXECUTADO |
| **UT-04** | Conversão com mesma moeda ou valor zero | `domain` / `CurrencyConverter` | Retorna mesmo valor para moedas iguais e zero para entrada zero | - | - | - | NÃO EXECUTADO |
| **UT-05** | Conversão com moeda sem cotação ou `bid <= 0` | `domain` / `CurrencyConverter` | Retorna estado/erro específico de cotação indisponível sem crash | - | - | - | NÃO EXECUTADO |
| **UT-06** | Cálculo de dados desatualizados (> 15 minutos) | `domain` / `SyncStatus` | `<= 15 min` retorna atualizado (`isStale = false`); `> 15 min` retorna `isStale = true` | - | - | - | NÃO EXECUTADO |
| **UT-07** | Bloqueio de remoção das 10 moedas padrão | `domain` / `RemoveCurrencyUseCase` | Recusa remoção para `USD, EUR, GBP, CAD, AUD, CHF, JPY, ARS, CNY, MXN` | - | - | - | NÃO EXECUTADO |
| **UT-08** | Remoção permitida para moeda adicionada pelo usuário | `domain` / `RemoveCurrencyUseCase` | Remove moeda não padrão da persistência mantendo as demais | - | - | - | NÃO EXECUTADO |
| **UT-09** | Validação de par suportado antes de adicionar moeda | `domain` / `AddCurrencyUseCase` | Permite apenas moedas com par `*-BRL` presente no catálogo validado | - | - | - | NÃO EXECUTADO |
| **UT-10** | Pesquisa de moedas por código e nome amigável | `domain` / `SearchAvailableCurrenciesUseCase` | Filtra corretamente por código (ex.: `CHF`) e nome (ex.: `Franco`), ignorando maiúsculas/acentos | - | - | - | NÃO EXECUTADO |
| **UT-11** | Alternância e persistência de favorito | `domain` / `ToggleFavoriteUseCase` | Atualiza estado `isFavorite` no repositório sem alterar `isDefault` | - | - | - | NÃO EXECUTADO |
| **UT-12** | Mapeamento de DTO da AwesomeAPI (`/json/last`) | `data` / `QuoteMapper` | Converte strings `bid`, `ask`, `varBid`, `pctChange` e `timestamp` sem perda decimal | - | - | - | NÃO EXECUTADO |
| **UT-13** | Parser do catálogo disponível (`/available`) | `data` / `AvailableCatalogParser` | Extrai corretamente pares `*-BRL` suportados e ignora entradas inválidas | - | - | - | NÃO EXECUTADO |
| **UT-14** | Tratamento de erro HTTP 404 (`CoinNotExists`) e falha de rede | `data` / `QuoteRepositoryImpl` | Não apaga cotações existentes no Room e retorna erro de domínio apropriado | - | - | - | NÃO EXECUTADO |
| **UT-15** | Seed inicial das 10 moedas padrão no banco | `data` / `DolarapDatabase` / DAO | Banco recém-criado contém exatamente as 10 moedas padrão com `isDefault = true` | 10 moedas padrão semeados na criação (`onCreate`) | 2026-11-15 | Validado via `DolarapDatabaseIntegrationTest` (Robolectric + Room em memória) | APROVADO |

---

## 3. Validações Manuais Planejadas

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

