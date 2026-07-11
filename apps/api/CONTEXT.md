# Event Manager API

Contexto responsável pelas identidades, eventos e inscrições do Event Manager.

## Language

**Participante**:
Usuário criado pelo cadastro público, apto a consultar eventos e administrar as próprias inscrições.
É representado por `PARTICIPANTE` nos contratos e dados persistidos.
_Avoid_: Participant, user, usuário comum

**Organizador**:
Usuário provisionado manualmente pelo responsável do sistema, apto a criar e administrar os próprios eventos.
É representado por `ORGANIZADOR` nos contratos e dados persistidos.
_Avoid_: Organizer, administrador, admin

**Usuário**:
Identidade autenticável do sistema, associada a exatamente um perfil: Organizador ou Participante.
_Avoid_: User, conta

**Perfil**:
Classificação imutável que determina se um Usuário é Organizador ou Participante.
_Avoid_: Role, papel, tipo de usuário

**Evento**:
Encontro presencial ou online criado e administrado por um Organizador.
_Avoid_: Event, atividade

**Cancelamento de Evento**:
Encerramento lógico de um Evento sem remover seu documento nem suas Inscrições.
_Avoid_: Exclusão, remoção

**Inscrição**:
Vínculo entre um Participante e um Evento, com estado próprio de ativação ou cancelamento.
_Avoid_: Registration, matrícula, reserva

**Catálogo**:
Conjunto de todos os Eventos consultáveis pelo Participante, independentemente de ainda aceitarem inscrição.
_Avoid_: Eventos disponíveis
