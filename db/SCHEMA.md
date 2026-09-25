# 🗄️ Turso LibSQL Database Schema | AmortizaFlow

Este documento descreve o esquema de tabelas, tipos de dados e padrões do banco de dados na nuvem **Turso (LibSQL / SQLite)** do **AmortizaFlow**.

Este mesmo banco é utilizado pela aplicação web no Render.com e pode ser acessado diretamente por aplicativos mobile (Android / Flutter / React Native / AI Studio).

---

## 📡 Informações de Conexão

- **Host / URL**: `libsql://amortiza-francescotorres.aws-us-west-2.turso.io`
- **Driver**: `@libsql/client` (Node.js/JS), `libsql-client-kt` (Kotlin/Android), `sqflite` / `powersync` (Flutter), ou HTTP API padrão LibSQL.

---

## 🏛️ Tabelas e Definições de Colunas

### 1. `users` (Cadastro de Usuário / Titular)
| Coluna | Tipo SQLite | Nulo? | Padrão | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `TEXT` | NÃO | (PK) | Identificador único (`default_user`) |
| `name` | `TEXT` | NÃO | - | Nome completo do titular |
| `email` | `TEXT` | SIM | NULL | E-mail de contato |
| `phone` | `TEXT` | SIM | NULL | Telefone / WhatsApp |
| `created_at` | `TEXT` | NÃO | ISO-8601 | Data de criação do registro |
| `updated_at` | `TEXT` | NÃO | ISO-8601 | Data da última atualização |

---

### 2. `contracts` (Configurações Gerais do Financiamento)
| Coluna | Tipo SQLite | Nulo? | Padrão | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `TEXT` | NÃO | (PK) | ID do contrato (`main_contract`) |
| `user_id` | `TEXT` | SIM | `default_user` | Referência ao usuário |
| `title` | `TEXT` | NÃO | - | Título descritivo do contrato |
| `contract_number` | `TEXT` | SIM | `FIN-2026-60P` | Número do contrato bancário |
| `total_installments` | `INTEGER` | NÃO | `60` | Total de parcelas contratadas (60) |
| `nominal_installment` | `REAL` | NÃO | `985.38` | Valor nominal fixo da parcela (R$) |
| `total_contract_amount` | `REAL` | NÃO | `59122.80` | Valor contratual total (R$) |
| `monthly_interest_rate` | `REAL` | NÃO | `0.0165` | Taxa de juros mensal aproximada |
| `start_date` | `TEXT` | NÃO | `14/05/2026` | Data da 1ª parcela |
| `original_end_date` | `TEXT` | NÃO | `14/04/2031` | Data de término contratual |
| `current_projected_end_date` | `TEXT` | SIM | `14/01/2031` | Nova data estimada com adiantamentos |
| `created_at` | `TEXT` | NÃO | ISO-8601 | Registro de criação |
| `updated_at` | `TEXT` | NÃO | ISO-8601 | Registro de modificação |

---

### 3. `installments` (As 60 Parcelas do Financiamento)
| Coluna | Tipo SQLite | Nulo? | Padrão | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `parcel_number` | `INTEGER` | NÃO | (PK) | Número da parcela (1 a 60) |
| `contract_id` | `TEXT` | NÃO | `main_contract` | ID do contrato pai |
| `due_date` | `TEXT` | NÃO | - | Data de vencimento (`DD/MM/AAAA`) |
| `nominal_amount` | `REAL` | NÃO | `985.38` | Valor nominal da parcela (R$) |
| `theoretical_amortization` | `REAL` | NÃO | - | Parcela do principal amortizado (R$) |
| `interest_amount` | `REAL` | NÃO | - | Parcela de juros embutidos (R$) |
| `actual_paid_amount` | `REAL` | SIM | NULL | Valor efetivamente pago (R$) |
| `payment_date` | `TEXT` | SIM | NULL | Data da quitação (`DD/MM/AAAA`) |
| `is_paid` | `INTEGER` | NÃO | `0` | Status (`1` = Pago, `0` = Pendente) |
| `is_anticipated` | `INTEGER` | NÃO | `0` | Se foi quitada por antecipação (`1`/`0`) |
| `saved_interest` | `REAL` | NÃO | `0.00` | Economia em juros obtida na quitação |
| `notes` | `TEXT` | SIM | `""` | Observações / Registro bancário |
| `created_at` | `TEXT` | NÃO | ISO-8601 | Data de criação |
| `updated_at` | `TEXT` | NÃO | ISO-8601 | Data da última alteração |

---

### 4. `amortization_history` (Histórico de Amortizações Extraordinárias)
| Coluna | Tipo SQLite | Nulo? | Padrão | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `INTEGER` | NÃO | (PK AI) | Identificador sequencial |
| `contract_id` | `TEXT` | NÃO | `main_contract` | Contrato associado |
| `simulation_type` | `TEXT` | NÃO | `REDUCE_TERM` | Tipo (`REDUCE_TERM` ou `REDUCE_INSTALLMENT`) |
| `amount_invested` | `REAL` | NÃO | - | Valor total aplicado na amortização |
| `installments_eliminated` | `TEXT` | NÃO | - | JSON array de parcelas quitadas (ex: `[57, 56]`) |
| `months_shortened` | `INTEGER` | NÃO | `0` | Quantidade de meses reduzidos |
| `saved_interest` | `REAL` | NÃO | `0.00` | Total de juros futuros poupados |
| `projected_end_date` | `TEXT` | SIM | - | Nova data projetada de término |
| `notes` | `TEXT` | SIM | - | Observações adicionais |
| `created_at` | `TEXT` | NÃO | ISO-8601 | Timestamp da execução |

---

### 5. `app_settings` (Chaves de Configuração Genéricas)
| Coluna | Tipo SQLite | Nulo? | Padrão | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| `setting_key` | `TEXT` | NÃO | (PK) | Chave única da configuração |
| `setting_value` | `TEXT` | NÃO | - | Valor serializado (JSON ou string) |
| `updated_at` | `TEXT` | NÃO | ISO-8601 | Data de atualização |

---

## 🔄 Mapeamento e Compatibilidade Mobile / AI Studio

- No banco SQLite/LibSQL: Nomes de coluna em **`snake_case`** (`parcel_number`, `due_date`, `is_paid`, etc.).
- Na API REST (`/api/installments`, `/api/summary`): Nomes em **`camelCase`** (`parcelNumber`, `dueDate`, `isPaid`, etc.) para 100% de compatibilidade com os modelos do app Android/Compose existente.
