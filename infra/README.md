# Infraestrutura AWS

Esta pasta contém a fundação mínima e reproduzível do primeiro deploy público do PayFlow.

```text
infra/
├── bootstrap/
│   ├── cost-controls/
│   ├── identity/
│   └── state/
└── environments/
    └── production/
        ├── backend.hcl.example
        ├── main.tf
        ├── outputs.tf
        ├── providers.tf
        ├── terraform.tfvars.example
        ├── user-data.sh.tftpl
        └── variables.tf
```

O bootstrap cria o bucket persistente de state, o orçamento da conta e a identidade OIDC de deploy. O módulo de produção cria VPC, subnet pública,
Internet Gateway, Security Group, EC2, Elastic IP, perfil IAM da instância e bucket privado para
backups. Ele não executa o deploy da aplicação e não cria recursos ao rodar os testes da CI.

Consulte [production/README.md](environments/production/README.md) antes de executar Terraform.

## Ordem de preparação

1. `bootstrap/state`: bucket persistente do Terraform (já deve existir antes dos demais módulos);
2. `bootstrap/cost-controls`: orçamento persistente e alertas;
3. `bootstrap/identity`: roles OIDC separadas para infraestrutura e deploy;
4. `environments/production`: recursos efêmeros criados e destruídos pelo workflow manual.

Os três primeiros itens não pertencem ao TTL. Nenhum workflow de Pull Request executa `apply`.
