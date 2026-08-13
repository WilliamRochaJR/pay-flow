# ADR-0013 — Mesma origem e CORS explícito por ambiente

- Status: Aceito
- Data: 2026-08-12

## Contexto

React e API podem executar em portas diferentes durante o desenvolvimento. Liberar `http://localhost:*` resolve variações de porta, mas amplia desnecessariamente as origens autorizadas e não representa uma configuração adequada para homologação ou produção.

## Decisão

Preferir mesma origem para o navegador. O cliente sempre chama caminhos relativos `/api/*`. No desenvolvimento, o proxy do Vite encaminha essas chamadas para `localhost:8080` com `changeOrigin`. Na publicação, Nginx ou Caddy servirá o front-end e encaminhará `/api/*` internamente para o Spring Boot.

Quando CORS for necessário para acesso direto à API, aceitar somente a origem exata configurada por `APP_CORS_ALLOWED_ORIGIN`. Cada ambiente terá seu próprio valor, como `http://localhost:5173`, `https://hom.payflow.example` ou `https://payflow.example`. Wildcards de host ou porta não serão usados em produção.

## Consequências

O Vite pode usar uma porta alternativa, como `5174`, sem exigir que a API permita todas as portas locais, pois o proxy reescreve a origem antes de encaminhar a chamada. A configuração de produção fica explícita e auditável.

CORS não substitui autenticação ou autorização: JWT e propriedade dos recursos continuam sendo as proteções da API. CORS restringe quais páginas web podem ler respostas pelo navegador, mas não impede chamadas feitas fora dele.
