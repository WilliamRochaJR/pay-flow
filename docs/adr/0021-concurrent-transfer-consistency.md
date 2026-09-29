# ADR-0021 — Consistência de transferências concorrentes

## Status

Aceito.

## Contexto

Duas requisições podem tentar debitar a mesma conta ao mesmo tempo. Se ambas lerem o saldo antigo e
forem confirmadas independentemente, o sistema poderá gastar o mesmo saldo duas vezes. Repetições
simultâneas da mesma `Idempotency-Key` também não podem criar duas transferências.

## Decisão

Executar débito, crédito e criação da transferência em uma única transação PostgreSQL. Antes de
validar o saldo, carregar origem e destino com lock pessimista de escrita (`PESSIMISTIC_WRITE`). A
consulta ordena as contas por UUID, garantindo que operações inversas tentem adquirir locks na mesma
ordem e reduzindo o risco de deadlock.

Manter `Account.version` como defesa adicional de concorrência otimista. Para idempotência, conservar
o advisory lock transacional definido no ADR-0015 e o índice único parcial sobre
`(owner_id, idempotency_key)`.

Testar as garantias contra PostgreSQL real por meio de Testcontainers. Os testes usam duas threads,
uma barreira de prontidão e uma largada simultânea para cobrir:

- duas operações que disputam um saldo suficiente para apenas uma delas;
- duas requisições iguais com a mesma chave de idempotência.

## Consequências

Uma transferência concorrente pode aguardar a anterior liberar os locks. Esse custo é aceitável para
o volume da PoC e mantém a regra financeira simples e consistente. A solução depende de recursos do
PostgreSQL e não deve ser substituída por verificações somente em memória, pois múltiplas instâncias da
API não compartilhariam esse estado.

Os testes concorrentes são mais caros que testes unitários, por isso permanecem na camada de
integração e executam na CI com banco real.
