import { API_URL, criarErroApi, criarHeadersAutenticados } from "@/lib/api";
import type { EventoCatalogo, Inscricao, Sessao } from "@/types/api";

export const inscricoesApi = {
  async carregarAreaParticipante(sessao: Sessao) {
    const [respostaCatalogo, respostaInscricoes] = await Promise.all([
      fetch(`${API_URL}/catalogo/eventos`, {
        headers: criarHeadersAutenticados(sessao),
      }),
      fetch(`${API_URL}/inscricoes`, {
        headers: criarHeadersAutenticados(sessao),
      }),
    ]);

    if (!respostaCatalogo.ok) {
      throw await criarErroApi(respostaCatalogo, "Não foi possível carregar o catálogo.");
    }
    if (!respostaInscricoes.ok) {
      throw await criarErroApi(respostaInscricoes, "Não foi possível carregar suas inscrições.");
    }

    return {
      catalogo: (await respostaCatalogo.json()) as EventoCatalogo[],
      inscricoes: (await respostaInscricoes.json()) as Inscricao[],
    };
  },

  async detalharEvento(sessao: Sessao, eventoId: string) {
    const resposta = await fetch(`${API_URL}/catalogo/eventos/${eventoId}`, {
      headers: criarHeadersAutenticados(sessao),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível abrir os detalhes do evento.");
    }

    return resposta.json() as Promise<EventoCatalogo>;
  },

  async criar(sessao: Sessao, eventoId: string) {
    const resposta = await fetch(`${API_URL}/inscricoes`, {
      method: "POST",
      headers: {
        ...criarHeadersAutenticados(sessao),
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ eventoId }),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível criar a inscrição.");
    }

    return resposta.json() as Promise<Inscricao>;
  },

  async cancelar(sessao: Sessao, inscricaoId: string) {
    const resposta = await fetch(`${API_URL}/inscricoes/${inscricaoId}`, {
      method: "DELETE",
      headers: criarHeadersAutenticados(sessao),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível cancelar a inscrição.");
    }

    return resposta.json() as Promise<Inscricao>;
  },

  async reativar(sessao: Sessao, inscricaoId: string) {
    const resposta = await fetch(`${API_URL}/inscricoes/${inscricaoId}/reativar`, {
      method: "PATCH",
      headers: criarHeadersAutenticados(sessao),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível reativar a inscrição.");
    }

    return resposta.json() as Promise<Inscricao>;
  },
};
