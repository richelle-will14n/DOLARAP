# DOLARAP

> **Estado atual do projeto:** Etapa 0 — Especificação e Planejamento concluídos. **Nenhuma funcionalidade foi implementada até o momento.** Todas as seções abaixo descrevem o escopo e a arquitetura **planejados**.

Aplicativo Android nativo para consulta e conversão de cotações de moedas estrangeiras em relação ao Real brasileiro (**BRL**), projetado com foco em **Offline First**, precisão monetária e arquitetura limpa.

---

## Funcionalidades Previstas (Planejadas)

- **Consulta de Cotações:** Visualização de compra (`bid`), venda (`ask`), variação e data/hora da última atualização bem-sucedida, com atualização manual.
- **10 Moedas Padrão:** Inicialização automática com `USD`, `EUR`, `GBP`, `CAD`, `AUD`, `CHF`, `JPY`, `ARS`, `CNY` e `MXN` em relação ao BRL, preservadas contra remoção.
- **Pesquisa e Catálogo de Moedas:** Consulta ao catálogo de moedas/pares disponíveis na AwesomeAPI, busca por código ou nome, validação de suporte do par antes da adição e remoção apenas de moedas adicionadas pelo usuário.
- **Favoritos:** Marcação/desmarcação de moedas favoritas persistidas no Room, com área/filtro dedicado e preservação ao reabrir o app.
- **Conversor de Moedas:** Cálculo monetário preciso com `BigDecimal` utilizando `bid` como taxa padrão e tratamento explícito para moedas sem cotação disponível.
- **Offline First e Sincronização:** Room como fonte única da verdade observada via `Flow`, exibição imediata de dados locais, alerta de cotação desatualizada após **15 minutos**, atualização ao abrir o app, sincronização periódica a cada **6 horas** via WorkManager e re-sincronização condicionada à volta de conexão após falhas.

---

## Tecnologias Planejadas

- **Linguagem:** Kotlin
- **Interface:** Jetpack Compose + Material Design 3
- **Navegação:** Navigation Compose
- **Arquitetura:** Clean Architecture + MVVM (módulo único `:app`)
- **Injeção de Dependência:** Koin
- **Assincronismo:** Kotlin Coroutines, `Flow` e `StateFlow`
- **Rede:** Retrofit + OkHttp
- **Persistência:** Room
- **Background:** WorkManager
- **Cálculos Monetários:** `java.math.BigDecimal`
- **Testes:** JUnit

---

## Arquitetura Planejada

O projeto utilizará módulo único `:app`, separado estritamente por camadas/pacotes:

- `presentation`: telas Jetpack Compose, navegação e `ViewModel`s expondo `StateFlow`.
- `domain`: modelos de domínio, contratos de repositório, calculadora monetária em `BigDecimal` e casos de uso (sem dependência de Android, Retrofit ou Room).
- `data`: implementações de repositório, banco Room (DAOs e entidades), serviços Retrofit, parsers, catálogo complementar e workers do WorkManager.
- `di`: módulos Koin para injeção de dependência.

---

## Funcionamento Offline First (Planejado)

1. O banco **Room** é a única fonte da verdade para toda a interface.
2. As telas observam continuamente os dados locais por meio de `Flow`.
3. Ao abrir o aplicativo, as cotações já armazenadas são exibidas instantaneamente, mesmo sem internet.
4. Quando há conexão, novas cotações são buscadas na AwesomeAPI, persistidas no Room e refletidas automaticamente na UI.
5. Em caso de falha de conexão ou erro da API, os dados anteriores permanecem intactos.
6. Cotações com mais de **15 minutos** desde a última atualização bem-sucedida são marcadas visualmente como desatualizadas.

---

## API Utilizada

- **Provedor:** AwesomeAPI — API de Cotações de Moedas
- **Documentação Oficial:** https://docs.awesomeapi.com.br/api-de-moedas
- **Exemplo de endpoint de cotações:** `https://economia.awesomeapi.com.br/json/last/USD-BRL,EUR-BRL`
- **Endpoint de disponibilidade a validar/integrar:** `https://economia.awesomeapi.com.br/json/available` (e endpoints oficiais em XML documentados: `https://economia.awesomeapi.com.br/xml/available` e `https://economia.awesomeapi.com.br/xml/available/uniq`)

---

## Estrutura do Projeto (Planejada)

```text
/
├── SPECIFICATION.md        # Requisitos funcionais, não funcionais, regras e rastreabilidade
├── PLANNING.md             # Arquitetura, fluxo de dados, Room, API e sincronização
├── TASKS.md                # Plano incremental em 8 etapas (E00 a E07)
├── TEST_RESULTS.md         # Registro de testes unitários e validações manuais
├── README.md               # Visão geral do projeto
└── app/
    └── src/main/java/com/example/
        ├── presentation/   # Telas Compose, ViewModels, Navigation, Theme
        ├── domain/         # Modelos, UseCases, Contratos, Calculadora BigDecimal
        ├── data/           # Room, Retrofit, Repositories, WorkManager
        └── di/             # Módulos Koin
```

---

## Configuração e Execução (Planejadas — Ainda Não Implementadas)

> As instruções abaixo descrevem o fluxo previsto após a implementação das etapas E01–E06:

1. Abrir o projeto no Android Studio (ou compilar via Gradle no ambiente).
2. Sincronizar dependências Gradle (`compile_applet` / `gradle assembleDebug`).
3. Executar em dispositivo ou emulador Android (API 24+).
4. Não é necessária nenhuma chave de API paga ou cadastro/login para o fluxo básico de consulta pública na AwesomeAPI.
5. Para executar os testes unitários planejados: `gradle :app:testDebugUnitTest`.

---

## Estratégia de Testes Planejada

- **Testes Unitários (JUnit):** Regras de conversão em `BigDecimal` com taxa `bid`, validação de expiração após 15 minutos, proteção das 10 moedas padrão contra remoção, validação de pares suportados no catálogo, pesquisa por código/nome, parsing/mapeamento de DTOs da AwesomeAPI e comportamento Offline First do repositório.
- **Validações Manuais:** Inicialização com 10 moedas padrão, adição/remoção de moedas, persistência de favoritos após reiniciar o app, cenários offline/modo avião, sinalização de dados desatualizados (> 15 min) e agendamento com WorkManager.

---

## Limitações Conhecidas

- **WorkManager e horário exato:** Tarefas periódicas de 6 horas e trabalhos condicionados à rede dependem das políticas de energia do Android (Doze, App Standby) e não garantem execução em horário exato.
- **Cache e bloqueio de 404 na AwesomeAPI:** Chamadas não autenticadas na AwesomeAPI possuem cache de 1 minuto no servidor; requisições repetidas para pares inexistentes (HTTP 404 `CoinNotExists`) podem gerar bloqueio temporário de IP (~1 hora), exigindo validação prévia pelo catálogo local.
- **Endpoint `/available`:** Na documentação oficial da AwesomeAPI, os endpoints listados formalmente para catálogo estão em formato XML (`/xml/available` e `/xml/available/uniq`). O comportamento exato de `/json/available` será validado na Etapa E03 com fallback planejado para o formato XML documentado.
- **Pares de moedas:** Nem toda moeda existente no mundo possui par direto `XXX-BRL` suportado pela API; apenas pares confirmados no catálogo poderão ser adicionados.
- **Fora de escopo:** Sem login, integração bancária, pagamentos, compra/venda real, carteira, gráficos avançados, alertas/notificações ou backend próprio.

---

## Evidências e Entrega

- **Status atual:** Aguardando validação da Etapa 0 para início da Etapa E01.
- **Evidências (APK, capturas de tela, logs de testes):** Serão adicionadas na Etapa E07 após implementação e execução dos testes planejados em `TEST_RESULTS.md`.
