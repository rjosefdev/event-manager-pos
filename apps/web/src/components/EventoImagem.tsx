import { resolverImagemEvento } from "@/lib/formatters";
import type { Evento } from "@/types/api";

export function EventoImagem({
  evento,
  destaque = false,
}: {
  evento: Pick<Evento, "titulo" | "imagemUrl">;
  destaque?: boolean;
}) {
  const imagem = resolverImagemEvento(evento.imagemUrl);
  const inicial = evento.titulo.trim().charAt(0).toUpperCase() || "E";

  return (
    <div className={destaque ? "evento-imagem destaque" : "evento-imagem"} aria-hidden={!imagem}>
      {imagem && (
        /* eslint-disable-next-line @next/next/no-img-element -- Imagens de eventos podem vir da API ou de URLs externas cadastradas. */
        <img
          src={imagem}
          alt={`Imagem de ${evento.titulo}`}
          loading="lazy"
          onError={(event) => {
            event.currentTarget.hidden = true;
          }}
        />
      )}
      <span aria-hidden="true">{inicial}</span>
    </div>
  );
}
