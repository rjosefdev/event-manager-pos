"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/features/auth/api/authApi";
import { criarCookieSessao, criarCookieSessaoExpirado } from "@/features/auth/sessionCookie";
import type { Sessao } from "@/types/api";

const CHAVE_SESSAO = "event-manager:sessao";

type SessaoProtegida = {
  sessao: Sessao;
  encerrarSessao: () => void;
};

const SessaoContext = createContext<SessaoProtegida | null>(null);

export function AuthProvider({
  sessaoInicial,
  children,
}: {
  sessaoInicial: Sessao;
  children: ReactNode;
}) {
  const router = useRouter();
  const [sessao, setSessao] = useState<Sessao | null>(sessaoInicial);

  useEffect(() => {
    persistirSessao(sessaoInicial);
  }, [sessaoInicial]);

  const encerrarSessao = useCallback(() => {
    removerCredenciais();
    setSessao(null);
    router.replace("/login");
  }, [router]);

  if (!sessao) {
    return null;
  }

  return (
    <SessaoContext.Provider value={{ sessao, encerrarSessao }}>
      {children}
    </SessaoContext.Provider>
  );
}

export function useSessao() {
  const contexto = useContext(SessaoContext);

  if (!contexto) {
    throw new Error("useSessao deve ser usado dentro de AuthProvider.");
  }

  return contexto;
}

export function useSessaoPublica() {
  const router = useRouter();
  const [restaurandoSessao, setRestaurandoSessao] = useState(true);
  const [erroRestauracao, setErroRestauracao] = useState("");

  const salvarSessao = useCallback((novaSessao: Sessao) => {
    persistirSessao(novaSessao);
    setErroRestauracao("");
    router.replace(rotaPorPerfil(novaSessao));
  }, [router]);

  const encerrarSessao = useCallback(() => {
    removerCredenciais();
    setErroRestauracao("");
  }, []);

  useEffect(() => {
    let ativo = true;

    async function restaurarSessao() {
      const sessaoSalva = window.localStorage.getItem(CHAVE_SESSAO);
      if (!sessaoSalva) {
        setRestaurandoSessao(false);
        return;
      }

      try {
        const sessaoPersistida = JSON.parse(sessaoSalva) as Partial<Sessao>;
        if (!sessaoPersistida.tokenAcesso || sessaoPersistida.tipoToken !== "Bearer") {
          throw new Error("sessao-invalida");
        }

        const usuario = await authApi.usuarioAtual(sessaoPersistida.tokenAcesso, sessaoPersistida.tipoToken);
        const sessaoAtualizada: Sessao = {
          tokenAcesso: sessaoPersistida.tokenAcesso,
          tipoToken: "Bearer",
          expiraEm: sessaoPersistida.expiraEm ?? "",
          usuario,
        };

        if (!ativo) {
          return;
        }

        persistirSessao(sessaoAtualizada);
        router.replace(rotaPorPerfil(sessaoAtualizada));
      } catch {
        removerCredenciais();
        if (ativo) {
          setErroRestauracao("Sua sessão expirou ou não pôde ser restaurada. Entre novamente.");
        }
      } finally {
        if (ativo) {
          setRestaurandoSessao(false);
        }
      }
    }

    restaurarSessao();

    return () => {
      ativo = false;
    };
  }, [router]);

  return {
    restaurandoSessao,
    erroRestauracao,
    salvarSessao,
    encerrarSessao,
  };
}

function persistirSessao(sessao: Sessao) {
  window.localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao));
  document.cookie = criarCookieSessao(sessao.tokenAcesso, sessao.expiraEm);
}

function removerCredenciais() {
  window.localStorage.removeItem(CHAVE_SESSAO);
  document.cookie = criarCookieSessaoExpirado();
}

function rotaPorPerfil(sessao: Sessao) {
  return sessao.usuario.perfil === "ORGANIZADOR" ? "/organizador" : "/participante";
}
