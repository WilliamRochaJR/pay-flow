# Contrato inicial da API

Prefixo: `/api/v1`.

## Documentação interativa

Com a API em execução:

```text
Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
```

O Swagger UI permite consultar e experimentar o contrato HTTP pelo navegador. A especificação OpenAPI é gerada a partir dos controllers, DTOs e anotações do código.

## M0 local

### Contas

```http
GET /api/v1/accounts
GET /api/v1/accounts/{accountId}
```

### Transferências

```http
POST /api/v1/transfers
GET  /api/v1/transfers
GET  /api/v1/transfers/{transferId}
```

A listagem é paginada e ordenada da transferência mais recente para a mais antiga:

```http
GET /api/v1/transfers?page=0&size=5&from=2026-09-01T00:00:00Z&to=2026-09-30T23:59:59.999Z
```

- `page`: página baseada em zero;
- `size`: entre 1 e 50, com padrão 5;
- `from` e `to`: instantes ISO-8601 opcionais e inclusivos;
- `status`: filtro opcional preparado para os estados do domínio; atualmente o único valor é
  `COMPLETED`.

A resposta contém `content`, `page`, `size`, `totalElements`, `totalPages`, `first` e `last`. Isso
evita carregar um histórico ilimitado no navegador e mantém o contrato independente da representação
interna do Spring Data.

Exemplo de criação:

```json
{
  "sourceAccountId": "5b99802c-24c0-4462-8260-6317a984da20",
  "destinationAccountId": "565620a5-e66d-48c9-8ff2-39aa22ace194",
  "amount": 350.0,
  "currency": "BRL"
}
```

Resposta `201 Created`:

```json
{
  "id": "7e2cb1ed-c44f-4cb2-9495-b1ca81042c5a",
  "type": "INTERNAL_TRANSFER",
  "amount": 350.0,
  "currency": "BRL",
  "status": "COMPLETED",
  "createdAt": "2026-08-10T22:00:00Z"
}
```

## Adições do M1 público

```http
POST /api/v1/auth/register
POST /api/v1/auth/login
GET  /api/v1/me
```

### Cadastro

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "name": "William Rocha",
  "email": "william@example.com",
  "password": "senha-com-no-minimo-8-caracteres"
}
```

Retorna `201 Created`. A senha nunca aparece na resposta e é persistida somente como hash BCrypt.
O cadastro não emite JWT nem inicia uma sessão. Após a criação, o front-end retorna ao formulário
de login com o e-mail preenchido; somente o login bem-sucedido libera o dashboard.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "william@example.com",
  "password": "senha-com-no-minimo-8-caracteres"
}
```

Resposta:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### Usuário autenticado

```http
GET /api/v1/me
Authorization: Bearer <jwt>
```

Sem um token válido, retorna `401 Unauthorized`.

Contas e transferências também exigem Bearer JWT. A API lista apenas recursos visíveis ao usuário autenticado e somente permite débito em uma conta de sua propriedade.

`POST /transfers` passa a exigir `Authorization: Bearer <token>` e `Idempotency-Key: <uuid>`. A conta de origem deve pertencer ao usuário autenticado.

A primeira chamada cria a transferência e retorna `201 Created`. Repetir a mesma chave com o mesmo
corpo retorna a transferência original sem alterar os saldos novamente. Reutilizar a chave com um
corpo diferente retorna `409 Conflict`. A chave é isolada por usuário autenticado.

## Erros

Usar `application/problem+json` (Problem Details), com `type`, `title`, `status`, `detail` e `instance`. Validações de campo podem acrescentar `errors`.

Status principais: `400` entrada inválida, `401` não autenticado, `403` sem acesso ao recurso, `404` inexistente, `409` conflito/idempotência e `422` saldo insuficiente.

## Correlação de requisições

O cliente pode enviar `X-Correlation-ID: <uuid>`. A API preserva UUIDs válidos ou gera um novo e
sempre devolve o identificador no mesmo header. Erros de negócio e validação tratados também incluem
`correlationId` no corpo `application/problem+json`. Esse valor pode ser informado ao suporte para
localizar a requisição nos logs, mas não concede acesso a nenhum recurso.

## Limite de requisições

Login e cadastro possuem limite por IP; transferências possuem limite por usuário autenticado. Ao
exceder o limite, a API retorna `429 Too Many Requests`, um `ProblemDetail` com `correlationId` e o
header `Retry-After`, indicando quantos segundos aguardar. Os limites são uma proteção operacional e
não alteram as regras de autorização.

Não haverá `PUT` ou `DELETE` de transferências. Uma operação financeira concluída é um registro
histórico; o ADR-0025 define o estorno como uma nova operação vinculada à original.

## Estorno integral — M5.1

O ADR-0025 define o contrato:

```http
POST /api/v1/transfers/{transferId}/reversals
Authorization: Bearer <token>
Idempotency-Key: <uuid>
```

O endpoint não recebe corpo. Ele cria uma nova transferência `REVERSAL`, com contas invertidas e o
mesmo valor e moeda da original. A transferência original permanece imutável. Nesta primeira etapa,
o estorno é permitido somente quando as duas contas continuam pertencendo ao usuário autenticado.

A primeira tentativa retorna `201 Created`. Repetir a mesma `Idempotency-Key` devolve o mesmo estorno
sem movimentar os saldos novamente. Uma chave diferente para uma transferência já estornada retorna
`409 Conflict`. Saldo insuficiente na conta que devolveria o valor retorna `422 Unprocessable Entity`.

A resposta usa `type: "REVERSAL"` e informa `originalTransferId`. Transferências comuns usam
`type: "INTERNAL_TRANSFER"` e não possuem vínculo de origem.

Exemplo de resposta:

```json
{
  "id": "47bfd5bf-61c8-4559-97d6-b143779b762c",
  "type": "REVERSAL",
  "originalTransferId": "7e2cb1ed-c44f-4cb2-9495-b1ca81042c5a",
  "sourceAccountId": "565620a5-e66d-48c9-8ff2-39aa22ace194",
  "destinationAccountId": "5b99802c-24c0-4462-8260-6317a984da20",
  "amount": 350.0,
  "currency": "BRL",
  "status": "COMPLETED",
  "createdAt": "2026-10-01T16:00:00Z"
}
```

O OpenAPI declara explicitamente as respostas `201`, `400`, `401`, `404`, `409` e `422` desse
endpoint. O Swagger UI permite autenticar com JWT e experimentar o contrato localmente.

Na mesma transação que conclui o estorno, a outbox grava `TransferReversed.v1`. O contrato inclui
`eventId`, `eventType`, `eventVersion`, `occurredAt`, `correlationId`, `transferId`,
`originalTransferId`, contas, valor e moeda. O relay publica transferências e estornos no tópico local
`payflow.transfer-events.v1`; o consumidor os deduplica e preserva a projeção em `audit_events`.
