# ADR-0019 — PoC AWS efêmera controlada por TTL

- Status: Aceito
- Data: 2026-08-15

## Contexto

O PayFlow precisa de uma demonstração pública do M1, mas não precisa permanecer disponível
continuamente. Manter EC2, volume e endereço IPv4 público ativos sem uso gera custo desnecessário,
inclusive quando algum item estiver dentro do Free Tier. O projeto ainda não possui domínio.

## Decisão

Publicar a PoC por um workflow manual do GitHub Actions, protegido pelo Environment `production`. A
execução provisiona a infraestrutura temporária com Terraform, implanta a revisão selecionada, valida
o endpoint de saúde e mantém a aplicação disponível por um TTL de 20 minutos por padrão e 60 minutos
no máximo. O TTL começa após a aplicação ficar saudável.

Ao final, inclusive depois de falhas, o workflow executa `terraform destroy`. Um workflow separado de
limpeza de emergência poderá ser acionado manualmente, e uma execução agendada removerá ambientes cuja
concessão tenha expirado. A concessão contém somente metadados operacionais e fica no bucket privado de
estado.

Permanecem fora do ciclo de destruição:

- bucket e histórico do estado remoto;
- provider OIDC e roles do GitHub Actions;
- orçamento e assinatura de alerta da conta;
- registros de concessão usados pela limpeza de emergência.

Enquanto não existir domínio, o Caddy atenderá por HTTP no Elastic IP temporário. HTTPS será ativado
quando um domínio puder ser validado; não será usado certificado inválido ou aviso de segurança no
navegador para aparentar HTTPS.

O e-mail do alerta de orçamento será recebido por uma variável sensível. O valor não será versionado,
mas ficará armazenado no estado Terraform privado e no AWS Budgets, onde administradores autorizados
da conta poderão consultá-lo.

## Consequências

A demonstração tem custo e exposição reduzidos e seu desligamento é reproduzível. Ela deixa de estar
disponível depois do TTL, perde o banco temporário no `destroy` e recebe outro IP na próxima execução.
O link precisa ser obtido na execução correspondente do workflow.

GitHub Actions e AWS podem falhar de forma independente. Por isso o bloco de limpeza do workflow não é
a única proteção: o watchdog agendado deve conferir a concessão persistida. Ainda assim, AWS Budgets é
um alerta, não um limitador automático de cobrança, e não garante custo zero.

Esta decisão especializa o ADR-0004 para a fase de portfólio. A topologia de uma única EC2 permanece;
somente seu ciclo de vida passa a ser temporário.
