export type ErrosCampos = Partial<Record<string, string>>;
export type ModoAutenticacao = "cadastro" | "login";
export type OrdenacaoEventos = "data-asc" | "data-desc";
export type AbaParticipante = "catalogo" | "inscricoes";

export type FiltrosEventos = {
  busca: string;
  categoria: string;
  ordenacao: OrdenacaoEventos;
};

export type ProblemaApi = {
  detail?: string;
  erros?: ErrosCampos;
};

export type UsuarioSessao = {
  id: string;
  nome: string;
  email: string;
  perfil: "ORGANIZADOR" | "PARTICIPANTE";
};

export type Sessao = {
  tokenAcesso: string;
  tipoToken: "Bearer";
  expiraEm: string;
  usuario: UsuarioSessao;
};

export type SituacaoTemporal = "FUTURO" | "EM_ANDAMENTO" | "FINALIZADO";

export type Evento = {
  id: string;
  organizadorId: string;
  titulo: string;
  descricao: string;
  iniciaEm: string;
  terminaEm: string;
  local: string;
  online: boolean;
  categoria: string;
  vagas: number;
  imagemUrl?: string | null;
  cancelado: boolean;
  situacaoTemporal: SituacaoTemporal;
};

export type EventoCatalogo = Omit<Evento, "organizadorId"> & {
  vagasDisponiveis: number;
  inscricaoPermitida: boolean;
};

export type SituacaoInscricao = "ATIVA" | "CANCELADA";

export type Inscricao = {
  id: string;
  eventoId: string;
  participanteId: string;
  situacao: SituacaoInscricao;
  inscritoEm: string;
  canceladoEm?: string | null;
  evento: EventoCatalogo;
};

export type ParticipanteInscrito = {
  id: string;
  nome: string;
  email: string;
};

export type InscritoEvento = {
  id: string;
  eventoId: string;
  participanteId: string;
  situacao: SituacaoInscricao;
  inscritoEm: string;
  canceladoEm?: string | null;
  participante: ParticipanteInscrito;
};

export type DadosCadastro = {
  nome: string;
  email: string;
  senha: string;
};

export type DadosLogin = {
  email: string;
  senha: string;
};

export type DadosEvento = {
  titulo: string;
  descricao: string;
  iniciaEm: string;
  terminaEm: string;
  local: string;
  online: boolean;
  categoria: string;
  vagas: number;
  imagemUrl: string;
};
