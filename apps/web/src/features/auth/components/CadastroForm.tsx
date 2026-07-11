import type { FormEvent } from "react";
import { Campo } from "@/components/Campo";
import type { ErrosCampos } from "@/types/api";

export function CadastroForm({
  erros,
  erroGeral,
  sucesso,
  enviando,
  onSubmit,
  onIrParaLogin,
}: {
  erros: ErrosCampos;
  erroGeral: string;
  sucesso: string;
  enviando: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onIrParaLogin: () => void;
}) {
  return (
    <form onSubmit={onSubmit} noValidate>
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
      <button className="botao-secundario" type="button" onClick={onIrParaLogin}>
        Já tenho conta
      </button>
    </form>
  );
}
