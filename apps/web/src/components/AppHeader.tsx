import type { ReactNode } from "react";
import type { Sessao } from "@/types/api";

export function AppHeader({
  titulo,
  sessao,
  onSair,
  children,
}: {
  titulo: string;
  sessao: Sessao;
  onSair: () => void;
  children?: ReactNode;
}) {
  return (
    <header className="app-header">
      <div>
        <p className="marca">Event Manager</p>
        <h1>{titulo}</h1>
      </div>
      <div className="app-header-acoes">
        {children}
        <div className="usuario-resumo" aria-label="Usuário logado">
          <strong>{sessao.usuario.nome}</strong>
          <span>{sessao.usuario.perfil}</span>
        </div>
        <button className="botao-compacto" type="button" onClick={onSair}>Sair</button>
      </div>
    </header>
  );
}
