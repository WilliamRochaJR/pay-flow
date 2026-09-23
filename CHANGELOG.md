# Changelog

Todas as mudanças relevantes do PayFlow serão registradas neste arquivo. O formato segue
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o projeto usa
[Semantic Versioning](https://semver.org/lang/pt-BR/).

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

[0.1.0]: https://github.com/WilliamRochaJR/pay-flow/releases/tag/v0.1.0
