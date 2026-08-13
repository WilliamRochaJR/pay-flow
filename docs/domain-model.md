# Modelo de classes

Este documento representa o modelo **implementado atualmente no M0**. Ele separa entidades persistidas, objetos do contrato HTTP e serviços responsáveis pelo fluxo de transferência.

## Visão geral das classes

```mermaid
classDiagram
    direction LR

    class Account {
        -UUID id
        -String holderName
        -String currency
        -BigDecimal balance
        -long version
        +getId() UUID
        +getHolderName() String
        +getCurrency() Currency
        +getBalance() BigDecimal
        +debit(amount) void
        +credit(amount) void
    }

    class AccountResponse {
        +UUID id
        +String holderName
        +BigDecimal balance
        +String currency
        +from(account) AccountResponse
    }

    class Transfer {
        -UUID id
        -UUID sourceAccountId
        -UUID destinationAccountId
        -BigDecimal amount
        -String currency
        -TransferStatus status
        -Instant createdAt
        +completed(sourceId, destinationId, amount, currency) Transfer
    }

    class TransferStatus {
        <<enumeration>>
        COMPLETED
    }

    class CreateTransferRequest {
        +UUID sourceAccountId
        +UUID destinationAccountId
        +BigDecimal amount
        +String currency
    }

    class TransferResponse {
        +UUID id
        +String type
        +UUID sourceAccountId
        +UUID destinationAccountId
        +BigDecimal amount
        +String currency
        +TransferStatus status
        +Instant createdAt
        +from(transfer) TransferResponse
    }

    class TransferService {
        +create(request) TransferResponse
        +list() List
        +find(id) TransferResponse
    }

    class User {
        -UUID id
        -String name
        -String email
        -String passwordHash
        -Instant createdAt
        +register(name, email, passwordHash) User
    }

    class AuthService {
        +register(request) UserResponse
        +login(request) TokenResponse
        +findById(subject) UserResponse
    }

    class TokenService {
        +issue(user) TokenResponse
    }

    Account "1" --> "0..*" Transfer : origem de
    Account "1" --> "0..*" Transfer : destino de
    AccountResponse "1" ..> "1" Account : converte uma
    Transfer "0..*" --> "1" TransferStatus : possui
    TransferResponse "1" ..> "1" Transfer : converte uma
    TransferResponse "0..*" --> "1" TransferStatus : expõe
    TransferService ..> CreateTransferRequest : recebe
    TransferService ..> TransferResponse : devolve
    TransferService ..> Account : debita e credita
    TransferService ..> Transfer : cria e persiste
    AuthService ..> User : cadastra e consulta
    AuthService ..> TokenService : solicita token
    TokenService ..> User : usa identidade
```

### Legenda das cardinalidades e relacionamentos

| Símbolo | Leitura                                                                               |
| ------- | ------------------------------------------------------------------------------------- |
| `1`     | exatamente uma instância                                                              |
| `0..1`  | nenhuma ou uma instância                                                              |
| `0..*`  | nenhuma, uma ou muitas instâncias                                                     |
| `-->`   | associação: uma classe possui ou está relacionada à outra                             |
| `..>`   | dependência: uma classe usa a outra temporariamente, por exemplo para converter dados |

Exemplos do diagrama:

- cada `Transfer` possui exatamente um `TransferStatus`;
- um mesmo `TransferStatus`, como `COMPLETED`, pode aparecer em zero ou muitas transferências;
- cada transferência possui exatamente uma conta de origem e uma conta de destino;
- uma conta pode não ter nenhuma transferência ou participar de muitas;
- cada `TransferResponse` é criado a partir de exatamente uma `Transfer`.

As duas associações entre `Account` e `Transfer` representam o relacionamento do domínio e do banco. No código Java, `Transfer` armazena `sourceAccountId` e `destinationAccountId` como UUIDs; ela não mantém campos do tipo `Account` nem associações JPA como `@ManyToOne`.

As dependências do `TransferService` não recebem cardinalidade porque representam chamadas temporárias durante um caso de uso, e não objetos mantidos como parte permanente do modelo.

## Entidades persistidas

`User`, `Account` e `Transfer` são entidades JPA. Seus dados são armazenados respectivamente nas tabelas `users`, `accounts` e `transfers`.

```mermaid
erDiagram
    ACCOUNTS ||--o{ TRANSFERS : "conta de origem"
    ACCOUNTS ||--o{ TRANSFERS : "conta de destino"

    ACCOUNTS {
        UUID id PK
        VARCHAR holder_name
        VARCHAR currency
        NUMERIC balance
        BIGINT version
    }

    USERS {
        UUID id PK
        VARCHAR name
        VARCHAR email UK
        VARCHAR password_hash
        TIMESTAMPTZ created_at
    }

    TRANSFERS {
        UUID id PK
        UUID source_account_id FK
        UUID destination_account_id FK
        NUMERIC amount
        VARCHAR currency
        VARCHAR status
        TIMESTAMPTZ created_at
    }
```

Uma conta pode participar de zero ou muitas transferências como origem e de zero ou muitas como destino. No Java, `Transfer` guarda apenas os identificadores das contas, em vez de carregar objetos `Account`. Isso mantém o registro financeiro simples e evita associações JPA desnecessárias.

## Responsabilidade das classes

| Classe                  | Tipo                 | Responsabilidade                                               |
| ----------------------- | -------------------- | -------------------------------------------------------------- |
| `Account`               | entidade JPA         | manter saldo e aplicar as regras de débito e crédito           |
| `Transfer`              | entidade JPA         | representar o registro imutável de uma transferência concluída |
| `TransferStatus`        | enum                 | limitar os estados válidos da transferência                    |
| `CreateTransferRequest` | DTO de entrada       | receber e validar os dados enviados pelo cliente               |
| `AccountResponse`       | DTO de saída         | expor uma conta sem devolver diretamente a entidade JPA        |
| `TransferResponse`      | DTO de saída         | expor uma transferência no contrato HTTP                       |
| `TransferService`       | serviço de aplicação | coordenar validações, débito, crédito e persistência atômica   |
| `User`                  | entidade JPA         | armazenar identidade, e-mail normalizado e hash da senha       |
| `AuthService`           | serviço de aplicação | coordenar cadastro, login e consulta do usuário                |
| `TokenService`          | serviço de segurança | emitir access token JWT com validade curta                     |

DTO significa **Data Transfer Object**: objeto usado para transportar dados entre a API e seus consumidores. Separar DTOs das entidades impede que mudanças internas do banco alterem acidentalmente o contrato HTTP.

## Fluxo de criação de transferência

```mermaid
sequenceDiagram
    actor Client as Front-end/cliente
    participant Controller as TransferController
    participant Service as TransferService
    participant Accounts as AccountRepository
    participant Source as Account origem
    participant Destination as Account destino
    participant Transfers as TransferRepository

    Client->>Controller: POST /api/v1/transfers
    Controller->>Service: create(CreateTransferRequest)
    Service->>Accounts: buscar e bloquear as duas contas
    Accounts-->>Service: contas encontradas
    Service->>Source: debit(amount)
    Service->>Destination: credit(amount)
    Service->>Transfers: save(Transfer.completed(...))
    Transfers-->>Service: Transfer persistida
    Service-->>Controller: TransferResponse
    Controller-->>Client: 201 Created
```

O método do serviço é transacional: débito, crédito e criação da transferência formam uma única operação. Se uma regra falhar, como saldo insuficiente, nenhuma parte deve permanecer gravada.

## Regras representadas no modelo

- dinheiro usa `BigDecimal` no Java e `NUMERIC(19,2)` no PostgreSQL;
- `Account.debit` impede que o saldo fique negativo;
- origem e destino devem ser contas diferentes;
- valor deve ser maior que zero e ter no máximo duas casas decimais;
- as duas contas devem usar a moeda informada;
- uma transferência criada neste marco recebe o estado `COMPLETED`;
- transferências concluídas não possuem operação de alteração ou exclusão;
- `Account.version` oferece suporte a controle de concorrência otimista pelo JPA.

## Evolução planejada

O modelo acima mostra somente o que existe no código. A associação entre `User` e `Account`, a chave de idempotência e o estorno pertencem aos próximos passos do M1 e devem ser adicionados ao diagrama apenas quando forem implementados.
