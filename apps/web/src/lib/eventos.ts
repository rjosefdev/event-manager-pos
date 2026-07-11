import type { Evento, EventoCatalogo, FiltrosEventos, Inscricao } from "@/types/api";

export function ordenarEventosPorInicio(a: Pick<Evento, "iniciaEm">, b: Pick<Evento, "iniciaEm">) {
  return new Date(a.iniciaEm).getTime() - new Date(b.iniciaEm).getTime();
}

export function filtrosPadrao(): FiltrosEventos {
  return {
    busca: "",
    categoria: "",
    ordenacao: "data-asc",
  };
}

export function categoriasEventos<T extends Pick<Evento, "categoria">>(eventos: T[]) {
  return Array.from(new Set(eventos.map((evento) => evento.categoria).filter(Boolean))).sort((a, b) => a.localeCompare(b, "pt-BR"));
}

export function filtrarEOrdenarEventos<T extends Pick<Evento, "titulo" | "descricao" | "categoria" | "local" | "iniciaEm">>(
  eventos: T[],
  filtros: FiltrosEventos
) {
  const termo = normalizarTexto(filtros.busca);

  return [...eventos]
    .filter((evento) => {
      const categoriaCombina = !filtros.categoria || evento.categoria === filtros.categoria;
      const textoCombina = !termo || normalizarTexto(`${evento.titulo} ${evento.descricao} ${evento.categoria} ${evento.local}`).includes(termo);
      return categoriaCombina && textoCombina;
    })
    .sort((a, b) => {
      const diferenca = new Date(a.iniciaEm).getTime() - new Date(b.iniciaEm).getTime();
      return filtros.ordenacao === "data-asc" ? diferenca : -diferenca;
    });
}

export function sincronizarEventosComInscricoes(eventos: EventoCatalogo[], inscricoes: Inscricao[]) {
  return eventos.map((evento) => {
    const inscricao = inscricoes.find((item) => item.eventoId === evento.id);
    return inscricao?.evento ?? evento;
  });
}

function normalizarTexto(valor: string) {
  return valor.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim();
}
