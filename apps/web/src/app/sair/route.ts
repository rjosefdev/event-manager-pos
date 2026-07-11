import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { CAMINHO_COOKIE_SESSAO, NOME_COOKIE_SESSAO } from "@/features/auth/sessionCookie";

export function GET(request: NextRequest) {
  const destino = new URL("/login", request.url);
  const motivo = request.nextUrl.searchParams.get("motivo");

  if (motivo) {
    destino.searchParams.set("sessao", motivo);
  }

  const resposta = NextResponse.redirect(destino);
  resposta.cookies.set({
    name: NOME_COOKIE_SESSAO,
    value: "",
    path: CAMINHO_COOKIE_SESSAO,
    maxAge: 0,
  });

  return resposta;
}
