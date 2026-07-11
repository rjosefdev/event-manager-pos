import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { AppHeader } from "@/components/AppHeader";
import { BarraFiltrosEventos } from "@/components/BarraFiltrosEventos";
import { Drawer } from "@/components/Drawer";
import { eventosApi } from "@/features/eventos/api/eventosApi";
import { FormularioEvento } from "@/features/eventos/components/FormularioEvento";
import { ListaEventosOrganizador } from "@/features/eventos/components/ListaEventosOrganizador";
import { ListaInscritosEvento } from "@/features/eventos/components/ListaInscritosEvento";
import { categoriasEventos, filtrarEOrdenarEventos, filtrosPadrao, ordenarEventosPorInicio } from "@/lib/eventos";
import { ApiError } from "@/lib/api";
import { paraIso } from "@/lib/formatters";
import type { ErrosCampos, Evento, InscritoEvento, Sessao } from "@/types/api";

export function AreaOrganizador({ sessao, onSair }: { sessao: Sessao; onSair: () => void }) {
  const [eventos, setEventos] = useState<Evento[]>([]);
  const [inscritosPorEvento, setInscritosPorEvento] = useState<Record<string, InscritoEvento[]>>({});
  const [eventoEmEdicao, setEventoEmEdicao] = useState<Evento | null>(null);
  const [drawerEventoAberto, setDrawerEventoAberto] = useState(false);
  const [eventoInscritosSelecionado, setEventoInscritosSelecionado] = useState<Evento | null>(null);
  const [filtros, setFiltros] = useState(filtrosPadrao());
  const [carregandoEventos, setCarregandoEventos] = useState(true);
  const [carregandoInscritosId, setCarregandoInscritosId] = useState<string | null>(null);
  const [criandoEvento, setCriandoEvento] = useState(false);
  const [atualizandoEvento, setAtualizandoEvento] = useState(false);
  const [cancelandoEventoId, setCancelandoEventoId] = useState<string | null>(null);
  const [errosEvento, setErrosEvento] = useState<ErrosCampos>({});
  const [erroEvento, setErroEvento] = useState("");
  const [sucessoEvento, setSucessoEvento] = useState("");
  const eventosFiltrados = filtrarEOrdenarEventos(eventos, filtros);

  useEffect(() => {
    let ativo = true;

    eventosApi.listar(sessao)
      .then((eventosCarregados) => {
        if (ativo) {
          setEventos(eventosCarregados);
        }
      })
      .catch((erro: Error) => {
        if (ativo) {
          setErroEvento(erro.message || "Não foi possível conectar à API para carregar seus eventos.");
        }
      })
      .finally(() => {
        if (ativo) {
          setCarregandoEventos(false);
        }
      });

    return () => {
      ativo = false;
    };
  }, [sessao]);

  async function criarEvento(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const elementoFormulario = event.currentTarget;
    const formulario = new FormData(elementoFormulario);
    const editando = Boolean(eventoEmEdicao);
    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    if (editando) {
      setAtualizandoEvento(true);
    } else {
      setCriandoEvento(true);
    }

    const dados = {
      titulo: String(formulario.get("titulo") ?? ""),
      descricao: String(formulario.get("descricao") ?? ""),
      iniciaEm: paraIso(String(formulario.get("iniciaEm") ?? "")),
      terminaEm: paraIso(String(formulario.get("terminaEm") ?? "")),
      local: String(formulario.get("local") ?? ""),
      online: formulario.get("online") === "on",
      categoria: String(formulario.get("categoria") ?? ""),
      vagas: Number(formulario.get("vagas") ?? 0),
      imagemUrl: String(formulario.get("imagemUrl") ?? ""),
    };

    try {
      const eventoSalvo = await eventosApi.salvar(sessao, dados, eventoEmEdicao?.id);
      setEventos((atuais) => {
        if (editando) {
          return atuais.map((evento) => evento.id === eventoSalvo.id ? eventoSalvo : evento).sort(ordenarEventosPorInicio);
        }
        return [...atuais, eventoSalvo].sort(ordenarEventosPorInicio);
      });
      setSucessoEvento(editando ? "Evento atualizado." : "Evento criado e vinculado à sua conta.");
      setEventoEmEdicao(null);
      setDrawerEventoAberto(false);
      elementoFormulario.reset();
    } catch (erro) {
      if (erro instanceof ApiError) {
        setErrosEvento(erro.erros ?? {});
        setErroEvento(erro.message);
        return;
      }
      setErroEvento(editando ? "Não foi possível conectar à API para editar o evento." : "Não foi possível conectar à API para criar o evento.");
    } finally {
      setCriandoEvento(false);
      setAtualizandoEvento(false);
    }
  }

  async function abrirInscritos(evento: Evento) {
    setEventoInscritosSelecionado(evento);

    if (inscritosPorEvento[evento.id]) {
      return;
    }

    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    setCarregandoInscritosId(evento.id);

    try {
      const inscritos = await eventosApi.listarInscricoes(sessao, evento.id);
      setInscritosPorEvento((atuais) => ({ ...atuais, [evento.id]: inscritos }));
    } catch (erro) {
      const mensagem = erro instanceof Error ? erro.message : "Não foi possível conectar à API para carregar os inscritos.";
      setErroEvento(mensagem);
    } finally {
      setCarregandoInscritosId(null);
    }
  }

  async function cancelarEvento(evento: Evento) {
    const confirmado = window.confirm(`Cancelar "${evento.titulo}"?`);
    if (!confirmado) {
      return;
    }

    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    setCancelandoEventoId(evento.id);

    try {
      const eventoCancelado = await eventosApi.cancelar(sessao, evento.id);
      setEventos((atuais) => atuais.map((item) => item.id === eventoCancelado.id ? eventoCancelado : item));
      if (eventoEmEdicao?.id === eventoCancelado.id) {
        setEventoEmEdicao(null);
      }
      setSucessoEvento("Evento cancelado sem remover o histórico.");
    } catch (erro) {
      const mensagem = erro instanceof Error ? erro.message : "Não foi possível conectar à API para cancelar o evento.";
      setErroEvento(mensagem);
    } finally {
      setCancelandoEventoId(null);
    }
  }

  function abrirNovoEvento() {
    setEventoEmEdicao(null);
    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    setDrawerEventoAberto(true);
  }

  function iniciarEdicao(evento: Evento) {
    setEventoEmEdicao(evento);
    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    setDrawerEventoAberto(true);
  }

  function cancelarEdicao() {
    setEventoEmEdicao(null);
    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    setDrawerEventoAberto(false);
  }

  return (
    <>
      <AppHeader titulo="Meus eventos" sessao={sessao} onSair={onSair}>
        <button className="botao-compacto" type="button" onClick={abrirNovoEvento}>Novo evento</button>
      </AppHeader>

      <section className="app-conteudo" aria-label="Eventos próprios">
        {erroEvento && <p className="mensagem erro-geral" role="alert">{erroEvento}</p>}
        {sucessoEvento && <p className="mensagem sucesso" role="status">{sucessoEvento}</p>}

        <BarraFiltrosEventos
          filtros={filtros}
          categorias={categoriasEventos(eventos)}
          total={eventosFiltrados.length}
          onChange={setFiltros}
        />

        {carregandoEventos ? (
          <p className="estado-lista" role="status">Carregando eventos...</p>
        ) : eventos.length === 0 ? (
          <p className="estado-lista">Nenhum evento criado para esta conta.</p>
        ) : (
          <ListaEventosOrganizador
            eventos={eventosFiltrados}
            carregando={false}
            carregandoInscritosId={carregandoInscritosId}
            cancelandoEventoId={cancelandoEventoId}
            onAbrirInscritos={abrirInscritos}
            onEditar={iniciarEdicao}
            onCancelar={cancelarEvento}
          />
        )}
      </section>

      <Drawer aberto={drawerEventoAberto} titulo={eventoEmEdicao ? "Editar evento" : "Novo evento"} onFechar={cancelarEdicao}>
        <FormularioEvento
          evento={eventoEmEdicao}
          erros={errosEvento}
          erroGeral={erroEvento}
          criando={criandoEvento}
          atualizando={atualizandoEvento}
          onSubmit={criarEvento}
          onCancelarEdicao={cancelarEdicao}
        />
      </Drawer>

      <Drawer aberto={Boolean(eventoInscritosSelecionado)} titulo="Inscritos" onFechar={() => setEventoInscritosSelecionado(null)}>
        {eventoInscritosSelecionado && (
          <ListaInscritosEvento
            evento={eventoInscritosSelecionado}
            inscritos={inscritosPorEvento[eventoInscritosSelecionado.id] ?? []}
            carregando={carregandoInscritosId === eventoInscritosSelecionado.id}
          />
        )}
      </Drawer>
    </>
  );
}
