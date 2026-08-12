# Arquitetura

## Arquitetura inicial

```text
Browser
  |
  | HTTPS
  v
Caddy (EC2)
  |--------------------|
  v                    v
React estático     Spring Boot API
                         |
                         v
                    PostgreSQL
```

No desenvolvimento, os mesmos componentes são iniciados por Docker Compose. No primeiro deploy, ficam em uma única instância para reduzir custo, quantidade de recursos e esforço operacional. Containers e módulos mantêm caminhos de evolução sem prometer escalabilidade que a POC ainda não precisa.

## Módulos do back-end

```text
auth          cadastro, login e tokens
accounts      propriedade e consulta de saldo
transfers     regras e execução atômica
shared        erros e infraestrutura transversal mínima
```

Cada módulo pode conter `api`, `application`, `domain` e `infrastructure`, mas pacotes só devem existir quando tiverem conteúdo real. A API chama casos de uso; regras de saldo não ficam em controllers.

## Organização do front-end

```text
src/
├── app/                 composição e estado compartilhado da tela
├── features/
│   ├── accounts/
│   │   └── list-accounts/
│   └── transfers/
│       ├── create-transfer/
│       └── list-transfers/
└── shared/              cliente HTTP, formatadores e estilos globais
```

O front-end é organizado por domínio e caso de uso. Cada caso pode conter modelo, service, coordenação, componentes internos e testes colocalizados. Código começa dentro da feature que o utiliza e só vai para `shared` quando for realmente transversal. Bibliotecas de cache, roteamento e estado global serão adicionadas apenas quando a aplicação apresentar essas necessidades.

## Modelo mínimo

- `users`: identidade e credenciais.
- `accounts`: proprietário, moeda e saldo atual.
- `transfers`: origem, destino, valor, status, chave de idempotência e timestamps.

Restrições essenciais:

- valor maior que zero;
- origem diferente do destino;
- mesma moeda no M0/M1;
- saldo nunca negativo;
- débito e crédito na mesma transação de banco;
- chave de idempotência única por usuário;
- transferência concluída não pode ser editada ou removida.

## Arquitetura futura

```text
React -> Transaction API -> PostgreSQL
                 |
                 v
          Transactional Outbox
                 |
                 v
               Kafka
              /     \
             v       v
        Audit       Notification
        Service       Service
           |
           v
        MongoDB
```

Kafka não participa da confirmação financeira. PostgreSQL continua sendo a fonte de verdade; a outbox impede o intervalo inconsistente entre salvar a transferência e publicar seu evento.

## Segurança e limites

- O sistema representa dinheiro fictício e não processa pagamentos reais.
- Senhas são armazenadas com hash adaptativo suportado pelo Spring Security.
- Autorização é verificada por recurso, não apenas pela presença do token.
- Segredos não ficam em imagens, Compose versionado ou logs.
- Logs não incluem senha, token completo ou dados pessoais desnecessários.
