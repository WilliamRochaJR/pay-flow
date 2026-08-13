# ADR 0014 — Roteamento explícito no front-end

## Status

Aceito.

## Decisão

Usar React Router para representar cada tela navegável por um caminho explícito. `/login` e
`/register` são rotas públicas; `/dashboard` é protegida e exige uma sessão válida. `/` apenas
direciona para a tela compatível com o estado da autenticação e caminhos desconhecidos retornam a
esse ponto de entrada.

Usuários autenticados não permanecem nas páginas de login ou cadastro. Usuários sem sessão que
tentam abrir o dashboard são direcionados ao login. Cadastro concluído navega para `/login`, login
concluído navega para `/dashboard` e logout volta para `/login`.

## Consequências

A URL passa a descrever a tela atual, o histórico do navegador funciona e novas páginas podem ser
adicionadas sem concentrar toda a navegação em estados condicionais do `App`. Como o React continua
sendo uma SPA, o servidor web precisa devolver `index.html` para rotas que não correspondem a um
arquivo físico; o Nginx já faz isso com `try_files $uri $uri/ /index.html`.

As rotas melhoram navegação, mas não substituem autorização. A API continua validando o JWT e o
proprietário de cada recurso.
