import type { FormEvent } from "react";
import { useState } from "react";
import { ApiError } from "@/lib/api";
import { authApi } from "@/features/auth/api/authApi";
import { CadastroForm } from "@/features/auth/components/CadastroForm";
import { LoginForm } from "@/features/auth/components/LoginForm";
import { SessionRestorer } from "@/features/auth/components/SessionRestorer";
import type { ErrosCampos, ModoAutenticacao, Sessao } from "@/types/api";

export function AuthCard({
  modoInicial = "cadastro",
  restaurandoSessao,
  erroRestauracao,
  onSessaoAutenticada,
}: {
  modoInicial?: ModoAutenticacao;
  restaurandoSessao: boolean;
  erroRestauracao: string;
  onSessaoAutenticada: (sessao: Sessao) => void;
}) {
  const [modo, setModo] = useState<ModoAutenticacao>(modoInicial);
  const [erros, setErros] = useState<ErrosCampos>({});
  const [erroGeral, setErroGeral] = useState("");
  const [mostrarErroRestauracao, setMostrarErroRestauracao] = useState(true);
  const [sucesso, setSucesso] = useState("");
  const [enviando, setEnviando] = useState(false);

  function trocarModo(proximoModo: ModoAutenticacao) {
    setModo(proximoModo);
    setErros({});
    setErroGeral("");
    setMostrarErroRestauracao(false);
    setSucesso("");
  }

  async function cadastrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const elementoFormulario = event.currentTarget;
    setErros({});
    setErroGeral("");
    setMostrarErroRestauracao(false);
    setSucesso("");
    setEnviando(true);

    const formulario = new FormData(elementoFormulario);
    const dados = {
      nome: String(formulario.get("nome") ?? ""),
      email: String(formulario.get("email") ?? ""),
      senha: String(formulario.get("senha") ?? ""),
    };

    try {
      const participante = await authApi.cadastrar(dados);
      setSucesso(`${participante.nome}, seu cadastro foi concluído. Agora você já pode entrar.`);
      setModo("login");
      elementoFormulario.reset();
    } catch (erro) {
      if (erro instanceof ApiError) {
        setErros(erro.erros ?? {});
        setErroGeral(erro.message);
        return;
      }
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
    setMostrarErroRestauracao(false);
    setSucesso("");
    setEnviando(true);

    const formulario = new FormData(elementoFormulario);
    const dados = {
      email: String(formulario.get("email") ?? ""),
      senha: String(formulario.get("senha") ?? ""),
    };

    try {
      const novaSessao = await authApi.entrar(dados);
      onSessaoAutenticada(novaSessao);
      setSucesso(`${novaSessao.usuario.nome}, você entrou no Event Manager.`);
      elementoFormulario.reset();
    } catch (erro) {
      if (erro instanceof ApiError) {
        setErros(erro.erros ?? {});
        setErroGeral(erro.message);
        return;
      }
      setErroGeral("Não foi possível conectar à API. Tente novamente em instantes.");
    } finally {
      setEnviando(false);
    }
  }

  if (restaurandoSessao) {
    return <SessionRestorer />;
  }

  return (
    <>
      <CabecalhoFormulario modo={modo} />
      {modo === "cadastro" ? (
        <CadastroForm
          erros={erros}
          erroGeral={erroGeral || (mostrarErroRestauracao ? erroRestauracao : "")}
          sucesso={sucesso}
          enviando={enviando}
          onSubmit={cadastrar}
          onIrParaLogin={() => trocarModo("login")}
        />
      ) : (
        <LoginForm
          erros={erros}
          erroGeral={erroGeral || (mostrarErroRestauracao ? erroRestauracao : "")}
          sucesso={sucesso}
          enviando={enviando}
          onSubmit={entrar}
          onIrParaCadastro={() => trocarModo("cadastro")}
        />
      )}
    </>
  );
}

function CabecalhoFormulario({ modo }: { modo: ModoAutenticacao }) {
  return (
    <div>
      <p className="etiqueta">{modo === "cadastro" ? "Comece agora" : "Acesse sua conta"}</p>
      <h2>{modo === "cadastro" ? "Crie sua conta" : "Entrar"}</h2>
      <p className="apoio">
        {modo === "cadastro"
          ? "Preencha os dados abaixo. Leva menos de um minuto."
          : "Use o mesmo acesso de Participante ou Organizador."}
      </p>
    </div>
  );
}
