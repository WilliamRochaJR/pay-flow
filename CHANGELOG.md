# Changelog

Todas as mudanças relevantes do PayFlow serão registradas neste arquivo. O formato segue
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Semantic Versioning](https://semver.org/lang/pt-BR/).

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

[0.2.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.2.0
[0.1.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.1.0
