import type { ReactNode } from "react";

export function Drawer({
  aberto,
  titulo,
  onFechar,
  children,
}: {
  aberto: boolean;
  titulo: string;
  onFechar: () => void;
  children: ReactNode;
}) {
  if (!aberto) {
    return null;
  }

  return (
    <div className="drawer-overlay" role="presentation">
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="drawer-cabecalho">
          <h2>{titulo}</h2>
          <button className="botao-fechar" type="button" onClick={onFechar} aria-label="Fechar">×</button>
        </div>
        <div className="drawer-conteudo">
          {children}
        </div>
      </aside>
    </div>
  );
}
