# ⚡ Cabô – Controle, Balanço & Orçamento

> **C**ontrole de **A**quisições, **B**alanço e **Ô**rçamento.  
> *"Chegou dia 10 e já cabô? Assuma o controle de listas, tickets e orçamento para nunca mais passar do limite."*

Aplicativo nativo Android moderno, veloz e offline-first desenvolvido com **Kotlin**, **Jetpack Compose (Material 3)**, **Room Database (SQLite)** e **AlarmManager**.

---

## ✨ Principais Funcionalidades

### 💳 1. Gestão Multi-Carteiras & Teto de Gastos
- Cadastro independente de saldos por meio de pagamento:
  - **Ticket Alimentação**
  - **Ticket Refeição**
  - **Cartão de Crédito**
  - **Cartão de Débito**
  - **Dinheiro / Pix**
- **Dedução Automática:** Ao finalizar uma compra, selecione o meio utilizado e o valor é abatido instantaneamente do saldo disponível.
- **Apelido para Gastos Não Cadastrados:** Se o orçamento de um meio estiver zerado ou não cadastrado, atribua um apelido ao gasto para identificá-lo no extrato.

### ⏰ 2. Recarga Automática com Notificações Agendadas
- Programe o dia do mês e o valor da recarga de cada benefício/carteira.
- Notificação local disparada no dia agendado via `AlarmManager` + `BroadcastReceiver`, atualizando o saldo automaticamente.

### 📊 3. Painel Analítico Mensal & Barra de Excesso
- Aba dedicada **"Mensal"** que abre e fecha os ciclos a cada mês:
  - **Barra Segmentada Dinâmica:** Fica **verde** conforme o orçamento é consumido dentro do limite e enche em **vermelho** apenas a fração excedente.
  - **Porcentagem de Excesso:** Detalha quanto cada meio de pagamento estourou a cota.
  - **Picos de Compra:** Identifica os períodos de maior volume de gastos no mês (Manhã, Tarde ou Noite).
  - **Histórico Mensal:** Navegue pelos meses anteriores para comparar seus padrões de consumo.

### 🛒 4. Listas Inteligentes & Comparador de Menor Preço
- Lista de compras ativa com cálculo em tempo real de itens e valores.
- Comparador de preços que identifica automaticamente a loja mais barata com base no histórico real de compras anteriores (e avisa caso os valores sejam os mesmos).
- Lembretes de compras com notificações e alertas sonoros configuráveis.

---

## 🏗️ Arquitetura e Tecnologias

- **Linguagem:** Kotlin 2.0+
- **Interface:** Jetpack Compose + Material Design 3 (Edge-to-Edge habilitado)
- **Persistência Local:** Room Database (SQLite relacional nativo)
- **Agendamento de Alarmes:** Android `AlarmManager` nativo com `RECEIVE_BOOT_COMPLETED`
- **Arquitetura:** MVVM (Model-View-ViewModel) com StateFlow e Coroutines
- **Identidade Visual:** Ícone adaptativo vetorial exclusivo com tema Slate 900 e Emerald

---

## 📁 Estrutura do Projeto

```
cabo-app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/mercadosmart/
│       │   ├── MainActivity.kt               # Scaffold, Edge-to-Edge e Navigation Tabs
│       │   ├── MercadoSmartApp.kt            # Application e canais de notificação
│       │   ├── data/
│       │   │   ├── AppDatabase.kt            # Singleton Room Database (v4)
│       │   │   ├── model/
│       │   │   │   ├── ShoppingItem.kt       # Entidade de itens da lista ativa
│       │   │   │   ├── PurchaseHistory.kt    # Entidade de compras arquivadas
│       │   │   │   ├── PaymentBudget.kt      # Entidade de carteiras e orçamentos
│       │   │   │   ├── MonthlySummary.kt     # Entidade de consolidação mensal
│       │   │   │   └── Reminder.kt           # Entidade de lembretes
│       │   │   └── dao/
│       │   │       ├── ShoppingItemDao.kt
│       │   │       ├── PurchaseHistoryDao.kt
│       │   │       ├── PaymentBudgetDao.kt
│       │   │       ├── MonthlySummaryDao.kt
│       │   │       └── ReminderDao.kt
│       │   ├── viewmodel/
│       │   │   └── MainViewModel.kt          # Regras de negócio, cálculos e StateFlows
│       │   ├── notification/
│       │   │   └── NotificationHelper.kt     # AlarmManager e agendamento de recargas
│       │   └── ui/
│       │       ├── theme/ (Color, Theme, Type)
│       │       ├── components/               # Diálogos de gastos, orçamentos e compras
│       │       └── screens/
│       │           ├── ShoppingListScreen.kt # Lista ativa
│       │           ├── HistoryScreen.kt      # Histórico de compras por mercado
│       │           ├── CompareScreen.kt      # Comparador de preços inteligentes
│       │           ├── MonthlySummaryScreen.kt # Dashboard mensal verde/vermelho
│       │           └── RemindersScreen.kt    # Lembretes com alarme
│       └── res/
│           ├── drawable/ (ic_launcher_background.xml, ic_launcher_foreground.xml)
│           └── values/ (strings.xml, themes.xml)
└── build.gradle.kts
```

---

## 🚀 Como Executar

1. Clone o repositório:
   ```bash
   git clone https://github.com/gabcoello/cabo-app.git
   ```
2. Abra o projeto no **Android Studio** (Koala ou superior).
3. Aguarde o Gradle sincronizar as dependências.
4. Execute em um emulador ou dispositivo físico com Android 8.0 (API 26) ou superior.
