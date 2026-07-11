"use client";

import type { FormEvent } from "react";
import { useEffect, useState } from "react";

import { Campo } from "@/components/Campo";
import { CabecalhoFormulario } from "@/features/auth/CabecalhoFormulario";
import { buscarUsuarioAtual, cadastrarParticipante, login } from "@/features/auth/api";
import { AreaUsuario } from "@/features/usuarios/AreaUsuario";
import type { ErrosCampos, Sessao } from "@/types/api";

type Modo = "cadastro" | "login";

const CHAVE_SESSAO = "event-manager:sessao";

export default function Home() {
  const [modo, setModo] = useState<Modo>("cadastro");
  const [sessao, setSessao] = useState<Sessao | null>(null);
  const [restaurandoSessao, setRestaurandoSessao] = useState(true);
  const [erros, setErros] = useState<ErrosCampos>({});
  const [erroGeral, setErroGeral] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [enviando, setEnviando] = useState(false);

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

        const resultado = await buscarUsuarioAtual({
          tipoToken: sessaoPersistida.tipoToken,
          tokenAcesso: sessaoPersistida.tokenAcesso,
        });

        if (!resultado.ok) {
          throw new Error(resultado.problema.detail ?? "token-recusado");
        }

        const usuario = resultado.usuario;
        const sessaoAtualizada: Sessao = {
          tokenAcesso: sessaoPersistida.tokenAcesso,
          tipoToken: "Bearer",
          expiraEm: sessaoPersistida.expiraEm ?? "",
          usuario,
        };

        if (!ativo) {
          return;
        }

        window.localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessaoAtualizada));
        setSessao(sessaoAtualizada);
      } catch {
        window.localStorage.removeItem(CHAVE_SESSAO);
        if (ativo) {
          setErroGeral("Sua sessão expirou ou não pôde ser restaurada. Entre novamente.");
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
  }, []);

  function trocarModo(proximoModo: Modo) {
    setModo(proximoModo);
    setErros({});
    setErroGeral("");
    setSucesso("");
  }

  async function cadastrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const elementoFormulario = event.currentTarget;
    setErros({});
    setErroGeral("");
    setSucesso("");
    setEnviando(true);

    const formulario = new FormData(elementoFormulario);
    const dados = {
      nome: String(formulario.get("nome") ?? ""),
      email: String(formulario.get("email") ?? ""),
      senha: String(formulario.get("senha") ?? ""),
    };

    try {
      const resultado = await cadastrarParticipante(dados);

      if (!resultado.ok) {
        setErros(resultado.problema.erros ?? {});
        setErroGeral(resultado.problema.detail ?? "Não foi possível concluir o cadastro.");
        return;
      }

      const participante = resultado.participante;
      setSucesso(`${participante.nome}, seu cadastro foi concluído. Agora você já pode entrar.`);
      setModo("login");
      elementoFormulario.reset();
    } catch {
      setErroGeral("Não foi possível conectar à API. Tente novamente em instantes.");
    } finally {
      setEnviando(false);
    }
  }

  async function entrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const elementoFormulario = event.currentTarget;
    setErros({});
    setErroGeral("");
    setSucesso("");
    setEnviando(true);

    const formulario = new FormData(elementoFormulario);
    const dados = {
      email: String(formulario.get("email") ?? ""),
      senha: String(formulario.get("senha") ?? ""),
    };

    try {
      const resultado = await login(dados);

      if (!resultado.ok) {
        setErros(resultado.problema.erros ?? {});
        setErroGeral(resultado.problema.detail ?? "E-mail ou senha inválidos.");
        return;
      }

      const novaSessao = resultado.sessao;
      window.localStorage.setItem(CHAVE_SESSAO, JSON.stringify(novaSessao));
      setSessao(novaSessao);
      setSucesso(`${novaSessao.usuario.nome}, você entrou no Event Manager.`);
      elementoFormulario.reset();
    } catch {
      setErroGeral("Não foi possível conectar à API. Tente novamente em instantes.");
    } finally {
      setEnviando(false);
    }
  }

  function sair() {
    window.localStorage.removeItem(CHAVE_SESSAO);
    setSessao(null);
    setSucesso("");
    setErroGeral("");
  }

  return (
    <main className="pagina-cadastro">
      <section className="apresentacao" aria-labelledby="titulo-cadastro">
        <p className="marca">Event Manager</p>
        <h1 id="titulo-cadastro">Encontre seu próximo evento.</h1>
        <p className="resumo">
          Crie sua conta de Participante para descobrir eventos e acompanhar suas inscrições em um só lugar.
        </p>
        <ul className="beneficios" aria-label="Benefícios da conta">
          <li>Catálogo completo de eventos</li>
          <li>Inscrições rápidas e organizadas</li>
          <li>Seus dados protegidos</li>
        </ul>
      </section>

      <section className="cartao-cadastro" aria-label="Formulário de autenticação">
        {restaurandoSessao ? (
          <div className="estado-sessao" role="status">
            <p className="etiqueta">Sessão</p>
            <h2>Restaurando acesso</h2>
            <p className="apoio">Validando seu token antes de abrir a aplicação.</p>
          </div>
        ) : sessao ? (
          <AreaUsuario sessao={sessao} onSair={sair} />
        ) : modo === "cadastro" ? (
          <>
            <CabecalhoFormulario modo={modo} />
            <form onSubmit={cadastrar} noValidate>
              <Campo nome="nome" rotulo="Nome completo" erro={erros.nome}>
                <input id="nome" name="nome" autoComplete="name" maxLength={120} aria-invalid={Boolean(erros.nome)} aria-describedby={erros.nome ? "erro-nome" : undefined} />
              </Campo>

              <Campo nome="email" rotulo="E-mail" erro={erros.email}>
                <input id="email" name="email" type="email" autoComplete="email" maxLength={254} aria-invalid={Boolean(erros.email)} aria-describedby={erros.email ? "erro-email" : undefined} />
              </Campo>

              <Campo nome="senha" rotulo="Senha" erro={erros.senha} dica="Use pelo menos 8 caracteres.">
                <input id="senha" name="senha" type="password" autoComplete="new-password" aria-invalid={Boolean(erros.senha)} aria-describedby={erros.senha ? "erro-senha" : "dica-senha"} />
              </Campo>

              {erroGeral && <p className="mensagem erro-geral" role="alert">{erroGeral}</p>}
              {sucesso && <p className="mensagem sucesso" role="status">{sucesso}</p>}

              <button type="submit" disabled={enviando}>
                {enviando ? "Criando conta..." : "Criar minha conta"}
              </button>
              <button className="botao-secundario" type="button" onClick={() => trocarModo("login")}>
                Já tenho conta
              </button>
            </form>
          </>
        ) : (
          <>
            <CabecalhoFormulario modo={modo} />
            <form onSubmit={entrar} noValidate>
              <Campo nome="email" rotulo="E-mail" erro={erros.email}>
                <input id="email" name="email" type="email" autoComplete="email" maxLength={254} aria-invalid={Boolean(erros.email)} aria-describedby={erros.email ? "erro-email" : undefined} />
              </Campo>

              <Campo nome="senha" rotulo="Senha" erro={erros.senha}>
                <input id="senha" name="senha" type="password" autoComplete="current-password" aria-invalid={Boolean(erros.senha)} aria-describedby={erros.senha ? "erro-senha" : undefined} />
              </Campo>

              {erroGeral && <p className="mensagem erro-geral" role="alert">{erroGeral}</p>}
              {sucesso && <p className="mensagem sucesso" role="status">{sucesso}</p>}

              <button type="submit" disabled={enviando}>
                {enviando ? "Entrando..." : "Entrar"}
              </button>
              <button className="botao-secundario" type="button" onClick={() => trocarModo("cadastro")}>
                Criar conta
              </button>
            </form>
          </>
        )}
      </section>
    </main>
  );
}
