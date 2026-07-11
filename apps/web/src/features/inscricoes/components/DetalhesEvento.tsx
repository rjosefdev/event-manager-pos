import { EventoImagem } from "@/components/EventoImagem";
import { AcoesInscricao } from "@/features/inscricoes/components/AcoesInscricao";
import { classeSituacao, formatarDataHora, motivoBloqueioInscricao, rotuloSituacaoCatalogo } from "@/lib/formatters";
import type { EventoCatalogo, Inscricao } from "@/types/api";

export function DetalhesEvento({
  evento,
  inscricao,
  acaoInscricaoId,
  onCriar,
  onCancelar,
  onReativar,
}: {
  evento: EventoCatalogo;
  inscricao?: Inscricao;
  acaoInscricaoId: string | null;
  onCriar: (evento: EventoCatalogo) => void;
  onCancelar: (inscricao: Inscricao) => void;
  onReativar: (inscricao: Inscricao) => void;
}) {
  return (
    <article className="drawer-evento">
      <EventoImagem evento={evento} destaque />
      <span className={`situacao ${classeSituacao(evento)}`}>
        {rotuloSituacaoCatalogo(evento)}
      </span>
      <h2>{evento.titulo}</h2>
      <p>{evento.descricao}</p>
      <dl>
        <div>
          <dt>Início</dt>
          <dd>{formatarDataHora(evento.iniciaEm)}</dd>
        </div>
        <div>
          <dt>Término</dt>
          <dd>{formatarDataHora(evento.terminaEm)}</dd>
        </div>
        <div>
          <dt>{evento.online ? "Online" : "Local"}</dt>
          <dd>{evento.local}</dd>
        </div>
        <div>
          <dt>Vagas</dt>
          <dd>{evento.vagasDisponiveis} de {evento.vagas}</dd>
        </div>
      </dl>
      <p className={evento.inscricaoPermitida ? "status-inscricao permitido" : "status-inscricao bloqueado"}>
        {evento.inscricaoPermitida ? "Inscrição permitida" : motivoBloqueioInscricao(evento)}
      </p>
      <AcoesInscricao
        evento={evento}
        inscricao={inscricao}
        acaoInscricaoId={acaoInscricaoId}
        onCriar={onCriar}
        onCancelar={onCancelar}
        onReativar={onReativar}
      />
    </article>
  );
}
