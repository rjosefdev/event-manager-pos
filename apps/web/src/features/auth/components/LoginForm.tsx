import type { FormEvent } from "react";
import { Campo } from "@/components/Campo";
import type { ErrosCampos } from "@/types/api";

export function LoginForm({
  erros,
  erroGeral,
  sucesso,
  enviando,
  onSubmit,
  onIrParaCadastro,
}: {
  erros: ErrosCampos;
  erroGeral: string;
  sucesso: string;
  enviando: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onIrParaCadastro: () => void;
}) {
  return (
    <form onSubmit={onSubmit} noValidate>
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
      <button className="botao-secundario" type="button" onClick={onIrParaCadastro}>
        Criar conta
      </button>
    </form>
  );
}
