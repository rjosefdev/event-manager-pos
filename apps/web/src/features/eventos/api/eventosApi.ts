import { API_URL, criarErroApi, criarHeadersAutenticados } from "@/lib/api";
import type { DadosEvento, Evento, InscritoEvento, Sessao } from "@/types/api";

export const eventosApi = {
  async listar(sessao: Sessao) {
    const resposta = await fetch(`${API_URL}/eventos`, {
      headers: criarHeadersAutenticados(sessao),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível carregar seus eventos.");
    }

    return resposta.json() as Promise<Evento[]>;
  },

  async salvar(sessao: Sessao, dados: DadosEvento, eventoId?: string) {
    const resposta = await fetch(eventoId ? `${API_URL}/eventos/${eventoId}` : `${API_URL}/eventos`, {
      method: eventoId ? "PUT" : "POST",
      headers: {
        ...criarHeadersAutenticados(sessao),
        "Content-Type": "application/json",
      },
      body: JSON.stringify(dados),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, eventoId ? "Não foi possível editar o evento." : "Não foi possível criar o evento.");
    }

    return resposta.json() as Promise<Evento>;
  },

  async listarInscricoes(sessao: Sessao, eventoId: string) {
    const resposta = await fetch(`${API_URL}/eventos/${eventoId}/inscricoes`, {
      headers: criarHeadersAutenticados(sessao),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível carregar os inscritos deste evento.");
    }

    return resposta.json() as Promise<InscritoEvento[]>;
  },

  async cancelar(sessao: Sessao, eventoId: string) {
    const resposta = await fetch(`${API_URL}/eventos/${eventoId}`, {
      method: "DELETE",
      headers: criarHeadersAutenticados(sessao),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível cancelar o evento.");
    }

    return resposta.json() as Promise<Evento>;
  },
};
