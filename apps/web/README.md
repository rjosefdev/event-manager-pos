# Web - Event Manager POS

Front end Next.js do Event Manager POS, usado por participantes e organizadores
para acessar os fluxos implementados na API Spring Boot do monorepo.

## Stack

- Next.js 16 com App Router
- React 19
- TypeScript
- Tailwind CSS 4
- pnpm workspaces

## Pre-requisitos

- Node.js compatível com Next.js 16
- pnpm 11
- API local em execução em `http://localhost:8080`
- MongoDB e variáveis da API configuradas em `apps/api/.env`

## Configuração da API

O front end usa a variável pública `NEXT_PUBLIC_API_URL` para montar as URLs da
API. Se ela não for definida, o valor padrão do código é:

```bash
http://localhost:8080
```

Para sobrescrever localmente, crie `apps/web/.env.local`:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

Use outro host apenas quando a API estiver rodando fora da porta padrão do
Spring Boot.

## Comandos

Instale as dependências a partir da raiz do monorepo:

```bash
pnpm install
```

Suba web e API juntos:

```bash
pnpm dev
```

Suba apenas o web app:

```bash
pnpm dev:web
```

Comandos equivalentes dentro deste pacote:

```bash
pnpm --dir apps/web dev
pnpm --dir apps/web build
pnpm --dir apps/web lint
```

URLs locais:

- Web: `http://localhost:3000`
- API esperada pelo web: `http://localhost:8080`

## Fluxos disponíveis

### Cadastro e login

- Cadastro público de participante.
- Login com e-mail e senha.
- Restauração de sessão usando token Bearer persistido em cookie/local storage.
- Redirecionamento por perfil após autenticação.

### Participante

- Catálogo de eventos.
- Busca por texto, filtro por categoria e ordenação por data.
- Detalhe de evento.
- Criação de inscrição.
- Cancelamento e reativação de inscrição.
- Histórico em "Minhas inscrições".

### Organizador

- Listagem dos próprios eventos.
- Criação e edição de evento.
- Cancelamento lógico de evento.
- Busca, filtro por categoria e ordenação por data.
- Listagem de inscritos por evento.

## Perfis suportados

- `PARTICIPANTE`: acessa `/participante`, catálogo e inscrições.
- `ORGANIZADOR`: acessa `/organizador`, eventos próprios e inscritos.

As rotas protegidas `/participante/*` e `/organizador/*` passam pelo proxy do
Next.js e exigem cookie de sessão. A API continua sendo a fonte de autorização
por token e perfil.

## Endpoints consumidos pela UI

Autenticação e sessão:

- `POST /autenticacao/cadastro`
- `POST /autenticacao/login`
- `GET /usuarios/atual`

Organizador:

- `GET /eventos`
- `POST /eventos`
- `PUT /eventos/{id}`
- `DELETE /eventos/{id}`
- `GET /eventos/{eventoId}/inscricoes`

Participante:

- `GET /catalogo/eventos`
- `GET /catalogo/eventos/{id}`
- `GET /inscricoes`
- `POST /inscricoes`
- `DELETE /inscricoes/{id}`
- `PATCH /inscricoes/{id}/reativar`

Imagens de catálogo podem apontar para URLs externas ou para endpoints da API,
dependendo do payload retornado por eventos.

## Limitações conhecidas

- O cadastro público cria apenas participantes. Organizadores dependem do fluxo
  administrativo da API ou de dados previamente existentes.
- O web app ainda não possui testes automatizados específicos para os fluxos de
  UI.
- As páginas do App Router são finas, mas parte da orquestração de tela ainda
  está concentrada em componentes como `AreaParticipante` e `AreaOrganizador`;
  ainda não há subrotas internas para cada etapa dos fluxos.
- A rota inicial não usa mais o antigo `src/app/page.tsx` do scaffold. A home
  atual está em `src/app/(public)/page.tsx`.

## Manutenção de contratos

Quando endpoints, payloads ou regras de autenticação mudarem, atualize na mesma
tarefa:

- este README;
- `docs/insomnia/README.md`;
- `docs/insomnia/insomnia-collection.json`.

Isso evita divergência entre a documentação do web app e o contrato executável
usado para testar a API.
