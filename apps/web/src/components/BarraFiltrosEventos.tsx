import type { FiltrosEventos, OrdenacaoEventos } from "@/types/api";

export function BarraFiltrosEventos({
  filtros,
  categorias,
  total,
  onChange,
}: {
  filtros: FiltrosEventos;
  categorias: string[];
  total: number;
  onChange: (filtros: FiltrosEventos) => void;
}) {
  return (
    <div className="barra-filtros" aria-label="Filtros da lista de eventos">
      <label>
        <span>Pesquisar</span>
        <input
          type="search"
          value={filtros.busca}
          onChange={(event) => onChange({ ...filtros, busca: event.currentTarget.value })}
          placeholder="Título, descrição ou local"
        />
      </label>
      <label>
        <span>Categoria</span>
        <select value={filtros.categoria} onChange={(event) => onChange({ ...filtros, categoria: event.currentTarget.value })}>
          <option value="">Todas</option>
          {categorias.map((categoria) => (
            <option key={categoria} value={categoria}>{categoria}</option>
          ))}
        </select>
      </label>
      <label>
        <span>Ordenar</span>
        <select value={filtros.ordenacao} onChange={(event) => onChange({ ...filtros, ordenacao: event.currentTarget.value as OrdenacaoEventos })}>
          <option value="data-asc">Data mais próxima</option>
          <option value="data-desc">Data mais distante</option>
        </select>
      </label>
      <p>{total} {total === 1 ? "evento" : "eventos"}</p>
    </div>
  );
}
