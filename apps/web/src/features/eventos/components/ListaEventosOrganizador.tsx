import { EventoImagem } from "@/components/EventoImagem";
import { classeSituacaoOrganizador, formatarDataHora, formatarSituacao } from "@/lib/formatters";
import type { Evento } from "@/types/api";

export function ListaEventosOrganizador({
  eventos,
  carregando,
  carregandoInscritosId,
  cancelandoEventoId,
  onAbrirInscritos,
  onEditar,
  onCancelar,
}: {
  eventos: Evento[];
  carregando: boolean;
  carregandoInscritosId: string | null;
  cancelandoEventoId: string | null;
  onAbrirInscritos: (evento: Evento) => void;
  onEditar: (evento: Evento) => void;
  onCancelar: (evento: Evento) => void;
}) {
  if (carregando) {
    return <p className="estado-lista" role="status">Carregando eventos...</p>;
  }

  if (eventos.length === 0) {
    return <p className="estado-lista">Nenhum evento encontrado com os filtros atuais.</p>;
  }

  return (
    <ul className="eventos-grid">
      {eventos.map((evento) => (
        <li key={evento.id} className="evento-card">
          <EventoImagem evento={evento} />
          <div className="evento-card-corpo">
            <div>
              <strong>{evento.titulo}</strong>
              <span>{evento.categoria}</span>
            </div>
            <p>{evento.descricao}</p>
            <dl>
              <div>
                <dt>Quando</dt>
                <dd>{formatarDataHora(evento.iniciaEm)} até {formatarDataHora(evento.terminaEm)}</dd>
              </div>
              <div>
                <dt>{evento.online ? "Online" : "Local"}</dt>
                <dd>{evento.local}</dd>
              </div>
              <div>
                <dt>Vagas</dt>
                <dd>{evento.vagas}</dd>
              </div>
            </dl>
          </div>
          <span className={`situacao ${classeSituacaoOrganizador(evento)}`}>
            {evento.cancelado ? "Cancelado" : formatarSituacao(evento.situacaoTemporal)}
          </span>
          <div className="evento-card-rodape">
            <button className="botao-lista" type="button" onClick={() => onAbrirInscritos(evento)} disabled={carregandoInscritosId === evento.id}>
              {carregandoInscritosId === evento.id ? "Carregando..." : "Inscritos"}
            </button>
            {evento.situacaoTemporal !== "FINALIZADO" && (
              <>
                <button className="botao-lista" type="button" onClick={() => onEditar(evento)}>
                  Editar
                </button>
                {!evento.cancelado && (
                  <button className="botao-lista perigo" type="button" onClick={() => onCancelar(evento)} disabled={cancelandoEventoId === evento.id}>
                    {cancelandoEventoId === evento.id ? "Cancelando..." : "Cancelar"}
                  </button>
                )}
              </>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
