# 🤖 QAI Control

> Plataforma de Governança, Monitoramento e Análise do Uso de Inteligência Artificial nas Organizações.

Projeto desenvolvido durante o **Projeto Integrado IV – Ciência de Dados e Negócios | ESPM**, em parceria com a **QSOFT**.

---

## 📌 Sobre o projeto

O **QAI Control** é uma plataforma web criada para auxiliar empresas a monitorar e compreender como ferramentas de Inteligência Artificial estão sendo utilizadas dentro da organização.

A solução centraliza dados relacionados ao uso de IA e os transforma em indicadores que auxiliam gestores na análise de:

- custos;
- volume de utilização;
- eficiência;
- produtividade;
- governança;
- comportamento de uso;
- riscos relacionados à dependência de IA.

O objetivo é permitir que organizações utilizem Inteligência Artificial de maneira mais **estratégica, eficiente, responsável e orientada por dados**.

---

## 🎯 Problema

Com o crescimento do uso de Inteligência Artificial nas empresas, diferentes ferramentas passaram a ser utilizadas por equipes e departamentos sem que exista, necessariamente, uma visão centralizada sobre:

- quanto está sendo gasto;
- quais ferramentas são mais utilizadas;
- quais departamentos utilizam mais IA;
- se o uso está gerando resultados;
- onde existem desperdícios;
- se existem comportamentos de uso excessivo;
- quais riscos de governança estão associados ao uso da tecnologia.

Essa falta de visibilidade pode dificultar a tomada de decisão e gerar custos desnecessários, além de riscos relacionados à governança e ao uso inadequado da Inteligência Artificial.

---

## 💡 Solução

O QAI Control reúne dados de utilização das ferramentas de IA e disponibiliza essas informações por meio de dashboards e indicadores.

A plataforma busca oferecer uma visão integrada do uso de IA dentro da organização, permitindo identificar padrões, comparar departamentos e apoiar decisões relacionadas à adoção e utilização das ferramentas.

---

## 🧠 Hipótese de inovação

### Mapa Inteligente de Risco de Dependência de IA

Como inovação do projeto, o QAI Control propõe um módulo capaz de identificar possíveis sinais de **dependência excessiva de Inteligência Artificial** dentro de equipes, departamentos e processos organizacionais.

O módulo analisa diferentes indicadores de utilização para gerar um:

### AI Dependency Risk Score

Índice de risco de dependência de IA em uma escala de **0 a 100**.

A análise poderá considerar informações como:

- frequência de utilização;
- volume de chamadas de IA;
- tempo de uso;
- concentração do uso em determinadas ferramentas;
- revisão humana;
- repetição de padrões de utilização;
- comportamento histórico;
- distribuição do uso entre usuários e departamentos.

Os resultados poderão ser apresentados por meio de:

- 🔥 Heatmaps;
- 🕸️ Network Graphs;
- 📊 Risk Scores;
- ⚠️ alertas de risco;
- 💡 recomendações.

O objetivo não é limitar a utilização da IA, mas ajudar as empresas a identificar situações em que seu uso pode estar se tornando excessivamente concentrado ou dependente.

---

## ⚙️ Principais funcionalidades

### MVP

- Cadastro e login de usuários;
- Registro e importação de dados de utilização de IA;
- Armazenamento dos dados;
- Dashboard com indicadores principais;
- Visualização do volume de utilização;
- Visualização dos custos das ferramentas;
- Indicadores de eficiência;
- Filtros por período, ferramenta e departamento;
- Comparação de indicadores;
- Identificação de possíveis gastos excessivos;
- Controle de acesso por perfil;
- Integração entre frontend, backend e banco de dados.

### Inovação

- Cálculo do risco de dependência de IA;
- Classificação de usuários, equipes e departamentos;
- Heatmap de risco;
- Network Graph de utilização;
- Histórico dos indicadores;
- Identificação de padrões de utilização;
- Recomendações com base nos riscos identificados.

---

## 📊 Exemplos de indicadores

A plataforma poderá acompanhar indicadores como:

| Indicador | Descrição |
|---|---|
| Volume de uso | Quantidade de utilizações das ferramentas de IA |
| Custo total | Valor gasto com ferramentas e serviços de IA |
| Custo por usuário | Custo médio associado a cada usuário |
| Custo por departamento | Distribuição dos custos entre áreas |
| Ferramenta mais utilizada | IA com maior quantidade de utilizações |
| Intensidade de uso | Frequência de utilização ao longo do tempo |
| Risk Score | Índice de risco de dependência de IA |
| Revisão humana | Indicador de validação humana das respostas |
| Concentração de uso | Dependência de uma ferramenta ou modelo específico |

---

## 🗄️ Banco de dados

O projeto utiliza **PostgreSQL** para armazenamento e gerenciamento dos dados.

O banco reúne informações relacionadas a:

- empresas;
- departamentos;
- usuários;
- ferramentas de IA;
- modelos de IA;
- utilização das ferramentas;
- custos;
- sessões;
- prompts;
- revisões humanas;
- indicadores;
- scores de dependência;
- histórico de risco.

Esses dados permitem alimentar os dashboards e realizar as análises necessárias para o módulo de risco de dependência de IA.

---

## 🏗️ Arquitetura da solução

Fluxo simplificado:

```text
Usuário
   │
   ▼
Frontend
   │
   ▼
Backend / API
   │
   ├──────────────► Módulo Analítico
   │                       │
   │                       ▼
   │               Indicadores / Scores
   │
   ▼
PostgreSQL
   │
   ▼
Dashboards e Visualizações
