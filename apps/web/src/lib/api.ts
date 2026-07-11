import type { ErrosCampos, ProblemaApi, Sessao } from "@/types/api";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ApiError extends Error {
  erros?: ErrosCampos;

  constructor(message: string, erros?: ErrosCampos) {
    super(message);
    this.name = "ApiError";
    this.erros = erros;
  }
}

export function authorizationHeader(sessao: Sessao) {
  return `${sessao.tipoToken} ${sessao.tokenAcesso}`;
}

export function criarHeadersAutenticados(sessao: Sessao) {
  return {
    Authorization: authorizationHeader(sessao),
  };
}

export async function criarErroApi(resposta: Response, mensagemPadrao: string) {
  let problema: ProblemaApi = {};

  try {
    problema = (await resposta.json()) as ProblemaApi;
  } catch {
    problema = {};
  }

  return new ApiError(problema.detail ?? mensagemPadrao, problema.erros);
}
