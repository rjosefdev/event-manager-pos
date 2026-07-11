import { API_URL, ProblemaApiError, lerProblemaApi } from "@/lib/api";
import type { Evento, InscritoEvento } from "@/types/api";

export type EventoPayload = {
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

async function falharComProblema(resposta: Response, mensagemPadrao: string): Promise<never> {
  const problema = await lerProblemaApi(resposta);
  throw new ProblemaApiError(problema, mensagemPadrao);
}

export async function listarEventos(autorizacao: string) {
  const resposta = await fetch(`${API_URL}/eventos`, {
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível carregar seus eventos.");
  }

  return resposta.json() as Promise<Evento[]>;
}

export async function salvarEvento(dados: EventoPayload, autorizacao: string, eventoId?: string) {
  const editando = Boolean(eventoId);
  const resposta = await fetch(editando ? `${API_URL}/eventos/${eventoId}` : `${API_URL}/eventos`, {
    method: editando ? "PUT" : "POST",
    headers: {
      Authorization: autorizacao,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(dados),
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, editando ? "Não foi possível editar o evento." : "Não foi possível criar o evento.");
  }

  return resposta.json() as Promise<Evento>;
}

export async function listarInscritosEvento(eventoId: string, autorizacao: string) {
  const resposta = await fetch(`${API_URL}/eventos/${eventoId}/inscricoes`, {
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível carregar os inscritos deste evento.");
  }

  return resposta.json() as Promise<InscritoEvento[]>;
}

export async function cancelarEvento(eventoId: string, autorizacao: string) {
  const resposta = await fetch(`${API_URL}/eventos/${eventoId}`, {
    method: "DELETE",
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível cancelar o evento.");
  }

  return resposta.json() as Promise<Evento>;
}
