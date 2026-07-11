import { BarraFiltrosEventos } from "@/components/BarraFiltrosEventos";
import { EventoImagem } from "@/components/EventoImagem";
import { categoriasEventos } from "@/lib/eventos";
import { classeSituacao, classeStatusInscricao, formatarDataHora, rotuloInscricao, rotuloSituacaoCatalogo } from "@/lib/formatters";
import type { EventoCatalogo, FiltrosEventos, Inscricao } from "@/types/api";

export function CatalogoEventos({
  eventos,
  eventosFiltrados,
  inscricoes,
  filtros,
  carregando,
  carregandoDetalhesId,
  onChangeFiltros,
  onAbrirDetalhes,
}: {
  eventos: EventoCatalogo[];
  eventosFiltrados: EventoCatalogo[];
  inscricoes: Inscricao[];
  filtros: FiltrosEventos;
  carregando: boolean;
  carregandoDetalhesId: string | null;
  onChangeFiltros: (filtros: FiltrosEventos) => void;
  onAbrirDetalhes: (eventoId: string) => void;
}) {
  return (
    <>
      <BarraFiltrosEventos
        filtros={filtros}
        categorias={categoriasEventos(eventos)}
        total={eventosFiltrados.length}
        onChange={onChangeFiltros}
      />

      {carregando ? (
        <p className="estado-lista" role="status">Carregando catálogo e inscrições...</p>
      ) : eventos.length === 0 ? (
        <p className="estado-lista">Nenhum evento publicado no catálogo.</p>
      ) : eventosFiltrados.length === 0 ? (
        <p className="estado-lista">Nenhum evento encontrado com os filtros atuais.</p>
      ) : (
        <ul className="eventos-grid">
          {eventosFiltrados.map((evento) => {
            const inscricao = inscricoes.find((item) => item.eventoId === evento.id);
            return (
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
                      <dd>{formatarDataHora(evento.iniciaEm)}</dd>
                    </div>
                    <div>
                      <dt>Vagas livres</dt>
                      <dd>{evento.vagasDisponiveis}</dd>
                    </div>
                  </dl>
                </div>
                <span className={`situacao ${classeSituacao(evento)}`}>
                  {rotuloSituacaoCatalogo(evento)}
                </span>
                <div className="evento-card-rodape">
                  <p className={classeStatusInscricao(inscricao)}>
                    {rotuloInscricao(inscricao)}
                  </p>
                  <button className="botao-lista" type="button" onClick={() => onAbrirDetalhes(evento.id)} disabled={carregandoDetalhesId === evento.id}>
                    {carregandoDetalhesId === evento.id ? "Abrindo..." : "Ver detalhes"}
                  </button>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </>
  );
}
