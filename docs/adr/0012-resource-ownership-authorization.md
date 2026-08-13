# ADR-0012 — Autorização por propriedade do recurso

- Status: Aceito
- Data: 2026-08-12

## Contexto

Autenticar um usuário não impede, por si só, que ele consulte ou movimente recursos de outra pessoa. Contas e transferências precisam ser isoladas antes da publicação do PayFlow.

## Decisão

Cada conta possui um `owner_id` obrigatório referenciando `users`. Cadastro cria duas carteiras fictícias para o novo usuário. Endpoints de contas e transferências exigem JWT e obtêm o proprietário exclusivamente do claim `sub`, nunca de um campo enviado pelo cliente.

Consultas retornam somente contas do usuário e transferências em que uma de suas contas participou. Uma transferência somente pode debitar uma conta pertencente ao usuário autenticado; a conta de destino pode pertencer a ele ou a outro usuário.

Quando um identificador existe, mas pertence a outra pessoa, a API responde `404`, evitando revelar a existência do recurso.

## Consequências

O isolamento é aplicado no servidor e não depende da interface. O front-end exige login e mantém o access token de curta duração no `sessionStorage`: recarregar a mesma aba restaura a sessão, enquanto fechar a aba remove essa persistência. Na inicialização, uma chamada protegida valida o token; uma resposta `401` limpa a sessão e retorna ao login. Não usamos `localStorage`, cuja persistência continuaria após fechar o navegador.

O cadastro não autentica automaticamente: depois do `201 Created`, a interface retorna ao login e somente esse endpoint pode emitir o JWT. O `sessionStorage`, assim como qualquer armazenamento acessível por JavaScript, pode ser lido em caso de XSS. Esta é uma decisão incremental do M1, mitigada por tokens de 15 minutos; refresh token rotativo em cookie `HttpOnly` permanece planejado para o M2.

As três contas legadas recebem um proprietário técnico sem credenciais válidas durante a migration. Novos usuários recebem duas contas próprias para manter a demonstração de transferência interna executável.
