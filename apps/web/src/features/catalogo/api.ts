import { API_URL, ProblemaApiError, lerProblemaApi } from "@/lib/api";
import type { EventoCatalogo, Inscricao } from "@/types/api";

async function falharComProblema(resposta: Response, mensagemPadrao: string): Promise<never> {
  const problema = await lerProblemaApi(resposta);
  throw new ProblemaApiError(problema, mensagemPadrao);
}

export async function listarEventosCatalogo(autorizacao: string) {
  const resposta = await fetch(`${API_URL}/catalogo/eventos`, {
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível carregar o catálogo.");
  }

  return resposta.json() as Promise<EventoCatalogo[]>;
}

export async function buscarEventoCatalogo(eventoId: string, autorizacao: string) {
  const resposta = await fetch(`${API_URL}/catalogo/eventos/${eventoId}`, {
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível abrir os detalhes do evento.");
  }

  return resposta.json() as Promise<EventoCatalogo>;
}

export async function listarInscricoes(autorizacao: string) {
  const resposta = await fetch(`${API_URL}/inscricoes`, {
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível carregar suas inscrições.");
  }

  return resposta.json() as Promise<Inscricao[]>;
}

export async function criarInscricao(eventoId: string, autorizacao: string) {
  const resposta = await fetch(`${API_URL}/inscricoes`, {
    method: "POST",
    headers: {
      Authorization: autorizacao,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ eventoId }),
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível criar a inscrição.");
  }

  return resposta.json() as Promise<Inscricao>;
}

export async function cancelarInscricao(inscricaoId: string, autorizacao: string) {
  const resposta = await fetch(`${API_URL}/inscricoes/${inscricaoId}`, {
    method: "DELETE",
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível cancelar a inscrição.");
  }

  return resposta.json() as Promise<Inscricao>;
}

export async function reativarInscricao(inscricaoId: string, autorizacao: string) {
  const resposta = await fetch(`${API_URL}/inscricoes/${inscricaoId}/reativar`, {
    method: "PATCH",
    headers: { Authorization: autorizacao },
  });

  if (!resposta.ok) {
    await falharComProblema(resposta, "Não foi possível reativar a inscrição.");
  }

  return resposta.json() as Promise<Inscricao>;
}
