"use client";

import { FormEvent, useState } from "react";

type Campos = "nome" | "email" | "senha";
type ErrosCampos = Partial<Record<Campos, string>>;

type ProblemaApi = {
  detail?: string;
  erros?: ErrosCampos;
};

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export default function Home() {
  const [erros, setErros] = useState<ErrosCampos>({});
  const [erroGeral, setErroGeral] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [enviando, setEnviando] = useState(false);

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
      const resposta = await fetch(`${API_URL}/autenticacao/cadastro`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dados),
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErros(problema.erros ?? {});
        setErroGeral(problema.detail ?? "Não foi possível concluir o cadastro.");
        return;
      }

      const participante = (await resposta.json()) as { nome: string };
      setSucesso(`${participante.nome}, seu cadastro foi concluído. Agora você já pode entrar.`);
      elementoFormulario.reset();
    } catch {
      setErroGeral("Não foi possível conectar à API. Tente novamente em instantes.");
    } finally {
      setEnviando(false);
    }
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

      <section className="cartao-cadastro" aria-label="Formulário de cadastro">
        <div>
          <p className="etiqueta">Comece agora</p>
          <h2>Crie sua conta</h2>
          <p className="apoio">Preencha os dados abaixo. Leva menos de um minuto.</p>
        </div>

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
            {enviando ? "Criando conta…" : "Criar minha conta"}
          </button>
        </form>
      </section>
    </main>
  );
}

function Campo({
  nome,
  rotulo,
  erro,
  dica,
  children,
}: {
  nome: Campos;
  rotulo: string;
  erro?: string;
  dica?: string;
  children: React.ReactNode;
}) {
  return (
    <div className="campo">
      <label htmlFor={nome}>{rotulo}</label>
      {children}
      {erro ? <p id={`erro-${nome}`} className="erro-campo">{erro}</p> : dica ? <p id={`dica-${nome}`} className="dica">{dica}</p> : null}
    </div>
  );
}
