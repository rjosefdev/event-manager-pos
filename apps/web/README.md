# Event Manager Web

Front end em Next.js para o Event Manager. A aplicação permite cadastro/login, restaura sessão com o token salvo no navegador e apresenta áreas diferentes para participantes e organizadores.

## Pré-requisitos

- Node.js compatível com Next.js 16
- pnpm 11.x
- API do Event Manager disponível localmente ou em uma URL acessível pelo navegador

## Configuração

A UI usa a variável pública `NEXT_PUBLIC_API_URL` para montar as chamadas HTTP. Quando ela não é definida, o valor padrão é:

```bash
http://localhost:8080
```

Exemplo de arquivo `.env.local`:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## Comandos

A partir da raiz do monorepo:

```bash
pnpm --filter web dev
pnpm --filter web build
pnpm --filter web lint
```

Ou, para subir web e API juntos quando as dependências do monorepo estiverem instaladas:

```bash
pnpm dev
```

O servidor web roda em `http://localhost:3000`.

## Fluxos implementados

- Cadastro de participante em `/autenticacao/cadastro`.
- Login em `/autenticacao/login`.
- Restauração de sessão consultando `/usuarios/atual`.
- Área do participante com catálogo, detalhes de eventos, criação, cancelamento e reativação de inscrições.
- Área do organizador com criação, edição, cancelamento e listagem de inscritos dos próprios eventos.

## Endpoints consumidos

- `POST /autenticacao/cadastro`
- `POST /autenticacao/login`
- `GET /usuarios/atual`
- `GET /catalogo/eventos`
- `GET /catalogo/eventos/{eventoId}`
- `GET /inscricoes`
- `POST /inscricoes`
- `DELETE /inscricoes/{inscricaoId}`
- `PATCH /inscricoes/{inscricaoId}/reativar`
- `GET /eventos`
- `POST /eventos`
- `PUT /eventos/{eventoId}`
- `DELETE /eventos/{eventoId}`
- `GET /eventos/{eventoId}/inscricoes`

## Observações de arquitetura

Hoje a maior parte do front end está concentrada em `src/app/page.tsx`: tipos da API, helpers, chamadas HTTP, estado de sessão e componentes das áreas autenticadas. Consulte `FRONTEND_REVIEW.md` para as tarefas sugeridas de desacoplamento, roteamento protegido e correção de documentação.

A próxima evolução de roteamento deve usar `src/proxy.ts` para bloqueio inicial por cookie de `/participante` e `/organizador`, layouts em `src/app/(protected)` para validar sessão real e layouts específicos por role. Esse fluxo melhora a experiência e evita flash de conteúdo protegido, mas a autorização definitiva das ações continua no backend.
