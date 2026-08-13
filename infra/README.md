# Infraestrutura AWS

Esta pasta contém a fundação mínima e reproduzível do primeiro deploy público do PayFlow.

```text
infra/
├── bootstrap/
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

O bootstrap cria o bucket persistente de state. O módulo de produção cria VPC, subnet pública,
Internet Gateway, Security Group, EC2, Elastic IP, perfil IAM da instância e bucket privado para
backups. Ele não executa o deploy da aplicação e não cria recursos ao rodar os testes da CI.

Consulte [production/README.md](environments/production/README.md) antes de executar Terraform.
