# Serviços Mobile em Cloud AWS

### Aluna: Jasmini Rebecca Gomes dos Santos

## Tema 2: Avatares Avançados

Nenhum usuário merece ficar sem avatar. O servidor tenta definir um automaticamente ao criar o usuário e também permite refazer esse processo manualmente.

### O que foi implementado

#### 1. Busca no Gravatar

Ao criar um usuário, o `GravatarService` calcula o hash MD5 do e-mail e consulta o [Gravatar](https://docs.gravatar.com/general/images/). A requisição usa `d=404` para distinguir quem tem avatar cadastrado de quem não tem. Se a resposta for HTTP 404, o fluxo segue para o fallback.

#### 2. Fallback com ui-avatars.com

Se o usuário não tiver Gravatar, o serviço chama a [API ui-avatars.com](https://ui-avatars.com/) com o nome do usuário, gerando um avatar em **PNG** (`format=png`, tamanho 200px).

#### 3. Download e upload para o S3

O avatar encontrado é **baixado** (bytes em memória) e enviado ao armazenamento configurado — **não** basta gravar o link externo no campo `avatar` do usuário.

* `DownloadedMultipartFile` encapsula o arquivo baixado como `MultipartFile`, reutilizando o mesmo fluxo de upload manual
* `AvatarService` valida o tipo (`image/jpeg` ou `image/png`) e grava em `avatars/{userId}/a_{userId}.{ext}`
* Em produção (`profile` padrão), o `S3Storage` envia o arquivo ao bucket S3 e expõe a URL via CloudFront
* Com o profile `fs`, o `FileSystemStorage` grava localmente em `./fs` (útil para desenvolvimento)

A atribuição do avatar acontece em `UserService.insert`, logo após a criação do usuário. Falhas na busca do avatar são logadas, mas não impedem o cadastro.


#### 4. Endpoint DELETE `/users/{id}/avatar` (opcional)

Implementado em `UserController.resetAvatar`. Refaz todo o processo (Gravatar → ui-avatars → S3) e atualiza o campo `avatar` do usuário.

* Requer autenticação JWT
* O próprio usuário ou um admin pode chamar o endpoint
* Retorna o `UserResponse` atualizado com a nova URL do avatar

### Arquivos principais

| Arquivo | Responsabilidade |
|---|---|
| `users/GravatarService.kt` | Busca no Gravatar, fallback ui-avatars, download da imagem |
| `users/DownloadedMultipartFile.kt` | Adapta bytes baixados para `MultipartFile` |
| `users/AvatarService.kt` | Valida tipo, define path e delega ao storage |
| `users/UserService.kt` | Avatar automático no `insert`; `resetAvatar` no DELETE |
| `users/UserController.kt` | `PUT /users/{id}/avatar` (upload manual) e `DELETE /users/{id}/avatar` |
| `files/S3Storage.kt` | Upload e leitura no S3 |
| `files/FilesConfig.kt` | Escolhe S3 (padrão) ou filesystem (`fs`) |

### Endpoints de avatar

| Método | Rota | Descrição |
|---|---|---|
| `PUT` | `/users/{id}/avatar` | Upload manual de avatar (multipart) |
| `DELETE` | `/users/{id}/avatar` | Refaz busca Gravatar/ui-avatars e reenvia ao S3 |

### Fluxo

```text
Criar usuário
│
└── Buscar avatar no Gravatar (MD5 do e-mail, d=404)
    ├── HTTP 200
    │   └── Download da imagem
    └── HTTP 404
        └── Gerar avatar em ui-avatars.com (PNG)
            │
            ▼
DownloadedMultipartFile
(bytes → MultipartFile)
            │
            ▼
AvatarService
            │
            ▼
S3Storage
            │
            ▼
Bucket S3
            │
            ▼
Banco salva apenas o path
(ex.: 2/a_2.png)
            │
            ▼
API retorna a URL do arquivo armazenado no S3
(nunca a URL do Gravatar ou ui-avatars)
```

### Configuração na AWS

| Item | Detalhe |
|---|---|
| S3 Bucket | `jasmini-authserver-avatars` |
| Região | `us-east-2` (Ohio) |
| IAM User | usuário dedicado com access key |
| Policy IAM | `PutObject`, `GetObject`, `DeleteObject`, `ListBucket` no bucket |
| Credenciais | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` via `.env` |
| Path no S3 | `avatars/{userId}/a_{userId}.png` |

### Vídeo

https://github.com/user-attachments/assets/50862393-9d5b-4013-8044-770e30920650
