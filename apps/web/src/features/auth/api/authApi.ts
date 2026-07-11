import { API_URL, criarErroApi } from "@/lib/api";
import type { DadosCadastro, DadosLogin, Sessao, UsuarioSessao } from "@/types/api";

export const authApi = {
  async cadastrar(dados: DadosCadastro) {
    const resposta = await fetch(`${API_URL}/autenticacao/cadastro`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dados),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Não foi possível concluir o cadastro.");
    }

    return resposta.json() as Promise<{ nome: string }>;
  },

  async entrar(dados: DadosLogin) {
    const resposta = await fetch(`${API_URL}/autenticacao/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(dados),
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "E-mail ou senha inválidos.");
    }

    return resposta.json() as Promise<Sessao>;
  },

  async usuarioAtual(tokenAcesso: string, tipoToken: "Bearer") {
    const resposta = await fetch(`${API_URL}/usuarios/atual`, {
      headers: {
        Authorization: `${tipoToken} ${tokenAcesso}`,
      },
    });

    if (!resposta.ok) {
      throw await criarErroApi(resposta, "Sua sessão expirou ou não pôde ser restaurada. Entre novamente.");
    }

    return resposta.json() as Promise<UsuarioSessao>;
  },
};
