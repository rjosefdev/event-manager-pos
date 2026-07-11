import type { EventoCatalogo, Inscricao } from "@/types/api";

export function AcoesInscricao({
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
  if (!inscricao) {
    return (
      <button type="button" onClick={() => onCriar(evento)} disabled={!evento.inscricaoPermitida || acaoInscricaoId === evento.id}>
        {acaoInscricaoId === evento.id ? "Inscrevendo..." : "Inscrever-se"}
      </button>
    );
  }

  if (inscricao.situacao === "ATIVA") {
    return (
      <button className="botao-secundario" type="button" onClick={() => onCancelar(inscricao)} disabled={acaoInscricaoId === inscricao.id}>
        {acaoInscricaoId === inscricao.id ? "Cancelando..." : "Cancelar inscrição"}
      </button>
    );
  }

  return (
    <button type="button" onClick={() => onReativar(inscricao)} disabled={!evento.inscricaoPermitida || acaoInscricaoId === inscricao.id}>
      {acaoInscricaoId === inscricao.id ? "Reativando..." : "Reativar inscrição"}
    </button>
  );
}
