# 🚀 AmortizaFlow

<div align="center">

![AmortizaFlow Banner](https://img.shields.io/badge/AmortizaFlow-Finan%C3%A7as%20Inteligentes-006C4C?style=for-the-badge&logo=cashapp&logoColor=white)
![Node.js](https://img.shields.io/badge/Node.js-18+-339933?style=for-the-badge&logo=node.js&logoColor=white)
![Android](https://img.shields.io/badge/Android-Jetpack%20Compose-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Turso DB](https://img.shields.io/badge/Turso-LibSQL%20Cloud-4FF8D2?style=for-the-badge&logo=sqlite&logoColor=black)
![Render](https://img.shields.io/badge/Render-Deploy%20Cloud-46E3B7?style=for-the-badge&logo=render&logoColor=black)

**Controle Inteligente de Financiamento, Amortização Extraordinária e Economia de Juros**  
*Plataforma integrada Web PWA & Aplicativo Nativo Android*

[Acessar no GitHub](https://github.com/francescotorres/AmortizaFlow) • [Guia de Deploy no Render](RENDER_DEPLOY.md)

</div>

---

## 📖 Sobre o Projeto

O **AmortizaFlow** é uma solução completa para controle financeiro e planejamento estratégico de quitação de financiamentos (especialmente financiamento veicular C6 Auto). 

A plataforma permite acompanhar a evolução do contrato mês a mês, registrar pagamentos ordinários, simular e registrar **amortizações extraordinárias** (com cálculo automático da redução do saldo devedor e juros poupados), além de oferecer links rápidos para emissão de 2ª via de boletos diretamente no portal oficial do credor.

---

## ✨ Principais Funcionalidades

- **📊 Dashboard Financeiro em Tempo Real**:
  - Saldo devedor atualizado automaticamente.
  - Total de parcelas pagas vs. restantes.
  - Economia acumulada de juros gerada por amortizações.
  - Data e valor da próxima parcela com lembrete visual.

- **⚡ Simulador de Amortização Antecipada**:
  - Simulações com cálculo SAC ou Price.
  - Projeção imediata de parcelas eliminadas do final do contrato e dinheiro economizado.

- **🏦 Integração com Portal C6 Auto**:
  - Atalhos integrados no cabeçalho e card exclusivo para emissão de 2ª via de boletos, extratos e solicitação de quitação.

- **☁️ Banco de Dados em Nuvem (Turso LibSQL)**:
  - Persistência contínua com banco SQLite em nuvem (Turso / LibSQL).
  - Fallback automático para SQLite local durante desenvolvimento offline.

- **🌓 Interface Moderna & Responsiva**:
  - Tema Claro e Tema Escuro (Dark Mode).
  - Totalmente adaptado para Mobile e Desktop (PWA instalável).

- **📱 App Nativo Android**:
  - Aplicativo desenvolvido em **Kotlin** com **Jetpack Compose** e **Material 3**.

---

## 🛠️ Tecnologias Utilizadas

### Web & Backend
- **Node.js** com **Express**: API REST leve e rápida.
- **Turso LibSQL Client (`@libsql/client`)**: Banco SQLite distribuído em nuvem.
- **PWA (Progressive Web App)**: Service Worker e Web Manifest para instalação como aplicativo.
- **CSS Moderno**: Glassmorphism, CSS Custom Properties, temas dinâmicos e design responsivo.

### Mobile (Android)
- **Kotlin**: Linguagem principal.
- **Jetpack Compose**: Interface declarativa moderna.
- **Material Design 3**: Componentes visuais refinados.
- **Coroutines & Flow**: Programação assíncrona.

---

## 🚀 Como Executar Localmente

### 1. Clonar o Repositório
```bash
git clone https://github.com/francescotorres/AmortizaFlow.git
cd AmortizaFlow
```

### 2. Configurar Variáveis de Ambiente
Crie um arquivo `.env` baseado no `.env.example`:
```env
PORT=10000
TURSO_DATABASE_URL=libsql://seu-banco.turso.io
TURSO_AUTH_TOKEN=seu_token_aqui
```
*(Se as variáveis do Turso não forem preenchidas, o sistema utilizará SQLite local automaticamente)*

### 3. Instalar Dependências e Iniciar o Servidor Web
```bash
npm install
npm start
```
Acesse no navegador: `http://localhost:10000`

### 4. Executar o Projeto Android
1. Abra o diretório do projeto no **Android Studio**.
2. Aguarde a sincronização do Gradle.
3. Execute o app em um emulador ou dispositivo físico conectado.

---

## 🌐 Deploy em Nuvem (Render.com)

O AmortizaFlow já possui configuração pronta para deploy contínuo no **Render**:
1. Conecte o repositório `francescotorres/AmortizaFlow` no Render.com.
2. Crie um novo **Web Service**.
3. Configure o Build Command: `npm install` e Start Command: `node server.js`.
4. Adicione as variáveis de ambiente `TURSO_DATABASE_URL` e `TURSO_AUTH_TOKEN`.
5. Consulte o passo a passo completo no arquivo [RENDER_DEPLOY.md](RENDER_DEPLOY.md).

---

## 📄 Licença

Este projeto está sob licença proprietária de seu autor.
Desenvolvido com foco em inteligência financeira e eficiência de amortização.
