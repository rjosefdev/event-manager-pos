import type { ProblemaApi } from "@/types/api";

export const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export class ProblemaApiError extends Error {
  constructor(public problema: ProblemaApi, mensagemPadrao: string) {
    super(problema.detail ?? mensagemPadrao);
  }
}

export async function lerProblemaApi(resposta: Response): Promise<ProblemaApi> {
  try {
    return (await resposta.json()) as ProblemaApi;
  } catch {
    return {};
  }
}
