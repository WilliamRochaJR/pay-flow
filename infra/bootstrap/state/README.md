# Bootstrap do state

Este módulo cria o bucket S3 que armazena o state dos demais módulos. Ele é executado uma vez com
state local porque um backend não pode armazenar seu próprio state antes de existir.

```bash
terraform init
terraform plan -out=bootstrap.tfplan
terraform apply bootstrap.tfplan
terraform output -raw state_bucket_name
```

Depois da criação, copie o nome retornado para `environments/production/backend.hcl`. Preserve o
state local do bootstrap em armazenamento seguro até migrá-lo para o bucket criado. Nunca faça
commit de state ou planos.

O bucket usa criptografia padrão do S3, versionamento, bloqueio completo de acesso público e
`prevent_destroy`. Excluir esse recurso exige uma mudança consciente no código e tratamento das
versões armazenadas.
