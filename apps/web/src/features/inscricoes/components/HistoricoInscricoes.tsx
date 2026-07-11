import { EventoImagem } from "@/components/EventoImagem";
import { formatarDataHora, formatarSituacaoInscricao } from "@/lib/formatters";
import type { Inscricao } from "@/types/api";

export function HistoricoInscricoes({
  inscricoes,
  carregando,
  acaoInscricaoId,
  onAbrirDetalhes,
  onCancelar,
  onReativar,
}: {
  inscricoes: Inscricao[];
  carregando: boolean;
  acaoInscricaoId: string | null;
  onAbrirDetalhes: (eventoId: string) => void;
  onCancelar: (inscricao: Inscricao) => void;
  onReativar: (inscricao: Inscricao) => void;
}) {
  if (carregando) {
    return <p className="estado-lista" role="status">Carregando inscrições...</p>;
  }

  if (inscricoes.length === 0) {
    return <p className="estado-lista">Nenhuma inscrição criada para esta conta.</p>;
  }

  return (
    <ul className="eventos-grid">
      {inscricoes.map((inscricao) => (
        <li key={inscricao.id} className="evento-card">
          <EventoImagem evento={inscricao.evento} />
          <div className="evento-card-corpo">
            <div>
              <strong>{inscricao.evento.titulo}</strong>
              <span>{inscricao.evento.categoria}</span>
            </div>
            <dl>
              <div>
                <dt>Situação</dt>
                <dd>{formatarSituacaoInscricao(inscricao.situacao)}</dd>
              </div>
              <div>
                <dt>Inscrito em</dt>
                <dd>{formatarDataHora(inscricao.inscritoEm)}</dd>
              </div>
              <div>
                <dt>Evento</dt>
                <dd>{formatarDataHora(inscricao.evento.iniciaEm)}</dd>
              </div>
            </dl>
          </div>
          <div className="evento-card-rodape">
            <button className="botao-lista" type="button" onClick={() => onAbrirDetalhes(inscricao.eventoId)}>
              Ver evento
            </button>
            {inscricao.situacao === "ATIVA" ? (
              <button className="botao-lista perigo" type="button" onClick={() => onCancelar(inscricao)} disabled={acaoInscricaoId === inscricao.id}>
                {acaoInscricaoId === inscricao.id ? "Cancelando..." : "Cancelar"}
              </button>
            ) : (
              <button className="botao-lista" type="button" onClick={() => onReativar(inscricao)} disabled={acaoInscricaoId === inscricao.id || !inscricao.evento.inscricaoPermitida}>
                {acaoInscricaoId === inscricao.id ? "Reativando..." : "Reativar"}
              </button>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
