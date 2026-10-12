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

## Painel de Produtividade de IA

Página `frontend/public/produtividade.html` (menu "Produtividade IA"). Os dados vêm do backend em
`GET /api/produtividade/resumo` e `GET /api/produtividade/empresas/{id}` (`ALL` para todas as empresas),
que consultam no PostgreSQL as tabelas da base de uso de IA (`ai_usage_logs`, `employees`, `departments`,
`ai_billing`, `ai_budgets`, `ai_licenses`, `approved_ai_tools`, `ai_tools`, `training_records`,
`security_events`, `projects`, `pull_requests`, `builds`, `deployments`) no schema `public`.

- Na primeira subida, se uma dessas tabelas não existir ou estiver vazia, o backend cria a tabela e importa
  o CSV correspondente de `dados/csv` (variável `PRODUTIVIDADE_CSV_DIR`). Tabelas que já têm dados não são alteradas.
- Cada carga da página consulta o banco: registros novos inseridos nessas tabelas aparecem ao clicar em "Atualizar dados".
- `ai_billing.total_cost` é somado como valor em reais.

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
