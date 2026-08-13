# ADR 0017 — Rate limiting em memória no primeiro deploy

## Status

Aceito.

## Decisão

Aplicar uma janela fixa configurável em memória: login e cadastro são limitados por endereço IP;
transferências, pelo identificador do usuário autenticado. Ao exceder o limite, a API responde `429
Too Many Requests`, `Retry-After` em segundos e um `ProblemDetail` com `correlationId`.

Os limites iniciais por minuto são 10 logins, 5 cadastros e 30 transferências. Eles podem ser
alterados por variáveis de ambiente sem rebuild. Entradas expiradas são removidas periodicamente.

Headers encaminhados pelo proxy não são confiáveis por padrão. `X-Forwarded-For` somente participa
da identificação quando `RATE_LIMIT_TRUST_FORWARDED_HEADERS=true` e a API está isolada atrás de um
proxy controlado. A porta da API não deve ficar exposta publicamente nesse cenário.

## Consequências

A solução é pequena e suficiente para o único processo do M1. Reiniciar a API zera os contadores e
múltiplas réplicas teriam limites independentes. Antes de escalar horizontalmente, o estado será
movido para um armazenamento compartilhado, como Redis, ou para o rate limiting da borda.

Rate limiting reduz abuso e custo, mas não substitui autenticação, bloqueio adaptativo, WAF ou
monitoramento de fraude.
