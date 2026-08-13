# ADR 0015 — Idempotência na criação de transferências

## Status

Aceito.

## Decisão

Exigir o header `Idempotency-Key`, no formato UUID, em toda criação de transferência. A chave é
associada ao usuário autenticado e à transferência persistida. O PostgreSQL garante unicidade do par
`(owner_id, idempotency_key)`.

Antes da consulta, a transação obtém um advisory lock derivado do usuário e da chave. Requisições
simultâneas para o mesmo par são processadas em sequência; o índice único permanece como garantia
final no banco. O lock dura somente até o fim da transação e não cria registros auxiliares.

Quando a mesma chave é repetida com os mesmos dados, a API devolve a transferência original sem
movimentar os saldos novamente. Se os dados forem diferentes, responde `409 Conflict`. Usuários
diferentes podem usar o mesmo UUID, pois a chave não é global.

O front-end gera a chave no primeiro envio e a conserva quando o resultado é incerto. A chave é
descartada após sucesso ou quando os dados da tentativa mudam.

## Consequências

Retries causados por timeout ou perda da resposta deixam de duplicar uma operação financeira. A
chave e o usuário passam a fazer parte do registro imutável da transferência. Registros anteriores
à decisão não possuem chave, mas recebem proprietário na migration.
