# Changelog

Todas as mudanças relevantes do PayFlow serão registradas neste arquivo. O formato segue
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Não publicado]

### Adicionado

- endpoint idempotente para estorno integral de transferências entre contas do mesmo usuário;
- tipo `REVERSAL` vinculado à transferência original, sem alterar o registro concluído;
- migration com constraints e índice único para impedir vínculos inválidos e estorno duplicado;
- testes com PostgreSQL real para saldo, autorização, idempotência e concorrência do estorno;
- evento `TransferReversed.v1` gravado na outbox na mesma transação do estorno;
- projeção de auditoria com `original_transfer_id` pesquisável para eventos de estorno;

### Segurança

- estornos não podem debitar unilateralmente uma conta pertencente a outro usuário;
- a rota de estorno compartilha o rate limit autenticado das transferências.

### Alterado

- o tópico Kafka local passa a se chamar `payflow.transfer-events.v1` para transportar contratos de
  conclusão e estorno sem publicar eventos em um tópico semanticamente incorreto.

## [0.4.0] - 2026-09-29

### Adicionado

- evento versionado `TransferCompleted.v1`, sem dados pessoais;
- outbox transacional gravada atomicamente com a transferência;
- Kafka local opcional em modo KRaft e relay interno para publicar a outbox;
- consumidor de auditoria dentro do monólito, com deduplicação persistente por consumidor e evento;
- projeção de auditoria em PostgreSQL e tópico de dead letter para falhas persistentes;
- métricas de publicação, consumo, deduplicação, esgotamento e retenção de eventos.

### Alterado

- o relay limita a publicação a cinco tentativas e preserva a última falha para diagnóstico;
- o consumidor executa duas retentativas antes de encaminhar a mensagem ao tópico DLT;
- outboxes publicadas e mensagens Kafka possuem retenção padrão de sete dias;
- logs de publicação e auditoria reutilizam o `correlationId` da requisição original.

### Segurança

- Kafka permanece local, opcional e fora do runtime AWS;
- eventos não carregam nome, e-mail, senha, token ou outros dados pessoais;
- identificadores de evento e correlação não são usados como tags de métricas.

### Observações

- PostgreSQL continua sendo a fonte de verdade de saldos e transferências;
- Kafka executa somente efeitos secundários depois da confirmação financeira;
- a entrega é pelo menos uma vez e os consumidores precisam permanecer idempotentes;
- a adoção de Kafka em cloud depende de decisão arquitetural e análise de custo posteriores.

## [0.3.0] - 2026-09-28

### Adicionado

- paginação e filtros por período e status no histórico de transferências;
- testes concorrentes com PostgreSQL real para saldo e idempotência;
- métricas de negócio para transferências concluídas, repetidas, recusadas e com falha;
- medição da duração das tentativas de transferência com cardinalidade controlada;
- alarmes CloudWatch efêmeros para falha de status da EC2 e CPU elevada;
- documentação operacional de observabilidade e ADRs sobre concorrência e métricas.

### Alterado

- transferências concorrentes passam a ser verificadas explicitamente contra débito duplicado e saldo
  negativo;
- permissões da role de infraestrutura incluem o ciclo de vida dos alarmes CloudWatch;
- lease efêmero de cada ambiente passa a usar um caminho específico no bucket de state.

### Segurança

- métricas do Actuator permanecem fora das rotas públicas do Caddy;
- identificadores de usuário, conta e transferência não são usados como tags de métricas;
- contadores de conclusão e replay são incrementados somente depois do commit da transação.

### Observações

- alarmes e aplicação continuam vinculados ao TTL do ambiente efêmero;
- a primeira validação da candidata ocorre em homologação antes da promoção para produção.

## [0.2.0] - 2026-09-27

### Adicionado

- política de promoção do mesmo artefato entre `development`, `homologation` e `production`;
- infraestrutura AWS e identidades OIDC isoladas para desenvolvimento e homologação;
- deploy efêmero de desenvolvimento acionado pela branch `develop`;
- deploy efêmero de homologação acionado por branches `release/*`;
- validação documentada da criação, acesso e destruição do ambiente de desenvolvimento;
- proteção de branches de release com Pull Request e checks obrigatórios.

### Alterado

- workflows de infraestrutura e aplicação agora resolvem conta, estado remoto, IAM e configuração
  pelo ambiente selecionado;
- revisão de produção exige uma tag anotada e imutável, enquanto ambientes anteriores usam o SHA
  do commit promovido.

### Segurança

- cada ambiente possui papéis IAM, variáveis e segredos próprios no GitHub;
- autenticação dos workflows permanece sem chaves AWS persistentes, usando OIDC e credenciais
  temporárias.

### Observações

- desenvolvimento foi validado ponta a ponta e destruído após o teste;
- a candidata `release/0.2.0` foi publicada e aprovada funcionalmente em homologação antes da
  promoção para produção.

## [0.1.0] - 2026-09-22

### Adicionado

- fluxo completo de cadastro, login e autenticação stateless com JWT;
- contas pertencentes ao usuário autenticado, saldo fictício e histórico de transferências;
- transferência atômica, imutável e protegida por chave de idempotência;
- dashboard React responsivo organizado por features e rotas protegidas;
- API Spring Boot documentada com OpenAPI e Swagger UI;
- PostgreSQL com migrations Flyway e dados de demonstração;
- testes unitários, de integração, componentes e fluxo E2E com Playwright;
- cobertura com Vitest/V8 e JaCoCo, análise SonarQube Cloud e Quality Gate;
- logs JSON estruturados, correlation ID, health checks e rate limiting;
- Docker Compose local e runtime de produção com Caddy;
- infraestrutura AWS reproduzível com Terraform, OIDC e Systems Manager;
- publicação AWS efêmera com TTL, lease, watchdog de limpeza e alerta de orçamento;
- Git Flow com branches protegidas e CI para `develop`, `release/*` e `main`.

### Segurança

- senhas protegidas com BCrypt e segredos fornecidos fora do repositório;
- autorização por propriedade do recurso e CORS explícito por ambiente;
- credenciais AWS temporárias por IAM Identity Center e GitHub OIDC;
- API e PostgreSQL sem portas públicas no runtime de produção.

### Observações

- o sistema utiliza somente valores e contas fictícias;
- a demonstração pública não movimenta dinheiro real;
- os ambientes AWS permanecem desligados por padrão e são removidos depois do TTL.

[0.4.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.4.0
[0.3.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.3.0
[0.2.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.2.0
[0.1.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.1.0
