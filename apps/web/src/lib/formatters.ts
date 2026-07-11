import type { Evento, EventoCatalogo, Inscricao, SituacaoInscricao, SituacaoTemporal } from "@/types/api";

export function paraIso(valor: string) {
  if (!valor) {
    return "";
  }
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return valor;
  }
  return data.toISOString();
}

export function paraDatetimeLocal(valor: string) {
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return "";
  }

  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  const hora = String(data.getHours()).padStart(2, "0");
  const minuto = String(data.getMinutes()).padStart(2, "0");
  return `${ano}-${mes}-${dia}T${hora}:${minuto}`;
}

export function ordenarEventosPorInicio(a: Evento, b: Evento) {
  return new Date(a.iniciaEm).getTime() - new Date(b.iniciaEm).getTime();
}

export function sincronizarEventosComInscricoes(eventos: EventoCatalogo[], inscricoes: Inscricao[]) {
  return eventos.map((evento) => {
    const inscricao = inscricoes.find((item) => item.eventoId === evento.id);
    return inscricao?.evento ?? evento;
  });
}

export function formatarDataHora(valor: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(valor));
}

export function formatarSituacao(situacao: SituacaoTemporal) {
  const rotulos = {
    FUTURO: "Futuro",
    EM_ANDAMENTO: "Em andamento",
    FINALIZADO: "Finalizado",
  };

  return rotulos[situacao];
}

export function classeSituacao(evento: Pick<EventoCatalogo, "cancelado" | "situacaoTemporal" | "vagasDisponiveis">) {
  if (evento.cancelado) {
    return "cancelado";
  }
  if (evento.vagasDisponiveis === 0 && evento.situacaoTemporal !== "FINALIZADO") {
    return "lotado";
  }
  return evento.situacaoTemporal.toLowerCase().replace("_", "-");
}

export function rotuloSituacaoCatalogo(evento: Pick<EventoCatalogo, "cancelado" | "situacaoTemporal" | "vagasDisponiveis">) {
  if (evento.cancelado) {
    return "Cancelado";
  }
  if (evento.vagasDisponiveis === 0 && evento.situacaoTemporal !== "FINALIZADO") {
    return "Lotado";
  }
  return formatarSituacao(evento.situacaoTemporal);
}

export function classeStatusInscricao(inscricao?: Pick<Inscricao, "situacao">) {
  if (!inscricao) {
    return "status-vinculo";
  }
  return `status-vinculo ${inscricao.situacao.toLowerCase()}`;
}

export function rotuloInscricao(inscricao?: Pick<Inscricao, "situacao">) {
  if (!inscricao) {
    return "Sem inscrição";
  }
  return inscricao.situacao === "ATIVA" ? "Inscrição ativa" : "Inscrição cancelada";
}

export function formatarSituacaoInscricao(situacao: SituacaoInscricao) {
  return situacao === "ATIVA" ? "Ativa" : "Cancelada";
}

export function motivoBloqueioInscricao(evento: EventoCatalogo) {
  if (evento.cancelado) {
    return "Inscrição bloqueada: evento cancelado";
  }
  if (evento.situacaoTemporal === "FINALIZADO") {
    return "Inscrição bloqueada: evento finalizado";
  }
  if (evento.vagasDisponiveis === 0) {
    return "Inscrição bloqueada: evento lotado";
  }
  return "Inscrição bloqueada";
}
