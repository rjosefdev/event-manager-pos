import { API_URL, lerProblemaApi } from "@/lib/api";
import type { Sessao, UsuarioSessao } from "@/types/api";

export async function cadastrarParticipante(dados: { nome: string; email: string; senha: string }) {
  const resposta = await fetch(`${API_URL}/autenticacao/cadastro`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });

  if (!resposta.ok) {
    return {
      ok: false as const,
      problema: await lerProblemaApi(resposta),
    };
  }

  return {
    ok: true as const,
    participante: (await resposta.json()) as { nome: string },
  };
}

export async function login(dados: { email: string; senha: string }) {
  const resposta = await fetch(`${API_URL}/autenticacao/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });

  if (!resposta.ok) {
    return {
      ok: false as const,
      problema: await lerProblemaApi(resposta),
    };
  }

  return {
    ok: true as const,
    sessao: (await resposta.json()) as Sessao,
  };
}

export async function buscarUsuarioAtual(sessao: Pick<Sessao, "tipoToken" | "tokenAcesso">) {
  const resposta = await fetch(`${API_URL}/usuarios/atual`, {
    headers: {
      Authorization: `${sessao.tipoToken} ${sessao.tokenAcesso}`,
    },
  });

  if (!resposta.ok) {
    return {
      ok: false as const,
      problema: await lerProblemaApi(resposta),
    };
  }

  return {
    ok: true as const,
    usuario: (await resposta.json()) as UsuarioSessao,
  };
}
