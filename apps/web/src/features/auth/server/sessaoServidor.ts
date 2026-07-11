import { cache } from "react";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { API_URL } from "@/lib/api";
import { NOME_COOKIE_SESSAO, normalizarTokenCookie } from "@/features/auth/sessionCookie";
import type { Sessao, UsuarioSessao } from "@/types/api";

export const obterSessaoProtegida = cache(async () => {
  const cookieStore = await cookies();
  const tokenCookie = cookieStore.get(NOME_COOKIE_SESSAO)?.value;

  if (!tokenCookie) {
    redirect("/login");
  }

  const tokenAcesso = normalizarTokenCookie(tokenCookie);

  try {
    const resposta = await fetch(`${API_URL}/usuarios/atual`, {
      headers: {
        Authorization: `Bearer ${tokenAcesso}`,
      },
      cache: "no-store",
    });

    if (!resposta.ok) {
      redirect("/sair?motivo=sessao-invalida");
    }

    const usuario = (await resposta.json()) as UsuarioSessao;

    return {
      tokenAcesso,
      tipoToken: "Bearer",
      expiraEm: obterExpiracaoJwt(tokenAcesso),
      usuario,
    } satisfies Sessao;
  } catch (erro) {
    if (isRedirectError(erro)) {
      throw erro;
    }

    redirect("/sair?motivo=sessao-invalida");
  }
});

export async function exigirPerfil(perfilEsperado: UsuarioSessao["perfil"]) {
  const sessao = await obterSessaoProtegida();

  if (sessao.usuario.perfil !== perfilEsperado) {
    redirect(sessao.usuario.perfil === "ORGANIZADOR" ? "/organizador" : "/participante");
  }

  return sessao;
}

function obterExpiracaoJwt(tokenAcesso: string) {
  try {
    const payloadBase64 = tokenAcesso.split(".")[1];
    if (!payloadBase64) {
      return "";
    }

    const payload = JSON.parse(Buffer.from(payloadBase64, "base64url").toString("utf8")) as { exp?: number };
    if (!payload.exp) {
      return "";
    }

    return new Date(payload.exp * 1000).toISOString();
  } catch {
    return "";
  }
}

function isRedirectError(erro: unknown) {
  return typeof erro === "object"
    && erro !== null
    && "digest" in erro
    && typeof erro.digest === "string"
    && erro.digest.startsWith("NEXT_REDIRECT");
}
