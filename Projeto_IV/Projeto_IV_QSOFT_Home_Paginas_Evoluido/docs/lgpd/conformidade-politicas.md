# Conformidade LGPD – Políticas de uso de IA e aceite (US PP-21)

Documento da Task PS-74 (DoD). Descreve o que a funcionalidade "Registrar Política de Uso Responsável de IA"
guarda, para quê, quem acessa e o que ainda precisa ser decidido pelo grupo/PO. **Não é parecer jurídico**: os itens
marcados como **A VALIDAR** dependem de decisão do grupo, do PO e, se houver, do encarregado (DPO) da empresa cliente.

## 1. O que a funcionalidade faz

- O administrador publica uma política de uso de IA vinculada a uma empresa (título, texto, obrigatória ou não).
- Toda alteração gera uma **nova versão**; as versões anteriores não são apagadas (RN14, CA-14.3).
- O colaborador lê a versão vigente e registra o aceite (RF15).
- Se a empresa tem política **obrigatória** vigente que o colaborador ainda não aceitou, o registro de uso de IA
  feito em nome dele é bloqueado (BDD 2).

## 2. Dados guardados

| Tabela | Campos | Contém dado pessoal? |
|---|---|---|
| `politica_uso` | `id`, `codigo`, `empresa_id`, `titulo`, `conteudo`, `versao`, `obrigatoria`, `vigente`, `publicada_em` | Não. É o texto da política e a empresa (pessoa jurídica). |
| `aceite_politica` | `id`, `funcionario_id`, `politica_id`, `versao`, `data_aceite` | **Sim.** Liga um funcionário identificado a uma versão da política e ao momento do aceite. |

O **dado pessoal novo** criado por esta US é o vínculo **funcionário ↔ versão da política ↔ data/hora do aceite**.
O funcionário é uma referência (`funcionario_id`) ao cadastro que já existe (`funcionario`); nenhum dado cadastral
é copiado para a tabela de aceites. A data é gravada em UTC (`Instant`) e exibida no fuso do navegador.

Pela API, as respostas de aceite trazem também o **nome** do funcionário (`funcionarioNome`), lido do cadastro.

**Não são coletados:** IP, navegador (user agent), localização, assinatura, nem cópia de documento.

**Dados sensíveis (art. 5º, II da LGPD):** a funcionalidade não coleta nenhum. Ressalva: o `conteudo` da política
é texto livre digitado pelo administrador; a orientação é não escrever dados pessoais nele (o texto é visto por todos
os colaboradores da empresa).

## 3. Finalidade

Comprovar que cada colaborador teve ciência e aceitou a versão vigente das políticas de uso de IA da empresa
(RF15, RN15) e condicionar o registro de uso de IA a esse aceite quando a política é obrigatória. Os dados não são
usados para outra finalidade (não entram em painéis de produtividade, custos ou pontuação).

## 4. Quem acessa

| Ação | Hoje | Quando existir login de colaborador |
|---|---|---|
| Publicar / nova versão (`POST`/`PUT /api/politicas`) | Só perfil **Administrador** (`ROLE_ADMIN`, regra no `SecurityConfig`). Sem login: 401. Outro perfil: 403. | Continua só para administrador; o colaborador **não** poderá publicar nem alterar. |
| Consultar políticas, versões e pendências | Qualquer usuário autenticado | Colaborador vê as políticas da própria empresa. |
| Registrar aceite | Qualquer usuário autenticado, escolhendo o colaborador na tela (provisório) | **A FAZER:** o colaborador só poderá aceitar em nome dele mesmo (`funcionarioId` vindo da sessão, não da tela). |
| Consultar histórico de aceites (`GET /api/politicas/aceites`) | Qualquer usuário autenticado | **A FAZER:** só o próprio colaborador e os administradores da empresa dele. |

Hoje o único usuário existente é o administrador temporário em memória (ver README, "Login temporário"), então na
prática só ele acessa. As linhas marcadas como **A FAZER** precisam ser implementadas junto com o login de colaborador.

Logs: o BFF (`frontend/server.js`) escreve no console a URL de cada chamada, o que inclui identificadores
(`funcionarioId`, UUID) nas consultas de pendências e aceites. Não há nome nem outro dado cadastral nesses logs.

## 5. Preservação do histórico (RN14 / RN15 / CA-15.4)

- Versões de política nunca são apagadas: a nova versão encerra a vigência da anterior (`vigente = false`).
- Cada aceite aponta para a **linha da versão aceita**; publicar uma nova versão não altera os aceites anteriores.
- Uma nova versão exige novo aceite: o aceite da versão 1 não vale para a versão 2.
- Aceitar de novo a mesma versão **não duplica** o registro: a API devolve o aceite já existente, com a data original
  (há também restrição única `funcionario_id + politica_id` no banco).
- Não existe endpoint de exclusão de política, versão ou aceite.

## 6. Segurança

Sessão HTTP com cookie `http-only`/`same-site=lax`, proteção CSRF, senha do usuário temporário com BCrypt e todas as
rotas `/api/politicas/**` exigindo login (mesma configuração do restante da API). Os dados ficam no PostgreSQL do
projeto; a cópia de segurança e a criptografia do banco seguem o que o grupo definir para o projeto inteiro.

## 7. Direitos do titular (art. 18) – situação atual

- **Acesso/confirmação:** possível pelo histórico de aceites do colaborador (tela de Governança e API).
- **Correção:** não se aplica ao aceite (é o registro de um fato); correções de nome etc. são feitas no cadastro.
- **Eliminação/anonimização:** **não implementado** – ver pendências.

## 8. Pontos EM ABERTO para o grupo validar

1. **Base legal – A VALIDAR.** A hipótese legal (art. 7º da LGPD) para guardar o aceite **não foi definida** neste
   trabalho e deve ser decidida pelo grupo/PO com apoio jurídico. Validar também se o "aceite" é apenas comprovação
   de ciência da política ou se será tratado como consentimento (as regras são diferentes).
2. **Prazo de retenção – A VALIDAR.** Hoje os aceites ficam guardados sem prazo, para cumprir RN14/RN15. Falta
   definir por quanto tempo guardar (por exemplo, durante o vínculo do colaborador e por quanto tempo depois dele).
3. **Exclusão/anonimização – A VALIDAR.** Não há procedimento. Ponto de atenção: preservar a prova do aceite
   (RN15) conflita com apagar o registro; uma alternativa a discutir é anonimizar o colaborador mantendo versão e
   data. Hoje também não existe exclusão de funcionário; quando existir, a chave estrangeira de `aceite_politica`
   impedirá apagar funcionário com aceites até essa regra ser decidida.
4. **Aviso ao colaborador – A VALIDAR.** Texto informando que o aceite fica registrado (com versão e data) e por quê.
5. **Encarregado (DPO) e registro das operações de tratamento** da empresa cliente – fora do escopo do sistema,
   mas precisam ser indicados pelo grupo/PO.
6. **Login de colaborador** – necessário para as restrições de acesso marcadas como A FAZER na seção 4.
