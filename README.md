# PayFlow

PayFlow é uma aplicação de portfólio para demonstrar um fluxo financeiro completo: autenticação, contas, transferências, persistência, publicação em cloud e, em etapas posteriores, processamento assíncrono e auditoria.

O projeto começa intencionalmente pequeno:

```text
React + TypeScript -> Spring Boot -> PostgreSQL
```

A primeira versão pública será um **monólito modular**, executado com Docker Compose em uma única instância AWS. Kafka, MongoDB, WebFlux e serviços separados só entram quando existir um caso de uso que justifique cada tecnologia.

## M0 disponível localmente

O primeiro fluxo vertical já inclui contas fictícias, transferência atômica, atualização de saldos e histórico.

### Executar tudo com Docker

Pré-requisito: Docker com Compose.

```bash
docker compose up --build
```

Acesse <http://localhost:5173>. A API fica em <http://localhost:8080/api/v1> e seu health check em <http://localhost:8080/actuator/health>.

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
cd backend && mvn test
cd frontend && npm test && npm run lint && npm run build
```

Os testes de integração do back-end usam Testcontainers e, portanto, precisam do Docker ativo.

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
- [Contrato inicial da API](docs/api.md)
- [Decisões arquiteturais](docs/adr/README.md)

## Estado

M0 implementado para execução local. O próximo marco é o M1: autenticação, endurecimento operacional e publicação na AWS.
