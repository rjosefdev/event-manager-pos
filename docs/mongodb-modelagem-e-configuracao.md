# MongoDB — Modelagem e configuração inicial

Documentação de apoio para configurar o banco de dados MongoDB do projeto **Event Manager - Pós**.

O projeto usa **Spring Boot + Java 17** no back-end e **MongoDB** como banco de dados. A aplicação possui dois perfis principais: **organizador** e **participante**. O organizador cria e gerencia eventos; o participante consulta eventos e gerencia suas inscrições.

## 1. Visão geral da modelagem

A modelagem inicial usa uma database com três collections principais:

```txt
event_manager_pos
├── usuarios
├── eventos
└── inscricoes
```

A separação segue os agregados principais do domínio:

| Collection | Responsabilidade |
|---|---|
| `usuarios` | Guarda dados de autenticação, identificação e perfil do usuário. |
| `eventos` | Guarda os eventos criados por organizadores. |
| `inscricoes` | Guarda as inscrições dos participantes nos eventos. |

Essa estrutura evita colocar tudo em uma única collection e também evita arrays grandes dentro de `eventos` ou `usuarios`. As inscrições têm regras próprias, como situacao, data de inscrição, cancelamento e bloqueio contra duplicidade, por isso foram modeladas como uma collection separada.

## 2. Convenções adotadas

### 2.1. Nome da database

```txt
event_manager_pos
```

### 2.2. Nome das collections

```txt
usuarios
eventos
inscricoes
```

### 2.3. Padrão de campos

- Usar `camelCase` nos nomes dos campos.
- Usar `_id` padrão do MongoDB como identificador principal.
- Usar referências por `ObjectId` entre collections.
- Usar datas em UTC no banco.
- Não salvar senha pura; salvar apenas `senhaHash`.
- Calcular situacao temporal do evento na API, quando possível, usando `iniciaEm` e `terminaEm`.

### 2.4. Status de evento

Os requisitos pedem listagem de eventos com situacao:

```txt
FUTURO / EM_ANDAMENTO / FINALIZADO
```

Recomendação: não salvar esse situacao diretamente no documento, porque ele muda com o tempo. A API pode calcular dinamicamente:

```txt
now < iniciaEm                -> FUTURO
iniciaEm <= now <= terminaEm     -> EM_ANDAMENTO
now > terminaEm                  -> FINALIZADO
```

Manter no documento apenas flags estáveis, como:

```json
{
  "cancelado": false
}
```

## 3. Collection `usuarios`

### 3.1. Responsabilidade

Representa os usuários cadastrados no sistema.

Atende aos fluxos de:

- cadastro;
- login;
- autenticação;
- autorização por perfil;
- diferenciação entre organizador e participante.

### 3.2. Documento exemplo

```json
{
  "_id": { "$oid": "66a000000000000000000001" },
  "nome": "Organizador Demo",
  "email": "organizador.demo@email.com",
  "senhaHash": "$2a$10$hash_bcrypt_apenas_exemplo",
  "perfil": "ORGANIZADOR",
  "criadoEm": { "$date": "2026-07-08T00:00:00.000Z" },
  "atualizadoEm": { "$date": "2026-07-08T00:00:00.000Z" },
  "ativo": true
}
```

### 3.3. Campos

| Campo | Tipo | Obrigatório | Descrição |
|---|---:|:---:|---|
| `_id` | `ObjectId` | Sim | Identificador gerado pelo MongoDB. |
| `nome` | `string` | Sim | Nome do usuário. |
| `email` | `string` | Sim | E-mail usado para login. Deve ser único. |
| `senhaHash` | `string` | Sim | Hash da senha. Nunca salvar senha pura. |
| `perfil` | `string` | Sim | Perfil do usuário: `ORGANIZADOR` ou `PARTICIPANTE`. |
| `criadoEm` | `date` | Sim | Data de criação do usuário. |
| `atualizadoEm` | `date` | Sim | Data da última atualização. |
| `ativo` | `boolean` | Sim | Indica se o usuário está ativo. |

### 3.4. Valores permitidos para `perfil`

```txt
ORGANIZADOR
PARTICIPANTE
```

### 3.5. Índices recomendados

```js
db.usuarios.createIndex(
  { email: 1 },
  { unique: true, nome: "uk_usuarios_email" }
);
```

Esse índice impede cadastro duplicado com o mesmo e-mail.

## 4. Collection `eventos`

### 4.1. Responsabilidade

Representa os eventos criados por organizadores.

Atende aos fluxos de:

- criação de evento;
- edição de evento;
- cancelamento lógico de evento;
- listagem dos próprios eventos do organizador;
- catálogo público para participantes;
- busca por texto;
- filtro por categoria;
- ordenação por data;
- visualização de detalhes do evento.

### 4.2. Documento exemplo

```json
{
  "_id": { "$oid": "66a000000000000000000101" },
  "titulo": "Workshop de Spring Boot",
  "descricao": "Evento introdutório sobre APIs REST com Spring Boot.",
  "iniciaEm": { "$date": "2026-08-10T22:00:00.000Z" },
  "terminaEm": { "$date": "2026-08-11T00:00:00.000Z" },
  "local": {
    "type": "ONLINE",
    "name": "Google Meet",
    "endereco": null,
    "url": "https://meet.google.com/exemplo"
  },
  "categoria": "TECNOLOGIA",
  "capacidade": 50,
  "urlImagem": null,
  "organizadorId": { "$oid": "66a000000000000000000001" },
  "cancelado": false,
  "criadoEm": { "$date": "2026-07-08T00:00:00.000Z" },
  "atualizadoEm": { "$date": "2026-07-08T00:00:00.000Z" }
}
```

### 4.3. Campos

| Campo | Tipo | Obrigatório | Descrição |
|---|---:|:---:|---|
| `_id` | `ObjectId` | Sim | Identificador gerado pelo MongoDB. |
| `titulo` | `string` | Sim | Título do evento. |
| `descricao` | `string` | Sim | Descrição do evento. |
| `iniciaEm` | `date` | Sim | Data e hora de início. |
| `terminaEm` | `date` | Sim | Data e hora de término. |
| `local` | `object` | Sim | Dados do local físico ou online. |
| `local.type` | `string` | Sim | `ONLINE` ou `PRESENCIAL`. |
| `local.name` | `string` | Não | Nome do local ou plataforma. |
| `local.endereco` | `string/null` | Não | Endereço físico, quando aplicável. |
| `local.url` | `string/null` | Não | Link online, quando aplicável. |
| `categoria` | `string` | Sim | Categoria usada para filtro. |
| `capacidade` | `number` | Sim | Número máximo de vagas. |
| `urlImagem` | `string/null` | Não | URL da imagem do evento. |
| `organizadorId` | `ObjectId` | Sim | Referência para `usuarios._id` do organizador. |
| `cancelado` | `boolean` | Sim | Indica se o evento foi cancelado. |
| `criadoEm` | `date` | Sim | Data de criação. |
| `atualizadoEm` | `date` | Sim | Data da última atualização. |

### 4.4. Valores permitidos para `local.type`

```txt
ONLINE
PRESENCIAL
```

### 4.5. Categorias iniciais sugeridas

As categorias podem ser evoluídas conforme a UI. Para o MVP, uma lista simples é suficiente:

```txt
TECNOLOGIA
EDUCACAO
NEGOCIOS
SAUDE
CULTURA
OUTROS
```

### 4.6. Índices recomendados

```js
db.eventos.createIndex(
  { organizadorId: 1, iniciaEm: 1 },
  { nome: "idx_eventos_organizer_starts_at" }
);

// Catálogo com filtro por categoria e ordenação por data.
db.eventos.createIndex(
  { categoria: 1, iniciaEm: 1 },
  { nome: "idx_eventos_category_starts_at" }
);

// Listagem geral ordenada por data.
db.eventos.createIndex(
  { iniciaEm: 1 },
  { nome: "idx_eventos_starts_at" }
);

// Busca textual por título e descrição.
db.eventos.createIndex(
  { titulo: "text", descricao: "text" },
  { nome: "txt_eventos_title_description" }
);
```

## 5. Collection `inscricoes`

### 5.1. Responsabilidade

Representa a inscrição de um participante em um evento.

Atende aos fluxos de:

- inscrição em evento;
- bloqueio contra inscrição duplicada;
- cancelamento de inscrição;
- listagem de “Minhas Inscrições”;
- listagem de inscritos por evento para o organizador.

### 5.2. Documento exemplo

```json
{
  "_id": { "$oid": "66a000000000000000000201" },
  "eventoId": { "$oid": "66a000000000000000000101" },
  "participanteId": { "$oid": "66a000000000000000000002" },
  "situacao": "ATIVA",
  "inscritoEm": { "$date": "2026-07-08T00:00:00.000Z" },
  "canceladoEm": null,
  "criadoEm": { "$date": "2026-07-08T00:00:00.000Z" },
  "atualizadoEm": { "$date": "2026-07-08T00:00:00.000Z" }
}
```

### 5.3. Campos

| Campo | Tipo | Obrigatório | Descrição |
|---|---:|:---:|---|
| `_id` | `ObjectId` | Sim | Identificador gerado pelo MongoDB. |
| `eventoId` | `ObjectId` | Sim | Referência para `eventos._id`. |
| `participanteId` | `ObjectId` | Sim | Referência para `usuarios._id` do participante. |
| `situacao` | `string` | Sim | `ATIVA` ou `CANCELADA`. |
| `inscritoEm` | `date` | Sim | Data da inscrição. |
| `canceladoEm` | `date/null` | Não | Data do cancelamento. |
| `criadoEm` | `date` | Sim | Data de criação do registro. |
| `atualizadoEm` | `date` | Sim | Data da última atualização. |

### 5.4. Valores permitidos para `situacao`

```txt
ATIVA
CANCELADA
```

### 5.5. Índices recomendados

```js
// Impede o mesmo participante de ter mais de uma inscrição para o mesmo evento.
db.inscricoes.createIndex(
  { eventoId: 1, participanteId: 1 },
  { unique: true, nome: "uk_inscricoes_event_participant" }
);

// Consulta das inscrições do participante.
db.inscricoes.createIndex(
  { participanteId: 1, situacao: 1 },
  { nome: "idx_inscricoes_participant_status" }
);

// Consulta da lista de inscritos por evento.
db.inscricoes.createIndex(
  { eventoId: 1, situacao: 1 },
  { nome: "idx_inscricoes_event_status" }
);
```

## 6. Relacionamentos entre collections

```txt
usuarios
  ├── ORGANIZADOR cria eventos
  └── PARTICIPANTE cria inscricoes

 eventos
  └── organizadorId -> usuarios._id

inscricoes
  ├── eventoId -> eventos._id
  └── participanteId -> usuarios._id
```

Cardinalidade:

| Relação | Cardinalidade |
|---|---:|
| Um organizador possui muitos eventos | `usuarios 1:N eventos` |
| Um participante possui muitas inscrições | `usuarios 1:N inscricoes` |
| Um evento possui muitas inscrições | `eventos 1:N inscricoes` |
| Um participante só pode ter uma inscrição por evento | `unique(eventoId, participanteId)` |

## 7. Regras de negócio impactadas pela modelagem

### 7.1. Participante não pode editar evento

A API deve verificar o `perfil` do usuário autenticado.

```txt
perfil = ORGANIZADOR -> pode criar evento
perfil = PARTICIPANTE -> não pode criar/editar/excluir evento
```

### 7.2. Organizador só pode editar os próprios eventos

A API deve comparar:

```txt
event.organizadorId == authenticatedUsuario.id
```

### 7.3. Participante não pode se inscrever duas vezes no mesmo evento

Garantido pelo índice único:

```js
{ eventoId: 1, participanteId: 1 }
```

### 7.4. Cancelamento antes do término do evento

Antes de cancelar uma inscrição, a API deve buscar o evento e validar:

```txt
now < evento.terminaEm
```

Depois disso, atualizar a inscrição:

```json
{
  "situacao": "CANCELADA",
  "canceladoEm": "data_atual"
}
```

### 7.5. Limite de vagas

Para o MVP, a API pode contar inscrições ativas antes de criar uma nova inscrição:

```js
db.inscricoes.countDocuments({
  eventoId: ObjectId("ID_DO_EVENTO"),
  situacao: "ATIVA"
});
```

Depois compara com:

```txt
event.capacidade
```

Atenção: em cenário real com alta concorrência, apenas contar e depois inserir pode permitir corrida entre requisições simultâneas. Para evoluir esse ponto, existem duas opções:

1. usar transação no MongoDB; ou
2. manter um contador `ativoInscricaoCount` em `eventos` e atualizar com condição atômica.

Para o projeto acadêmico, o fluxo com contagem + índice único é suficiente para começar.

## 8. How-to: configurar no MongoDB Atlas

### 8.1. Criar a database e a primeira collection

No MongoDB Atlas:

1. Acesse o projeto no Atlas.
2. Abra o `Cluster0`.
3. Clique em **Browse Collections** ou **Data Explorer**.
4. Clique em **Create Database**.
5. Preencha:

```txt
Database Name: event_manager_pos
Collection Name: usuarios
```

6. Confirme em **Create**.

### 8.2. Criar as collections restantes

Dentro da database `event_manager_pos`, crie:

```txt
eventos
inscricoes
```

Ao final, a estrutura esperada será:

```txt
Cluster0
├── admin
├── aula
├── event_manager_pos
│   ├── usuarios
│   ├── eventos
│   └── inscricoes
├── local
└── sample_mflix
```

## 9. How-to: criar índices pelo Atlas

No fluxo definitivo da aplicacao, os indices obrigatorios sao garantidos programaticamente na inicializacao com `MongoTemplate`. As instrucoes do Atlas abaixo permanecem uteis para inspecao e operacao manual, mas nao sao a fonte de verdade da aplicacao.

Para cada collection:

1. Abra a collection no Atlas.
2. Acesse a aba **Indexes**.
3. Clique em **Create Index**.
4. Informe os campos conforme os exemplos abaixo.

### 9.1. `usuarios`

```json
{
  "email": 1
}
```

Opções:

```json
{
  "unique": true,
  "name": "uk_usuarios_email"
}
```

### 9.2. `eventos`

Criar os seguintes índices:

```json
{ "organizadorId": 1, "iniciaEm": 1 }
```

```json
{ "categoria": 1, "iniciaEm": 1 }
```

```json
{ "iniciaEm": 1 }
```

```json
{ "titulo": "text", "descricao": "text" }
```

### 9.3. `inscricoes`

```json
{ "eventoId": 1, "participanteId": 1 }
```

Opções:

```json
{
  "unique": true,
  "name": "uk_inscricoes_event_participant"
}
```

Também criar:

```json
{ "participanteId": 1, "situacao": 1 }
```

```json
{ "eventoId": 1, "situacao": 1 }
```

## 10. How-to: criar índices via MongoDB Shell

Se estiver usando `mongosh`, execute:

```js
use event_manager_pos;

// usuarios
db.usuarios.createIndex(
  { email: 1 },
  { unique: true, nome: "uk_usuarios_email" }
);

// eventos
db.eventos.createIndex(
  { organizadorId: 1, iniciaEm: 1 },
  { nome: "idx_eventos_organizer_starts_at" }
);

db.eventos.createIndex(
  { categoria: 1, iniciaEm: 1 },
  { nome: "idx_eventos_category_starts_at" }
);

db.eventos.createIndex(
  { iniciaEm: 1 },
  { nome: "idx_eventos_starts_at" }
);

db.eventos.createIndex(
  { titulo: "text", descricao: "text" },
  { nome: "txt_eventos_title_description" }
);

// inscricoes
db.inscricoes.createIndex(
  { eventoId: 1, participanteId: 1 },
  { unique: true, nome: "uk_inscricoes_event_participant" }
);

db.inscricoes.createIndex(
  { participanteId: 1, situacao: 1 },
  { nome: "idx_inscricoes_participant_status" }
);

db.inscricoes.createIndex(
  { eventoId: 1, situacao: 1 },
  { nome: "idx_inscricoes_event_status" }
);
```

## 11. How-to: configurar a API Spring Boot

### 11.1. Dependência Maven

No `apps/api/pom.xml`, confirme se existe a dependência do MongoDB:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 11.2. Variável de ambiente

Não coloque usuário e senha do MongoDB direto no Git.

Use variável de ambiente:

```txt
MONGODB_URI=mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/event_manager_pos?retryWrites=true&w=majority&appName=Cluster0
```

### 11.3. `application.properties`

Arquivo:

```txt
apps/api/src/main/resources/application.properties
```

Conteúdo sugerido:

```properties
spring.application.name=eventos-api
spring.mongodb.uri=${MONGODB_URI}
```

### 11.4. Rodando no Linux/WSL

```bash
export MONGODB_URI="mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/event_manager_pos?retryWrites=true&w=majority&appName=Cluster0"
pnpm dev:api
```

### 11.5. Rodando no Windows PowerShell

```powershell
$env:MONGODB_URI="mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/event_manager_pos?retryWrites=true&w=majority&appName=Cluster0"
pnpm dev:api
```

## 12. How-to: classes de domínio no Spring Data MongoDB

### 12.1. `Usuario`

```java
@Document(collection = "usuarios")
public class Usuario {
    @Id
    private String id;

    private String nome;
    private String email;
    private String senhaHash;
    private Perfil perfil;
    private Instant criadoEm;
    private Instant atualizadoEm;
    private Boolean ativo;
}
```

### 12.2. `Perfil`

```java
public enum Perfil {
    ORGANIZADOR,
    PARTICIPANTE
}
```

### 12.3. `Evento`

```java
@Document(collection = "eventos")
public class Evento {
    @Id
    private String id;

    private String titulo;
    private String descricao;
    private Instant iniciaEm;
    private Instant terminaEm;
    private LocalEvento local;
    private String categoria;
    private Integer capacidade;
    private String urlImagem;
    private String organizadorId;
    private Boolean cancelado;
    private Instant criadoEm;
    private Instant atualizadoEm;
}
```

### 12.4. `LocalEvento`

```java
public class LocalEvento {
    private TipoLocal tipo;
    private String nome;
    private String endereco;
    private String url;
}
```

### 12.5. `TipoLocal`

```java
public enum TipoLocal {
    ONLINE,
    PRESENCIAL
}
```

### 12.6. `Inscricao`

```java
@Document(collection = "inscricoes")
public class Inscricao {
    @Id
    private String id;

    private String eventoId;
    private String participanteId;
    private SituacaoInscricao situacao;
    private Instant inscritoEm;
    private Instant canceladoEm;
    private Instant criadoEm;
    private Instant atualizadoEm;
}
```

### 12.7. `SituacaoInscricao`

```java
public enum SituacaoInscricao {
    ATIVA,
    CANCELADA
}
```

## 13. How-to: dados de teste iniciais

Execute no `mongosh` dentro da database `event_manager_pos`.

### 13.1. Criar usuários

```js
const organizadorId = new ObjectId();
const participanteId = new ObjectId();

db.usuarios.insertMany([
  {
    _id: organizadorId,
    nome: "Organizador Demo",
    email: "organizador.demo@email.com",
    senhaHash: "$2a$10$hash_bcrypt_apenas_exemplo",
    perfil: "ORGANIZADOR",
    criadoEm: new Date(),
    atualizadoEm: new Date(),
    ativo: true
  },
  {
    _id: participanteId,
    nome: "Participante Demo",
    email: "participante.demo@email.com",
    senhaHash: "$2a$10$hash_bcrypt_apenas_exemplo",
    perfil: "PARTICIPANTE",
    criadoEm: new Date(),
    atualizadoEm: new Date(),
    ativo: true
  }
]);
```

### 13.2. Criar evento

```js
const eventoId = new ObjectId();

db.eventos.insertOne({
  _id: eventoId,
  titulo: "Workshop de Spring Boot",
  descricao: "Evento introdutório sobre APIs REST com Spring Boot.",
  iniciaEm: new Date("2026-08-10T22:00:00.000Z"),
  terminaEm: new Date("2026-08-11T00:00:00.000Z"),
  local: {
    type: "ONLINE",
    nome: "Google Meet",
    endereco: null,
    url: "https://meet.google.com/exemplo"
  },
  categoria: "TECNOLOGIA",
  capacidade: 50,
  urlImagem: null,
  organizadorId: organizadorId,
  cancelado: false,
  criadoEm: new Date(),
  atualizadoEm: new Date()
});
```

### 13.3. Criar inscrição

```js
db.inscricoes.insertOne({
  eventoId: eventoId,
  participanteId: participanteId,
  situacao: "ATIVA",
  inscritoEm: new Date(),
  canceladoEm: null,
  criadoEm: new Date(),
  atualizadoEm: new Date()
});
```

## 14. Consultas úteis para validar o banco

### 14.1. Buscar usuário por e-mail

```js
db.usuarios.findOne({ email: "organizador.demo@email.com" });
```

### 14.2. Listar eventos de um organizador

```js
db.eventos.find({ organizadorId: organizadorId }).sort({ iniciaEm: 1 });
```

### 14.3. Buscar eventos por texto

```js
db.eventos.find({
  $text: { $search: "Spring Boot" }
});
```

### 14.4. Filtrar eventos por categoria

```js
db.eventos.find({
  categoria: "TECNOLOGIA",
  cancelado: false
}).sort({ iniciaEm: 1 });
```

### 14.5. Listar inscrições de um participante

```js
db.inscricoes.find({
  participanteId: participanteId,
  situacao: "ATIVA"
});
```

### 14.6. Listar inscritos de um evento

```js
db.inscricoes.find({
  eventoId: eventoId,
  situacao: "ATIVA"
});
```

## 15. Próximos passos no código

Após configurar o MongoDB, implementar na API:

```txt
src/main/java/com/rjosefdev/eventos_api
├── usuarios
│   ├── Usuario.java
│   ├── UsuarioRepository.java
│   ├── UsuarioService.java
│   └── Perfil.java
├── eventos
│   ├── Evento.java
│   ├── EventoController.java
│   ├── EventoService.java
│   ├── EventoRepository.java
│   └── dto
├── inscricoes
│   ├── Inscricao.java
│   ├── InscricaoController.java
│   ├── InscricaoService.java
│   ├── InscricaoRepository.java
│   └── dto
```

Repositórios esperados:

```java
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
}
```

```java
public interface EventoRepository extends MongoRepository<Evento, String> {
    List<Evento> findByOrganizadorIdOrderByIniciaEmAsc(String organizadorId);
    List<Evento> findByCategoriaAndCanceladoFalseOrderByIniciaEmAsc(String categoria);
}
```

```java
public interface InscricaoRepository extends MongoRepository<Inscricao, String> {
    boolean existsByEventoIdAndParticipanteId(String eventoId, String participanteId);
    long countByEventoIdAndSituacao(String eventoId, SituacaoInscricao situacao);
    List<Inscricao> findByParticipanteIdAndSituacao(String participanteId, SituacaoInscricao situacao);
    List<Inscricao> findByEventoIdAndSituacao(String eventoId, SituacaoInscricao situacao);
}
```

## 16. Resumo da decisão

A modelagem inicial usa três collections porque:

1. `usuarios` tem responsabilidade de identidade, autenticação e perfil;
2. `eventos` tem responsabilidade de catálogo e gestão de eventos;
3. `inscricoes` representa uma relação com estado próprio entre participante e evento.

Essa estrutura atende bem ao MVP, facilita a implementação dos módulos da API e mantém o banco organizado sem tentar forçar tudo dentro de um único documento.
