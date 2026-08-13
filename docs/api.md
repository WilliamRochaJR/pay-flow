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

Não haverá `PUT` ou `DELETE` de transferências. Uma operação financeira concluída é um registro histórico; estorno será um novo caso de uso futuro.
