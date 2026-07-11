import type { Sessao } from "@/types/api";
import { AreaParticipante } from "@/features/catalogo/AreaParticipante";
import { AreaOrganizador } from "@/features/eventos/AreaOrganizador";

export function AreaUsuario({ sessao, onSair }: { sessao: Sessao; onSair: () => void }) {
  const organizador = sessao.usuario.perfil === "ORGANIZADOR";

  return (
    <div className="area-usuario">
      <div className="cabecalho-area">
        <div>
          <p className="etiqueta">{organizador ? "Área do organizador" : "Área do participante"}</p>
          <h2>{organizador ? "Eventos sob sua gestão" : "Seu catálogo"}</h2>
          <p className="apoio">
            {organizador
              ? "Crie eventos e acompanhe somente os itens vinculados à sua conta."
              : "Consulte eventos disponíveis e acompanhe suas próprias inscrições."}
          </p>
        </div>
        <button className="botao-compacto" type="button" onClick={onSair}>Sair</button>
      </div>

      <div className="sessao">
        <p className="usuario-logado">{sessao.usuario.nome}</p>
        <p>{sessao.usuario.email}</p>
        <span>{sessao.usuario.perfil}</span>
      </div>

      {organizador ? (
        <AreaOrganizador sessao={sessao} />
      ) : (
        <AreaParticipante sessao={sessao} />
      )}
    </div>
  );
}
