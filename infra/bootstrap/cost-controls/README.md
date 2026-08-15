# Controles persistentes de custo

Este bootstrap cria um AWS Budget mensal para a conta. Ele é persistente: o workflow efêmero não o
destrói. O Budget envia alertas de custo real em 50% e 80% e de custo previsto em 100% do limite.

AWS Budgets não bloqueia cobranças e os dados de custo podem levar tempo para serem atualizados.

## Valor sensível

O endereço de e-mail não pertence ao repositório. Localmente, forneça-o pelo ambiente:

```bash
export TF_VAR_alert_email="seu-email@example.com"
```

No GitHub, use o secret `AWS_BUDGET_ALERT_EMAIL` no Environment `production` e atribua seu valor a
`TF_VAR_alert_email` apenas no step do Terraform. O e-mail ficará no state remoto privado e no AWS
Budgets, embora não apareça nos arquivos versionados.

## Validar e planejar

```bash
terraform init -backend-config=backend.hcl
terraform validate
terraform plan
```

Revise o plano antes de executar `terraform apply`. Criar o Budget é uma operação separada da
publicação temporária.
