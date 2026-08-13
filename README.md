# PayFlow

PayFlow é uma aplicação de portfólio para demonstrar um fluxo financeiro completo: autenticação, contas, transferências, persistência, publicação em cloud e, em etapas posteriores, processamento assíncrono e auditoria.

O projeto começa intencionalmente pequeno:

```text
React + TypeScript -> Spring Boot -> PostgreSQL
```

A primeira versão pública será um **monólito modular**, executado com Docker Compose em uma única instância AWS. Kafka, MongoDB, WebFlux e serviços separados só entram quando existir um caso de uso que justifique cada tecnologia.

## M0 disponível localmente

O primeiro fluxo vertical já inclui contas fictícias, transferência atômica, atualização de saldos e histórico.

O fluxo inclui cadastro e login no React, senha protegida por BCrypt, autenticação JWT e contas isoladas por usuário. O token permanece somente em memória e é descartado ao sair ou atualizar a página.

### Executar tudo com Docker

Pré-requisito: Docker com Compose.

```bash
docker compose up --build
```

Acesse <http://localhost:5173>. A API fica em <http://localhost:8080/api/v1>, a documentação interativa em <http://localhost:8080/swagger-ui.html> e o health check em <http://localhost:8080/actuator/health>.

Para encerrar, execute `docker compose down`. Os dados permanecem no volume `payflow-data`. Para também apagar os dados fictícios e recriar o seed, execute `docker compose down -v`.

### Desenvolvimento

Suba somente o PostgreSQL:

```bash
docker compose up -d db
```

O PostgreSQL do PayFlow fica disponível em `localhost:5433`, evitando conflito com instalações locais na porta padrão.

Em dois terminais:

```bash
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

Testes e verificações:

```bash
npm run check
npm run check:e2e
```

`check` executa formatação, tipos, lint, cobertura, testes e builds do front e `mvn verify` no back. `check:e2e` reconstrói a aplicação completa e executa o fluxo Playwright. Os testes de integração e E2E precisam do Docker ativo.

## Primeiro produto publicável

O usuário poderá:

1. criar uma conta e entrar;
2. consultar saldo e histórico;
3. transferir um valor entre duas contas de demonstração;
4. consultar o resultado da transferência.

O release público inclui autenticação JWT, validação de saldo, transferência atômica e uma interface responsiva. Não movimenta dinheiro real e será identificado como ambiente de demonstração.

## Documentação

- [Plano de entrega](docs/plan.md)
- [Arquitetura](docs/architecture.md)
- [Modelo de classes](docs/domain-model.md)
- [Contrato inicial da API](docs/api.md)
- [Decisões arquiteturais](docs/adr/README.md)

## Estado

M0 implementado para execução local. O próximo marco é o M1: autenticação, endurecimento operacional e publicação na AWS.
