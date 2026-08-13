# ADR-0011 — Autenticação stateless com JWT

- Status: Aceito
- Data: 2026-08-12

## Contexto

O primeiro release público precisa identificar usuários sem introduzir neste momento um servidor de autorização externo ou armazenamento de sessões. A API e o front-end são partes do mesmo produto e existe apenas uma instância planejada para o primeiro deploy.

## Decisão

Usar Spring Security como Resource Server para validar access tokens JWT assinados com HS256. A própria API cadastra usuários, verifica senhas com BCrypt e emite tokens com validade inicial de 15 minutos.

A chave de assinatura vem de `JWT_SECRET` em Base64, deve possuir ao menos 256 bits e não pode ser versionada com valores de produção. O token contém o UUID do usuário no claim `sub`, além de `iss`, `iat`, `exp` e e-mail.

Cadastro e login são públicos. `/api/v1/me` exige Bearer JWT. Durante a transição, contas e transferências do M0 permanecem públicas até que sejam vinculadas ao usuário em uma entrega própria.

## Consequências

A API não mantém sessão no servidor e pode validar tokens sem consulta ao banco em cada filtro de segurança. Revogação imediata e refresh token não fazem parte desta etapa; tokens curtos reduzem o período de exposição. A rotação da chave invalida todos os tokens existentes.

HS256 é suficiente para a primeira aplicação única, mas compartilha o mesmo segredo para emissão e validação. Se emissão e consumo forem separados no futuro, deverá ser reavaliada a adoção de chaves assimétricas ou de um provedor de identidade.
