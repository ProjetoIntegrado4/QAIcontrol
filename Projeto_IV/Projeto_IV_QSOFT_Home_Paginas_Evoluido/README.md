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
