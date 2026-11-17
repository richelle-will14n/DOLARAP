# SPECIFICATION — DOLARAP

> **Status do documento:** Especificação base congelada na Etapa 0 (E00) e preservada como referência dos requisitos do projeto. **Estado final (E07):** Etapas **E00–E06** implementadas, testadas (**92/92 testes unitários/Robolectric aprovados**) e encerradas; Etapa **E07** (revisão final, APK debug com API Key via Secrets Gradle Plugin e documentação consolidada) concluída e aguardando revisão final externa. Validações manuais (`MT-01`–`MT-11`) e testes instrumentados (`androidTest`) foram **dispensados / não executados**.

## 1. Visão Geral

### 1.1. Descrição e Objetivo
O **DOLARAP** é um aplicativo Android nativo, desenvolvido em Kotlin e Jetpack Compose, dedicado à consulta e conversão de cotações de moedas estrangeiras em relação ao Real brasileiro (**BRL**).

Seu objetivo principal é oferecer ao usuário uma ferramenta rápida, confiável e resiliente a falhas de conexão (*Offline First*) para acompanhar taxas de câmbio (compra, venda e variação), gerenciar uma lista personalizada de moedas favoritas ou monitoradas e realizar conversões monetárias precisas com `BigDecimal`.

### 1.2. Problema que Resolve
Usuários que acompanham moedas estrangeiras (viajantes, compradores internacionais, freelancers que recebem do exterior e profissionais que lidam com câmbio) frequentemente enfrentam:
- Aplicativos que param de funcionar quando o dispositivo está sem internet ou em áreas com sinal instável.
- Falta de clareza sobre a idade dos dados exibidos (cotações desatualizadas apresentadas como se fossem atuais).
- Conversores que não diferenciam claramente taxa de compra (*bid*) ou sofrem com imprecisões de ponto flutuante.
- Necessidade de consultar rapidamente um conjunto fixo de moedas principais, mas também poder pesquisar e adicionar outras moedas suportadas sem poluir a tela inicial.

### 1.3. Público-Alvo
- Viajantes brasileiros que acompanham moedas de destino mesmo sem internet durante deslocamentos.
- Consumidores que realizam compras internacionais e precisam estimar valores em BRL.
- Profissionais autônomos e prestadores de serviço que acompanham cotações diárias (USD, EUR, GBP etc.).
- Usuários gerais que buscam um conversor simples, direto e sem necessidade de login ou criação de conta.

---

## 2. Escopo Funcional

### 2.1. Funcionalidades Incluídas
1. **Consulta de Cotações em Relação ao BRL**
   - Exibição da lista de moedas selecionadas pelo usuário.
   - Exibição da cotação de compra (`bid`), venda (`ask`) e variação (`varBid` / `pctChange`).
   - Exibição de data e hora da última atualização bem-sucedida.
   - Atualização manual sob demanda.
   - Integração com a AwesomeAPI como fonte externa de cotações.

2. **Moedas Padrão, Catálogo e Pesquisa de Moedas**
   - Inicialização automática com as **10 moedas padrão** em relação ao BRL:
     `USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY` e `MXN`.
   - Consulta ao catálogo de moedas/pares disponíveis na AwesomeAPI (com catálogo complementar de nomes amigáveis).
   - Pesquisa de moedas por nome ou código (ex.: `"Dólar"` ou `"USD"`).
   - Adição de novas moedas à lista pessoal do usuário, somente após validação de que o par é suportado pela API.
   - Remoção de moedas adicionadas manualmente pelo usuário, preservando sempre as 10 moedas padrão.
   - Persistência local da lista selecionada no Room.

3. **Conversor de Moedas**
   - Entrada de valor monetário numérico.
   - Seleção de moeda de origem e moeda de destino (incluindo BRL e moedas disponíveis/selecionadas).
   - Cálculo utilizando `BigDecimal` com escala e arredondamento controlados.
   - Uso do preço de compra (`bid`) como taxa padrão de cálculo.
   - Tratamento claro quando uma das moedas não possuir cotação disponível localmente.

4. **Favoritos**
   - Marcação e desmarcação de moedas como favoritas.
   - Persistência dos favoritos no banco local Room.
   - Preservação do estado de favorito após fechar e reabrir o aplicativo.
   - Filtro ou área dedicada para visualização rápida de favoritos.

5. **Funcionamento Offline First**
   - Banco Room como fonte única da verdade (*Single Source of Truth*) para a UI.
   - Observação reativa dos dados locais via `Flow`.
   - Exibição imediata das últimas cotações armazenadas ao abrir o app.
   - Busca de atualizações na AwesomeAPI quando houver conectividade.
   - Persistência automática das novas cotações no Room e atualização reativa da interface.
   - Manutenção integral dos dados locais quando não houver internet.
   - Sinalização explícita quando os dados estiverem em modo offline ou **desatualizados (após 15 minutos da última atualização bem-sucedida)**.
   - Registro da data e hora da última atualização bem-sucedida.
   - Re-sincronização automática quando a conexão retornar.

6. **Sincronização em Primeiro e Segundo Plano**
   - Atualização ao abrir o aplicativo.
   - Atualização manual acionada pelo usuário.
   - Trabalho periódico em segundo plano com **WorkManager** a cada **6 horas**.
   - Trabalho de sincronização único condicionado à disponibilidade de rede (`NetworkType.CONNECTED`) após falhas relacionadas à conexão.
   - Prevenção de trabalhos duplicados com políticas de trabalho único do WorkManager.
   - Tratamento de falhas sem apagar ou corromper os dados locais existentes.

### 2.2. Funcionalidades Fora do Escopo
O DOLARAP **NÃO** incluirá na versão atual:
- Login, cadastro ou autenticação de usuários.
- Integração bancária ou Open Finance.
- Pagamentos, remessas ou transferências.
- Compra ou venda real de moedas.
- Carteira de investimentos ou controle de portfólio.
- Gráficos históricos avançados de séries temporais.
- Alertas e notificações push/locais de variação de preços.
- Servidor backend próprio.
- Arquitetura com múltiplos módulos Gradle (o projeto utilizará exclusivamente o módulo `:app`).

> **Nota sobre sugestões futuras:** Eventuais melhorias (como gráficos históricos ou alertas de preço) poderão ser registradas apenas como sugestões futuras fora do escopo atual.

---

## 3. Requisitos Funcionais (RF)

| ID | Requisito Funcional | Descrição |
|---|---|---|
| **RF01** | Exibição da lista de cotações | O sistema deve exibir as moedas selecionadas pelo usuário com código, nome amigável, cotação de compra (`bid`), cotação de venda (`ask`) e variação (`varBid` / `pctChange`). |
| **RF02** | Exibição de última atualização | O sistema deve exibir a data e a hora da última atualização bem-sucedida das cotações. |
| **RF03** | Atualização manual | O sistema deve permitir que o usuário dispare manualmente uma atualização das cotações da sua lista. |
| **RF04** | Carga inicial das 10 moedas padrão | Na primeira inicialização, o sistema deve pré-popular no banco local as 10 moedas padrão em relação ao BRL: `USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY` e `MXN`. |
| **RF05** | Consulta ao catálogo da AwesomeAPI | O sistema deve consultar e armazenar localmente o catálogo de pares/moedas disponíveis na AwesomeAPI para validar suporte antes de permitir adições. |
| **RF06** | Pesquisa de moedas | O sistema deve permitir pesquisar moedas do catálogo por código (ex.: `CHF`) ou nome amigável (ex.: `Franco Suíço`). |
| **RF07** | Adição de moeda à lista pessoal | O sistema deve permitir adicionar uma moeda suportada (par `XXX-BRL` válido) à lista pessoal persistida no Room e atualizar sua cotação. |
| **RF08** | Remoção de moeda personalizada | O sistema deve permitir remover moedas adicionadas pelo usuário, impedindo a remoção das 10 moedas padrão. |
| **RF09** | Gestão de favoritos | O sistema deve permitir marcar e desmarcar qualquer moeda da lista como favorita, persistindo essa escolha no Room. |
| **RF10** | Filtro/área de favoritos | O sistema deve oferecer uma área ou filtro dedicado para listar apenas as moedas marcadas como favoritas. |
| **RF11** | Conversor de moedas | O sistema deve permitir informar um valor e selecionar moeda de origem e moeda de destino, exibindo o resultado convertido calculado com `BigDecimal` usando `bid` como taxa padrão. |
| **RF12** | Conversor com cotação indisponível | O sistema deve informar claramente ao usuário quando a moeda de origem ou destino selecionada no conversor não possuir cotação disponível no banco local. |
| **RF13** | Exibição imediata Offline First | Ao abrir o aplicativo, a interface deve exibir imediatamente os dados já persistidos no Room via `Flow`, mesmo sem conexão com a internet. |
| **RF14** | Sinalização de dados desatualizados e offline | O sistema deve indicar visualmente quando os dados exibidos estiverem desatualizados (idade superior a 15 minutos) ou quando o último sync falhou/estiver offline. |
| **RF15** | Sincronização automática na abertura | Ao abrir a tela principal, o sistema deve tentar atualizar as cotações na AwesomeAPI caso haja conexão disponível. |
| **RF16** | Sincronização periódica em segundo plano | O sistema deve agendar trabalho periódico no WorkManager com intervalo de 6 horas para atualizar as cotações em segundo plano. |
| **RF17** | Re-sincronização ao retornar conexão | Quando ocorrer falha de sincronização por ausência de conexão, o sistema deve agendar trabalho único no WorkManager condicionado à rede conectada (`CONNECTED`), evitando trabalhos duplicados. |
| **RF18** | Estados de interface | Todas as telas principais devem tratar de forma explícita os estados `Loading`, `Success`, `Empty` e `Error` sem apagar dados locais válidos em caso de erro de rede. |

---

## 4. Requisitos Não Funcionais (RNF)

| ID | Categoria | Requisito Não Funcional |
|---|---|---|
| **RNF01** | Arquitetura | O projeto deve seguir Clean Architecture com MVVM em um único módulo Gradle (`:app`), dividido nos pacotes `presentation`, `domain`, `data` e `di`. |
| **RNF02** | Isolamento de Domínio | A camada `domain` não deve possuir dependências de Android SDK, Jetpack Compose, Retrofit, OkHttp, Room, WorkManager ou da camada `presentation`. |
| **RNF03** | Interface | A interface do usuário deve ser construída 100% em Jetpack Compose com Material Design 3 e navegação via Navigation Compose. |
| **RNF04** | Precisão Monetária | Todos os cálculos de conversão e formatação numérica monetária devem utilizar `java.math.BigDecimal`, evitando `Float` ou `Double`. |
| **RNF05** | Persistência Local | Toda persistência estruturada (moedas selecionadas, favoritos, cotações, catálogo de pares suportados e timestamp de atualização) deve utilizar Room como fonte única da verdade. |
| **RNF06** | Concorrência e Reatividade | Operações assíncronas devem usar Kotlin Coroutines, com exposição reativa de estado via `Flow` e `StateFlow`. |
| **RNF07** | Injeção de Dependência | As dependências devem ser gerenciadas com Koin. |
| **RNF08** | Resiliência a Falhas | Erros HTTP, timeouts, respostas malformadas ou ausência de rede não podem causar crash nem apagar cotações previamente salvas no Room. |
| **RNF09** | Proteção contra Bloqueio da API | O aplicativo não deve enviar requisições `/json/last/` para pares não validados no catálogo, prevenindo erros HTTP 404 recorrentes que causam bloqueio temporário de IP na AwesomeAPI. |
| **RNF10** | Limitações do Background | A sincronização periódica com WorkManager deve respeitar restrições de bateria e políticas do sistema Android (Doze/App Standby), sem prometer execução em horário exato. |
| **RNF11** | Testabilidade | Regras de negócio, casos de uso, cálculos com `BigDecimal`, lógica de expiração (15 minutos) e mapeadores devem possuir testes unitários em JUnit. |

---

## 5. Regras de Negócio (RN)

- **RN01 — Moedas padrão imutáveis na remoção:** As 10 moedas padrão (`USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY`, `MXN`) devem existir desde a primeira execução e não podem ser removidas pelo usuário (apenas moedas adicionadas pelo usuário podem ser removidas).
- **RN02 — Validação obrigatória de suporte do par:** Uma moeda só pode ser adicionada à lista do usuário se o par correspondente em relação ao BRL (`{CODIGO}-BRL`) constar como suportado no catálogo da AwesomeAPI (ou validado com sucesso na API sem erro 404 `CoinNotExists`).
- **RN03 — Paridade com BRL e taxa padrão `bid`:** Todas as cotações exibidas na lista principal referem-se ao par `{MOEDA}-BRL`. No conversor, a taxa padrão utilizada para cálculo é o campo `bid` (compra).
- **RN04 — Regra de conversão entre moedas:**
  - Quando a origem é uma moeda `X` e o destino é `BRL`: `valorConvertido = valor * bid(X-BRL)`.
  - Quando a origem é `BRL` e o destino é uma moeda `Y`: `valorConvertido = valor / bid(Y-BRL)` (validando `bid > 0`).
  - Quando origem `X` e destino `Y` são ambas diferentes de `BRL`: realiza-se conversão cruzada via `BRL` (`valor * bid(X-BRL) / bid(Y-BRL)`), desde que ambas possuam cotação `bid > 0` disponível localmente.
  - Se origem e destino forem iguais: retorna o próprio valor informado.
- **RN05 — Regra de obsolescência (Stale Data):** Uma cotação é considerada **desatualizada** quando decorridos mais de **15 minutos** desde o instante da última atualização bem-sucedida (`now - lastSuccessfulSyncMillis > 15 * 60 * 1000`).
- **RN06 — Persistência independente de favoritos:** O status de favorito de uma moeda permanece salvo mesmo após encerrar e reabrir o aplicativo e independe de a moeda ser padrão ou adicionada pelo usuário.
- **RN07 — Preservação de dados em falha:** Falhas de rede ou da API jamais devem limpar as cotações já persistidas no Room; o app deve manter os últimos dados válidos e sinalizar a falha/obsolescência.
- **RN08 — Prevenção de trabalhos duplicados:** O agendamento periódico (6 horas) e o agendamento condicional à rede (`CONNECTED`) devem usar identificadores únicos (`enqueueUniquePeriodicWork` e `enqueueUniqueWork`) no WorkManager.

---

## 6. Restrições Técnicas

- Módulo único Gradle: `:app`.
- Pacotes principais obrigatórios dentro do namespace da aplicação: `presentation`, `domain`, `data`, `di`.
- Sem uso de banco remoto, backend próprio ou autenticação.
- Sem bibliotecas pagas ou chaves privadas hardcoded no código.
- Compatibilidade com as configurações Android do projeto (`compileSdk`, `minSdk 24`, `targetSdk 36`).
- Acesso de rede apenas com permissão `INTERNET` e `ACCESS_NETWORK_STATE` declaradas no `AndroidManifest.xml`.

---

## 7. Critérios de Aceitação Gerais

1. Ao abrir o app pela primeira vez **com internet**, as 10 moedas padrão são exibidas com `bid`, `ask`, variação e horário de atualização.
2. Ao fechar e reabrir o app **sem internet**, as cotações salvas aparecem imediatamente e, após 15 minutos da última sincronização bem-sucedida, a interface sinaliza claramente que os dados estão desatualizados.
3. Na tela de pesquisa, o usuário consegue buscar por código ou nome, visualizar se o par é suportado, adicionar uma nova moeda suportada e removê-la depois, mantendo as 10 moedas padrão protegidas contra remoção.
4. Ao marcar moedas como favoritas e reiniciar o app, os favoritos continuam preservados e acessíveis pelo filtro/área de favoritos.
5. No conversor, cálculos entre `USD -> BRL`, `BRL -> EUR` e `USD -> EUR` utilizam `BigDecimal` com taxa `bid`, sem erros de arredondamento de ponto flutuante, e tratam graciosamente moedas sem cotação disponível.
6. Em falha de rede, os dados anteriores permanecem visíveis, é exibido aviso apropriado e o WorkManager agenda sincronização para quando a conectividade retornar, sem criar trabalhos duplicados.

---

## 8. Matriz de Rastreabilidade de Requisitos

| Requisito | Regra(s) | Camada / Componente Principal | Tela(s) | Etapa(s) |
|---|---|---|---|---|
| RF01, RF02, RF03 | RN03, RN07 | `QuoteRepository`, `GetQuotesUseCase`, `RefreshQuotesUseCase`, `HomeViewModel` | Tela Inicial de Cotações | E02, E03, E04, E05 |
| RF04 | RN01 | `DolarapDatabase` (seed/init), `CurrencySelectionDao` | Tela Inicial de Cotações | E02, E04 |
| RF05, RF06, RF07, RF08 | RN01, RN02 | `AwesomeApiService`, `CatalogRepository`, `SearchCurrenciesUseCase`, `AddCurrencyUseCase`, `RemoveCurrencyUseCase` | Tela de Pesquisa e Adição | E02, E03, E04, E05 |
| RF09, RF10 | RN06 | `CurrencySelectionDao`, `ToggleFavoriteUseCase`, `HomeViewModel` | Tela Inicial / Área de Favoritos | E02, E04, E05 |
| RF11, RF12 | RN03, RN04 | `ConvertCurrencyUseCase`, `ConverterViewModel` | Tela de Conversor | E04, E05 |
| RF13, RF14, RF15 | RN05, RN07 | `QuoteRepository`, `ObserveQuotesWithStatusUseCase`, `HomeViewModel` | Tela Inicial e Conversor | E02, E04, E05, E06 |
| RF16, RF17 | RN08 | `SyncQuotesWorker`, `WorkManagerSyncScheduler` | Background / Todas | E06 |
| RF18 | RN07 | `UiState` / `ViewModels` / Composables | Todas as Telas | E05, E06 |
