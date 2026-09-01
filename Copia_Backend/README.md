# Projeto Integrado - API Backend

## Arquitetura

Este projeto utiliza Java 17 com Spring Boot e Maven para a construção de uma API REST baseada em Arquitetura em Camadas. A escolha do ecossistema Spring Boot se justifica por sua ampla adoção no mercado, maturidade, produtividade no desenvolvimento de aplicações corporativas e integração nativa com recursos essenciais para APIs modernas, como injeção de dependência, configuração externa, exposição de endpoints REST, integração com bancos de dados e suporte a ambientes em nuvem.

A arquitetura foi organizada nas seguintes camadas:

## Controllers

A camada `controllers` é responsável por receber as requisições HTTP, expor os endpoints REST da aplicação e encaminhar as chamadas para a camada de serviço. Essa separação evita que regras de negócio sejam implementadas diretamente nos controladores, mantendo o código mais limpo, testável e aderente ao princípio da responsabilidade única.

## Services

A camada `services` concentra as regras de negócio da aplicação. Ela atua como intermediária entre os controladores e os repositórios, garantindo que validações, fluxos de processamento e decisões de domínio fiquem centralizados em uma camada específica. Essa organização facilita manutenção, evolução e testes unitários.

## Repositories

A camada `repositories` é responsável pela comunicação com a base de dados. Em aplicações Spring Boot, essa camada normalmente utiliza Spring Data JPA para abstrair operações de persistência, reduzindo código repetitivo e permitindo maior padronização no acesso aos dados.

## Models

A camada `models` representa as entidades e estruturas principais do domínio da aplicação. Ela define os objetos que serão manipulados pelas demais camadas e, quando integrada a um banco relacional, pode representar as tabelas do banco de dados por meio de anotações JPA.

## API Stateless

A API foi projetada para ser stateless, ou seja, sem armazenar estado de sessão na memória local do servidor. Cada requisição deve conter todas as informações necessárias para ser processada de forma independente. Essa decisão é essencial para ambientes de computação em nuvem, como Azure, pois permite escalabilidade horizontal com múltiplas instâncias da aplicação operando simultaneamente.

Ao evitar dependência de estado local, qualquer instância da API pode responder a qualquer requisição, reduzindo acoplamento com servidores específicos e facilitando balanceamento de carga, tolerância a falhas e elasticidade automática.

Essa abordagem também está alinhada às práticas de FinOps, pois permite ajustar dinamicamente a quantidade de recursos computacionais conforme a demanda real. Com isso, é possível escalar a aplicação para cima em momentos de maior uso e reduzir recursos em períodos de baixa utilização, otimizando custos operacionais em nuvem.
