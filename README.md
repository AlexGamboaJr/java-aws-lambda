# Java AWS Lambda — Serverless API com DynamoDB

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![AWS Lambda](https://img.shields.io/badge/AWS_Lambda-FF9900?style=flat&logo=awslambda&logoColor=white)
![DynamoDB](https://img.shields.io/badge/DynamoDB-4053D6?style=flat&logo=amazondynamodb&logoColor=white)
![Amazon S3](https://img.shields.io/badge/Amazon_S3-569A31?style=flat&logo=amazons3&logoColor=white)
![AWS SAM](https://img.shields.io/badge/AWS_SAM-FF9900?style=flat&logo=amazonaws&logoColor=white)
![CI](https://github.com/AlexGamboaJunior/java-aws-lambda/actions/workflows/ci.yml/badge.svg)

API REST serverless para gerenciamento de produtos, construída com **Java 21**, **AWS Lambda**, **API Gateway** e **DynamoDB**. Demonstra o padrão de desenvolvimento serverless na AWS com infraestrutura como código via **AWS SAM**.

---

## Arquitetura

```
┌─────────────┐     HTTPS      ┌──────────────────┐     invoke     ┌─────────────────────┐
│             │ ─────────────► │                  │ ─────────────► │                     │
│   Cliente   │                │   API Gateway    │                │   ProductHandler    │
│  (browser,  │ ◄───────────── │   (REST API)     │ ◄───────────── │   (Java Lambda)     │
│   Postman)  │    JSON resp.  │                  │   JSON resp.   │                     │
└─────────────┘                └──────────────────┘                └──────────┬──────────┘
                                                                              │
                                                                              │ SDK v2
                                                                              ▼
                                                                   ┌─────────────────────┐
                                                                   │                     │
                                                                   │     DynamoDB        │
                                                                   │  (Tabela Products)  │
                                                                   │                     │
                                                                   └─────────────────────┘

┌─────────────┐   s3:ObjectCreated   ┌──────────────────────────────────────────────────┐
│             │ ──────────────────►  │                                                  │
│  Amazon S3  │                      │   S3EventHandler (Java Lambda)                   │
│   Bucket    │ ◄────────────────── │   Processa uploads/remoções de objetos            │
│             │                      │                                                  │
└─────────────┘                      └──────────────────────────────────────────────────┘
```

---

## Arquitetura & SOLID

| Princípio | Onde foi aplicado |
|---|---|
| **S** — Single Responsibility | `ProductHandler` faz só roteamento HTTP, `ProductService` só regras de negócio, `DynamoDbService` só acesso ao banco |
| **O** — Open/Closed | Novos handlers podem ser adicionados sem modificar os existentes |
| **L** — Liskov Substitution | `DynamoDbService` usa construtor package-private para facilitar substituição por mock nos testes |
| **I** — Interface Segregation | Cada handler implementa apenas `RequestHandler` com os tipos que precisa |
| **D** — Dependency Inversion | `ProductService` recebe `DynamoDbService` via construtor — fácil de testar e substituir |

### Arquitetura Serverless
```
Internet → API Gateway → AWS Lambda (Java 21)
                              ├── ProductHandler → DynamoDB (CRUD)
                              └── S3EventHandler → processa uploads S3
```

### Design Patterns utilizados
- **Singleton Pattern** — `DynamoDbService.getInstance()` garante uma única conexão por container Lambda
- **Builder Pattern** — `DynamoDbClient.builder()` e `APIGatewayProxyResponseEvent` usam builders
- **Factory Method** — `ResponseBuilder` centraliza criação de respostas HTTP padronizadas

---

## Pré-requisitos

| Ferramenta   | Versão mínima | Link                                                              |
|--------------|---------------|-------------------------------------------------------------------|
| Java (JDK)   | 21            | https://aws.amazon.com/corretto/                                  |
| Maven        | 3.9+          | https://maven.apache.org/download.cgi                             |
| AWS CLI      | 2.x           | https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html |
| AWS SAM CLI  | 1.100+        | https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html |
| Docker       | 24+           | https://www.docker.com/get-started/ (necessário para `sam local`) |

Configure as credenciais AWS antes do deploy:
```bash
aws configure
```

---

## Como buildar

```bash
# Clonar o repositório
git clone https://github.com/AlexGamboaJunior/java-aws-lambda.git
cd java-aws-lambda

# Compilar e gerar o fat JAR (uber jar) para deploy na AWS
mvn package

# O artefato gerado fica em:
# target/lambda-1.0.0-aws.jar
```

---

## Como rodar testes unitários

```bash
# Roda todos os testes (sem necessidade de conexão com AWS)
mvn test

# Com relatório detalhado
mvn test -Dsurefire.failIfNoSpecifiedTests=false
```

Os testes usam **JUnit 5** + **Mockito** e não precisam de infraestrutura AWS — tudo é mockado.

---

## Como testar localmente com SAM

```bash
# Iniciar a API localmente (requer Docker)
sam local start-api

# Testar com os eventos de exemplo:
sam local invoke ProductsFunction --event events/list-products.json
sam local invoke ProductsFunction --event events/create-product.json
sam local invoke ProductsFunction --event events/get-product.json
```

---

## Como fazer deploy na AWS

```bash
# Build + deploy interativo (primeira vez)
mvn package && sam deploy --guided

# Parâmetros sugeridos no wizard:
#   Stack Name:          java-aws-lambda-stack
#   AWS Region:          us-east-1
#   Confirm changes:     y
#   Allow IAM roles:     y
#   Save to samconfig:   y

# Deploys subsequentes (usa samconfig.toml salvo)
mvn package && sam deploy
```

---

## Endpoints da API

| Método   | Path              | Descrição                    | Status de Sucesso |
|----------|-------------------|------------------------------|-------------------|
| `GET`    | `/products`       | Lista todos os produtos      | `200 OK`          |
| `GET`    | `/products/{id}`  | Busca produto por ID         | `200 OK`          |
| `POST`   | `/products`       | Cria um novo produto         | `201 Created`     |
| `DELETE` | `/products/{id}`  | Remove um produto por ID     | `200 OK`          |

### Exemplo de request body (POST /products)

```json
{
  "name": "Notebook Dell",
  "description": "Intel Core i7, 16GB RAM, 512GB SSD",
  "price": 4500.00,
  "stock": 10
}
```

### Exemplo de response (201 Created)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Notebook Dell",
  "description": "Intel Core i7, 16GB RAM, 512GB SSD",
  "price": 4500.00,
  "stock": 10,
  "createdAt": "2024-01-15T14:30:00.123456789Z"
}
```

---

## Estrutura do projeto

```
java-aws-lambda/
├── src/
│   ├── main/java/com/alexgamboa/lambda/
│   │   ├── handler/
│   │   │   ├── ProductHandler.java     ← Lambda CRUD (API Gateway)
│   │   │   └── S3EventHandler.java     ← Lambda de eventos S3
│   │   ├── model/
│   │   │   └── Product.java            ← POJO do domínio
│   │   ├── service/
│   │   │   ├── ProductService.java     ← Lógica de negócio
│   │   │   └── DynamoDbService.java    ← Acesso ao DynamoDB (Singleton)
│   │   └── util/
│   │       └── ResponseBuilder.java    ← Respostas HTTP padronizadas
│   └── test/java/com/alexgamboa/lambda/
│       ├── handler/ProductHandlerTest.java
│       └── service/ProductServiceTest.java
├── events/                             ← Payloads para testes locais (SAM)
│   ├── create-product.json
│   ├── get-product.json
│   └── list-products.json
├── .github/workflows/ci.yml           ← GitHub Actions CI
├── template.yaml                       ← AWS SAM IaC
└── pom.xml
```

---

## Stack de tecnologias

- **Runtime:** Java 21 (Amazon Corretto)
- **Framework serverless:** AWS SAM (Serverless Application Model)
- **Compute:** AWS Lambda
- **API:** Amazon API Gateway (REST)
- **Banco de dados:** Amazon DynamoDB (NoSQL, pay-per-request)
- **Storage:** Amazon S3
- **SDK:** AWS SDK for Java v2
- **Serialização:** Jackson Databind 2.16
- **Testes:** JUnit 5 + Mockito
- **Build:** Apache Maven + maven-shade-plugin (fat JAR)
- **CI/CD:** GitHub Actions

---

## Autor

**Alex Gamboa Junior**

- GitHub: [@AlexGamboaJunior](https://github.com/AlexGamboaJunior)
- Email: alexsandrojgj83@gmail.com
