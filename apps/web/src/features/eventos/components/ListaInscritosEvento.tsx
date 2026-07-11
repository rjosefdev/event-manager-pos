import { EventoImagem } from "@/components/EventoImagem";
import { classeStatusInscricao, formatarDataHora, formatarSituacaoInscricao, rotuloInscricao } from "@/lib/formatters";
import type { Evento, InscritoEvento } from "@/types/api";

export function ListaInscritosEvento({
  evento,
  inscritos,
  carregando,
}: {
  evento: Evento;
  inscritos: InscritoEvento[];
  carregando: boolean;
}) {
  return (
    <section className="drawer-evento" aria-label={`Inscritos em ${evento.titulo}`}>
      <EventoImagem evento={evento} destaque />
      <h2>{evento.titulo}</h2>

      {carregando ? (
        <p className="estado-lista" role="status">Carregando inscritos...</p>
      ) : inscritos.length === 0 ? (
        <p className="estado-lista">Nenhuma inscrição registrada para este evento.</p>
      ) : (
        <ul className="inscritos-lista">
          {inscritos.map((inscrito) => (
            <li key={inscrito.id}>
              <div>
                <strong>{inscrito.participante.nome}</strong>
                <span>{inscrito.participante.email}</span>
              </div>
              <dl>
                <div>
                  <dt>Situação</dt>
                  <dd>{formatarSituacaoInscricao(inscrito.situacao)}</dd>
                </div>
                <div>
                  <dt>Inscrito em</dt>
                  <dd>{formatarDataHora(inscrito.inscritoEm)}</dd>
                </div>
                <div>
                  <dt>Cancelamento</dt>
                  <dd>{inscrito.canceladoEm ? formatarDataHora(inscrito.canceladoEm) : "-"}</dd>
                </div>
              </dl>
              <span className={classeStatusInscricao(inscrito)}>
                {rotuloInscricao(inscrito)}
              </span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
