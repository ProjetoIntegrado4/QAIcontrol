# Projeto IV - QSOFT - Versão Corrigida

Exemplo didático com:

- **Spring Boot / Java 17**: domínio de Empresa e Consumo de IA;
- **PostgreSQL**: persistência;
- **Node.js Alert Service**: microserviço independente para avaliação de consumo;
- **Node.js Front/BFF**: serve a interface, atua como proxy e possui endpoint agregado de dashboard;
- **Docker Compose**: sobe todo o ambiente.

## Arquitetura

```text
Browser :3000
   |
   v
Node.js Front/BFF :3000
   |
   v
Spring Boot :8080 ------> Node Alert Service :3001
   |
   v
PostgreSQL :5432
```

## Requisito Empresa

A Empresa é requisito explícito do domínio. Cada empresa possui um UUID e um consumo de IA só pode ser registrado para uma empresa existente.

Fluxo:

1. `POST /api/empresa` cria a empresa;
2. o Java gera um `UUID` para a empresa;
3. `POST /api/usages` recebe `empresaId`;
4. o Spring valida se a empresa existe;
5. persiste o consumo;
6. chama o Node Alert Service;
7. devolve consumo + alerta ao BFF/front.

## Subir pelo Docker

Na pasta raiz:

```bash
docker compose down -v
docker compose build --no-cache
docker compose up
```

Acessos:

- Front/BFF: http://localhost:3000
- Login: http://localhost:3000/login.html
- Spring Boot: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui/index.html
- Alert Service health: http://localhost:3001/health
- pgAdmin: http://localhost:5050

pgAdmin:

- e-mail: `admin@admin.com`
- senha: `admin`
- host do PostgreSQL dentro do Docker: `postgres`
- porta: `5432`
- database: `iadb`
- usuário: `ia`
- senha: `ia`

## Login temporário

Enquanto não houver integração com usuários persistidos no banco, o acesso de desenvolvimento é:

- usuário: `admin@qsoft.com`
- senha: `Qsoft123!`

O backend valida a senha com BCrypt e mantém a autenticação em sessão HTTP. Antes de disponibilizar o sistema fora do ambiente local, configure `TEMP_LOGIN_USERNAME`, `TEMP_LOGIN_PASSWORD` e `TEMP_LOGIN_NAME` com valores próprios. O usuário temporário em memória não substitui a futura autenticação vinculada aos registros de admins e funcionários.

## Teste manual da API

Criar empresa:

```bash
curl -X POST http://localhost:8080/api/empresa \
  -H "Content-Type: application/json" \
  -d '{"name":"ACCamargo","area":"Saude"}'
```

Copie o `id` retornado e use como `empresaId`:

```bash
curl -X POST http://localhost:8080/api/usages \
  -H "Content-Type: application/json" \
  -d '{"empresaId":"COLE-O-UUID-AQUI","tokens":6000,"model":"gpt-demo"}'
```

Com 6000 tokens, o serviço Node deverá responder com alerta `WARNING`.

## BFF

O BFF possui dois papéis:

1. **Proxy** em `/api/*`, evitando que o browser conheça diretamente o endereço do Spring;
2. **Agregação específica da tela** em `GET /bff/dashboard`, combinando empresas e consumos.

Assim, o exemplo não é apenas um reverse proxy: há uma operação genuinamente orientada ao frontend.

## Capacitações e certificações em IA (US PP-19)

Página `frontend/public/capacitacao.html` (menu "Capacitações"). O colaborador é escolhido pela empresa e pelo
nome, porque ainda não existe login de funcionário (o acesso atual é o admin temporário).

- `POST /api/certificacao`: registra um curso ou certificação (`funcionarioId`, `empresaId`, `nome`, `instituicao`,
  `dataConclusao` no formato `AAAA-MM-DD`, `tipo` = `CURSO` ou `CERTIFICACAO`);
- `GET /api/certificacao?funcionarioId=...`: histórico do colaborador, do mais recente para o mais antigo;
- `GET /api/certificacao/resumo?funcionarioId=...`: total de registros e pontos de AI Literacy.

Regras: nome, instituição e data de conclusão são obrigatórios (nome e instituição com até 150 caracteres), a data
não pode ser futura e o colaborador precisa existir e ser da empresa informada. A tabela `certificacao` é criada pelo Hibernate (`ddl-auto: update`).

Os pontos de AI Literacy por registro ficam em `app.ai-literacy` no `application.yml` e são aplicados só em
`service/PontuacaoAiLiteracy`. Os valores atuais (curso = 10, certificação = 20) são **preliminares** e precisam ser
validados com o PO.

## Custos de IA (US PP-18)

Página `frontend/public/custos.html` (menu "Custos de IA"). O gestor escolhe a empresa, o período (já vem preenchido
com o último mês que tem uso registrado) e, se quiser, um departamento **ou** um colaborador, e clica em "Calcular".
A tela mostra o custo total, os usos cobrados, agentes × uso direto, os usos sem preço, tabelas por departamento,
colaborador (os de maior custo) e ferramenta, e a memória de cálculo (versão da regra, vigência e fórmula).
Se o período não tiver uso, o total é 0 e aparece "Nenhum uso de IA registrado no período selecionado".

Os dados vêm das tabelas analíticas importadas de `dados/csv` (`ai_usage_logs`, `ai_models`, `companies`,
`departments`, `employees`, `ai_tools`), com ids como `C01`, `C01-D02` e `C01-E0001`. Elas **não** são as entidades
Empresa/Funcionario (UUID) do CRUD. As tabelas `uso_custo` (ai_usage_logs com `model_id` e `application`) e `modelos`
(ai_models) foram acrescentadas ao `produtividade.yml`; as demais reaproveitam o mapeamento do painel.

**Endpoints** (exigem login, como os demais; erros de validação respondem 400 com `message`):

- `GET /api/custos/opcoes[?empresaId=C01]`: empresas e, para a empresa informada, seus departamentos e colaboradores;
  traz também `dataMinima` e `dataMaxima` do uso registrado;
- `GET /api/custos?empresaId=C01&inicio=2026-07-01&fim=2026-07-31[&departamentoId=C01-D02 | &colaboradorId=C01-E0001]`:
  `total`, `moeda`, `usosCobrados`, `usosSemPreco`, `tokensEntrada`, `tokensSaida`, `totalAgentes`, `totalUsoDireto`,
  `porDepartamento[]`, `porColaborador[]`, `porFerramenta[]`, `versaoRegra`, `vigencia`, `formula`, `statusCobrados`,
  `aplicacoesAgente`, `mensagem` e `indisponiveis`.

Validações: empresa, início e fim obrigatórios (datas em `AAAA-MM-DD`, fim inclusive); início ≤ fim; departamento e
colaborador precisam ser da empresa; não aceita os dois filtros ao mesmo tempo. Se as tabelas não existirem no banco,
a resposta vem com `total: null`, a mensagem e o motivo em `indisponiveis` (mesmo padrão do painel de produtividade).

**Regra de cálculo** (aplicada só em `service/custos/RegraCusto`, configurada na seção `custos` do `application.yml`):

```
custo por uso = input_tokens / 1000 × input_price_usd_per_1k_tokens + output_tokens / 1000 × output_price_usd_per_1k_tokens
```

- moeda `USD`, sem conversão de câmbio;
- só entram usos com status em `status-cobrados` (`[success]`);
- usos cuja `application` está em `aplicacoes-agente` (`Pipeline Interno`, `Integracao via Backend Interno`,
  `API Direta`) somam em `totalAgentes`; os demais, em `totalUsoDireto`;
- uso de modelo sem preço não vira custo zero: fica fora do total e é contado em `usosSemPreco` (RN19);
- a versão (`custos-v1-preliminar`) e a vigência voltam em toda resposta, com a fórmula (RN17);
- `limite-colaboradores` (10) define quantos colaboradores aparecem no detalhamento.

O banco faz a soma (`SUM`/`GROUP BY` por dimensão × modelo × agente, filtrando empresa, período e status) e o Java
aplica o preço de cada modelo. Como a fórmula é linear nos tokens, o resultado é o mesmo de somar uso a uso, sem
carregar os ~269 mil registros na memória.

**Pendências para validar com o PO** (a regra está marcada como PRELIMINAR):

1. moeda e câmbio: os preços de `ai_models` estão em USD, mas `ai_billing` está em BRL; falta definir se a tela
   converte e com qual cotação;
2. status cobrados: hoje só `success`; `error`, `timeout` e `rate_limited` também consomem tokens nos registros;
3. lista de "agentes": as três aplicações acima foram escolhidas por serem integrações automáticas, sem usuário;
4. não existe "equipe" nos dados (só empresa, departamento, colaborador, projeto e centro de custo), então o filtro
   por equipe da US não foi implementado.

## Painel de Produtividade de IA

Página `frontend/public/produtividade.html` (menu "Produtividade IA"). Os dados vêm do backend em
`GET /api/produtividade/resumo` e `GET /api/produtividade/empresas/{id}` (`ALL` para todas as empresas).

**Mapeamento do banco (`src/main/resources/produtividade.yml`).** As consultas usam nomes lógicos
(`${uso}`, `${uso.funcionario}`); o arquivo diz qual tabela e coluna física corresponde a cada um, quais
valores de status contam (ex.: `uso-sucesso: [success]`), o schema, a janela "recente" em dias e o tamanho
do ranking. Se o banco mudar de nomes ou de valores, ajuste só esse arquivo.

**Seções independentes.** Cada bloco do painel é uma classe em `service/produtividade/secao` que implementa
`SecaoPainel` e declara as tabelas/colunas de que precisa. Antes de calcular, o backend confere no
`information_schema` se elas existem: o que faltar vem nulo no JSON, com o motivo em `indisponiveis`, e a
página esconde o bloco e cita o motivo no rodapé. As seções rodam em paralelo.

Para acrescentar uma análise:
1. mapeie as tabelas novas em `produtividade.yml`;
2. crie as consultas em `repository/produtividade/<Nome>Consultas.java` usando `ConsultaPainel`;
3. crie `service/produtividade/secao/<Nome>Secao.java` (`@Component`, `@Order`) implementando `SecaoPainel`;
4. no frontend, crie `frontend/public/js/produtividade/secoes/<nome>.js`, registre em `SECOES` (`main.js`)
   e marque o HTML com `data-secao="<chave>"`.

**Importação de CSV.** Na subida, cada `*.csv` de `dados/csv` (variável `PRODUTIVIDADE_CSV_DIR`) vira a
tabela de mesmo nome, se ela não existir ou estiver vazia. Os tipos das colunas são deduzidos dos valores, a
primeira coluna `_id` com valores únicos vira chave primária e as demais `_id` ganham índice. Tabelas que já
têm dados não são alteradas. Cada carga da página consulta o banco: registros novos aparecem ao clicar em
"Atualizar dados".

**Frontend.** O HTML só tem a estrutura; o JavaScript fica em `frontend/public/js/produtividade`
(`main.js`, `api.js`, `formato.js`, `componentes/` com gráficos e tabelas, `secoes/` com um módulo por bloco)
e o estilo em `frontend/public/Estilo.css`.

## Testes automatizados e cobertura

Os testes ficam em `src/test/java`, na mesma estrutura de pacotes do código:
`controller`, `service`, `repository`, `integration/alert` e `domainTeste` (entidades e VOs).
Eles não precisam de banco nem do alert-service: os repositórios são simulados com Mockito
e o alert-service é substituído por um servidor HTTP local durante o teste.

```bash
mvn clean verify
```

O `verify` roda os testes e o JaCoCo. O relatório fica em `target/site/jacoco/index.html`
e o build falha se a cobertura de linhas ficar abaixo de 70%. No Jenkins, o mesmo comando
roda na etapa de testes; o relatório é arquivado e publicado pelo plugin Coverage ou JaCoCo
quando um deles estiver instalado.
