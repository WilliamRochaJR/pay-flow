# ADR-0006 — Front-end organizado por funcionalidades

- Status: Aceito
- Data: 2026-08-11

## Contexto

O primeiro dashboard concentrava carregamento de dados, formulário, histórico, formatação e toda a apresentação em `App.tsx`. Embora funcional, essa organização tornava o arquivo difícil de navegar e aumentaria o acoplamento conforme contas e transferências evoluíssem.

Uma Clean Architecture completa, uma biblioteca de estado global ou pastas genéricas para cada tipo técnico adicionariam abstrações sem necessidade no M0.

## Decisão

Organizar o front-end por funcionalidades e casos de uso, seguindo `features/<domínio>/<caso-de-uso>/`. Exemplos atuais são `accounts/list-accounts`, `transfers/create-transfer` e `transfers/list-transfers`.

Dentro do caso de uso, `*.model.ts` representa tipos/dados, `*.service.ts` trata comunicação externa e o arquivo sem sufixo coordena as regras do caso de uso. A pasta `components/` contém componentes internos usados pela apresentação daquele caso. Testes permanecem ao lado do arquivo testado.

Código transversal comprovadamente compartilhado fica em `shared`. A pasta `app` contém a composição da aplicação e coordena o estado compartilhado entre as funcionalidades.

Novas camadas, arquivos e bibliotecas só serão introduzidos quando houver uma necessidade concreta. Não serão criadas pastas vazias para uma arquitetura futura.

## Consequências

Arquivos relacionados a uma funcionalidade ficam próximos, o `App` deixa de conter toda a interface e cada módulo ganha uma responsabilidade mais clara. Alguns dados continuam coordenados no `App`, pois o dashboard ainda é uma única tela. A equipe precisa evitar que `shared` vire um destino genérico para código pertencente a uma feature.
