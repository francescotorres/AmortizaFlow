# 🚀 Guia de Implantação no Render.com | AmortizaFlow

Este guia orienta o deploy completo da versão Web do **AmortizaFlow** no **[Render.com](https://render.com)**. O projeto já está configurado com `render.yaml` (Blueprint), porta dinâmica e todas as rotas da API financeira.

---

## 📋 Pré-requisitos

1. **Conta no GitHub / GitLab**: Para hospedar o código do projeto em um repositório.
2. **Conta no Render.com**: Cadastro gratuito em [render.com](https://render.com).

---

## 🛠️ Método 1: Deploy Automático via Blueprint (Recomendado)

O repositório já inclui o arquivo [`render.yaml`](file:///C:/Users/Esc.%20Jo%C3%A3o%20Bosco/antigravity/AmortizaFlow/render.yaml), que define automaticamente toda a infraestrutura no Render.

### Passo a Passo:
1. O repositório já está criado e sincronizado no seu GitHub:
   👉 **`https://github.com/francescotorres/AmortizaFlow`**

2. Acesse seu painel no [Render Dashboard](https://dashboard.render.com).
3. Clique no botão **"New +"** no canto superior direito e selecione **"Blueprint"**.
4. Conecte sua conta do GitHub e selecione o repositório **`AmortizaFlow`**.
5. O Render detectará automaticamente o arquivo `render.yaml`:
   - **Service Name**: `amortizaflow-web`
   - **Environment**: `Node`
   - **Plan**: `Free`
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
6. Clique em **"Apply"**.
7. O Render fará o build e fornecerá uma URL pública gratuita (ex: `https://amortizaflow-web.onrender.com`).

---

## ⚙️ Método 2: Deploy Manual como Web Service

Se preferir configurar o serviço manualmente pelo painel do Render:

1. No [Render Dashboard](https://dashboard.render.com), clique em **"New +"** -> **"Web Service"**.
2. Conecte seu repositório Git.
3. Preencha as configurações:
   | Campo | Valor |
   | :--- | :--- |
   | **Name** | `amortizaflow` |
   | **Region** | `Oregon (US West)` ou `Frankfurt (EU Central)` |
   | **Branch** | `main` |
   | **Root Directory** | *(deixe em branco para usar a raiz)* |
   | **Runtime** | `Node` |
   | **Build Command** | `npm install` |
   | **Start Command** | `npm start` |
   | **Instance Type** | `Free` |

4. Em **Advanced**:
   - **Health Check Path**: `/api/health`
   - **Auto-Deploy**: `Yes`
5. Clique em **"Create Web Service"**.

---

## 💻 Testando Localmente Antes do Deploy

Você pode testar a aplicação na sua máquina antes de subir para a nuvem:

1. Abra o terminal na pasta do projeto:
   ```powershell
   cd "C:\Users\Esc. João Bosco\antigravity\AmortizaFlow"
   ```
2. Instale as dependências:
   ```powershell
   npm install
   ```
3. Inicie o servidor:
   ```powershell
   npm start
   ```
4. Abra seu navegador em:
   ```text
   http://localhost:10000
   ```

---

## 🔍 Endpoints da API REST Integrada

A aplicação no Render disponibiliza os seguintes endpoints prontos:

| Método | Endpoint | Descrição |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Status de saúde do serviço e tempo de atividade |
| `GET` | `/api/summary` | KPIs consolidados (total pago, economia de juros, saldo) |
| `GET` | `/api/installments` | Lista das 60 parcelas com filtros (`?status=PAID` / `PENDING`) |
| `PATCH` | `/api/installments/:number/toggle` | Alterna status de pagamento da parcela |
| `PUT` | `/api/installments/:number` | Edita dados da parcela (valor pago, data, observações) |
| `POST` | `/api/simulate` | Simula amortização extraordinária (Prazo vs Parcela) |
| `POST` | `/api/simulate/apply` | Aplica a simulação ao contrato ativo |
| `GET` | `/api/reports` | Relatório mensal histórico de evolução da dívida e juros |
| `POST` | `/api/reset` | Restaura a base para os dados originais da planilha |
| `GET` | `/api/export/csv` | Download da planilha completa em formato `.csv` |

---

## ⚡ Dicas para o Plano Gratuito do Render

- **Modo Sleep (Inatividade)**: No plano gratuito, o Render hiberna instâncias após 15 minutos sem requisições. O primeiro acesso após hibernar pode levar ~30 segundos para acordar o servidor.
- **PWA no Celular**: Você pode acessar a URL do Render no Chrome ou Safari do seu celular e clicar em **"Adicionar à tela de início"** para usar o AmortizaFlow como aplicativo nativo móvel com cache offline!
