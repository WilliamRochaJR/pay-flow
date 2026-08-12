# Contrato inicial da API

Prefixo: `/api/v1`.

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

`POST /transfers` passa a exigir `Authorization: Bearer <token>` e `Idempotency-Key: <uuid>`. A conta de origem deve pertencer ao usuário autenticado.

## Erros

Usar `application/problem+json` (Problem Details), com `type`, `title`, `status`, `detail` e `instance`. Validações de campo podem acrescentar `errors`.

Status principais: `400` entrada inválida, `401` não autenticado, `403` sem acesso ao recurso, `404` inexistente, `409` conflito/idempotência e `422` saldo insuficiente.

Não haverá `PUT` ou `DELETE` de transferências. Uma operação financeira concluída é um registro histórico; estorno será um novo caso de uso futuro.
