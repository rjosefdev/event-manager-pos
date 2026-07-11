import type { ReactNode } from "react";

export function Campo({
  nome,
  rotulo,
  erro,
  dica,
  children,
}: {
  nome: string;
  rotulo: string;
  erro?: string;
  dica?: string;
  children: ReactNode;
}) {
  return (
    <div className="campo">
      <label htmlFor={nome}>{rotulo}</label>
      {children}
      {erro ? <p id={`erro-${nome}`} className="erro-campo">{erro}</p> : dica ? <p id={`dica-${nome}`} className="dica">{dica}</p> : null}
    </div>
  );
}
