import { useEffect, useState } from "react";
import { AppHeader } from "@/components/AppHeader";
import { Drawer } from "@/components/Drawer";
import { inscricoesApi } from "@/features/inscricoes/api/inscricoesApi";
import { CatalogoEventos } from "@/features/inscricoes/components/CatalogoEventos";
import { DetalhesEvento } from "@/features/inscricoes/components/DetalhesEvento";
import { HistoricoInscricoes } from "@/features/inscricoes/components/HistoricoInscricoes";
import { filtrarEOrdenarEventos, filtrosPadrao, sincronizarEventosComInscricoes } from "@/lib/eventos";
import type { AbaParticipante, EventoCatalogo, Inscricao, Sessao } from "@/types/api";

export function AreaParticipante({ sessao, onSair }: { sessao: Sessao; onSair: () => void }) {
  const [eventos, setEventos] = useState<EventoCatalogo[]>([]);
  const [inscricoes, setInscricoes] = useState<Inscricao[]>([]);
  const [eventoSelecionado, setEventoSelecionado] = useState<EventoCatalogo | null>(null);
  const [abaAtiva, setAbaAtiva] = useState<AbaParticipante>("catalogo");
  const [filtros, setFiltros] = useState(filtrosPadrao());
  const [carregandoCatalogo, setCarregandoCatalogo] = useState(true);
  const [carregandoDetalhesId, setCarregandoDetalhesId] = useState<string | null>(null);
  const [acaoInscricaoId, setAcaoInscricaoId] = useState<string | null>(null);
  const [erroCatalogo, setErroCatalogo] = useState("");
  const [sucessoInscricao, setSucessoInscricao] = useState("");

  useEffect(() => {
    let ativo = true;

    async function carregarAreaParticipante() {
      try {
        const { catalogo, inscricoes: inscricoesCarregadas } = await inscricoesApi.carregarAreaParticipante(sessao);

        if (ativo) {
          const catalogoAtualizado = sincronizarEventosComInscricoes(catalogo, inscricoesCarregadas);
          setEventos(catalogoAtualizado);
          setInscricoes(inscricoesCarregadas);
        }
      } catch (erro) {
        if (ativo) {
          const mensagem = erro instanceof Error ? erro.message : "Não foi possível conectar à API para carregar o catálogo.";
          setErroCatalogo(mensagem);
        }
      } finally {
        if (ativo) {
          setCarregandoCatalogo(false);
        }
      }
    }

    carregarAreaParticipante();

    return () => {
      ativo = false;
    };
  }, [sessao]);

  const inscricaoSelecionada = eventoSelecionado
    ? inscricoes.find((inscricao) => inscricao.eventoId === eventoSelecionado.id)
    : undefined;
  const eventosFiltrados = filtrarEOrdenarEventos(eventos, filtros);
  const inscricoesOrdenadas = [...inscricoes].sort((a, b) => new Date(b.inscritoEm).getTime() - new Date(a.inscritoEm).getTime());

  async function abrirDetalhes(eventoId: string) {
    setErroCatalogo("");
    setSucessoInscricao("");
    setCarregandoDetalhesId(eventoId);

    try {
      const detalhes = await inscricoesApi.detalharEvento(sessao, eventoId);
      const inscricaoDoEvento = inscricoes.find((inscricao) => inscricao.eventoId === detalhes.id);
      const detalhesAtualizados = inscricaoDoEvento?.evento ?? detalhes;
      setEventoSelecionado(detalhesAtualizados);
      setAbaAtiva("catalogo");
      setEventos((atuais) => atuais.map((evento) => evento.id === detalhesAtualizados.id ? detalhesAtualizados : evento));
    } catch (erro) {
      const mensagem = erro instanceof Error ? erro.message : "Não foi possível conectar à API para abrir os detalhes do evento.";
      setErroCatalogo(mensagem);
    } finally {
      setCarregandoDetalhesId(null);
    }
  }

  async function criarInscricao(evento: EventoCatalogo) {
    await executarAcaoInscricao(evento.id, "Inscrição criada.", () => inscricoesApi.criar(sessao, evento.id));
  }

  async function cancelarInscricao(inscricao: Inscricao) {
    await executarAcaoInscricao(inscricao.id, "Inscrição cancelada.", () => inscricoesApi.cancelar(sessao, inscricao.id));
  }

  async function reativarInscricao(inscricao: Inscricao) {
    await executarAcaoInscricao(inscricao.id, "Inscrição reativada.", () => inscricoesApi.reativar(sessao, inscricao.id));
  }

  async function executarAcaoInscricao(
    chaveAcao: string,
    mensagemSucesso: string,
    acao: () => Promise<Inscricao>
  ) {
    setErroCatalogo("");
    setSucessoInscricao("");
    setAcaoInscricaoId(chaveAcao);

    try {
      const inscricaoAtualizada = await acao();
      aplicarInscricaoAtualizada(inscricaoAtualizada);
      setSucessoInscricao(mensagemSucesso);
    } catch (erro) {
      const mensagem = erro instanceof Error ? erro.message : "Não foi possível alterar a inscrição.";
      setErroCatalogo(mensagem);
    } finally {
      setAcaoInscricaoId(null);
    }
  }

  function aplicarInscricaoAtualizada(inscricaoAtualizada: Inscricao) {
    setInscricoes((atuais) => {
      const existe = atuais.some((inscricao) => inscricao.id === inscricaoAtualizada.id);
      if (existe) {
        return atuais.map((inscricao) => inscricao.id === inscricaoAtualizada.id ? inscricaoAtualizada : inscricao);
      }
      return [inscricaoAtualizada, ...atuais];
    });
    setEventos((atuais) => atuais.map((evento) => evento.id === inscricaoAtualizada.eventoId ? inscricaoAtualizada.evento : evento));
    setEventoSelecionado((atual) => atual?.id === inscricaoAtualizada.eventoId ? inscricaoAtualizada.evento : atual);
  }

  return (
    <>
      <AppHeader titulo="Eventos disponíveis" sessao={sessao} onSair={onSair} />

      <section className="app-conteudo" aria-label="Área do participante">
        <div className="abas" role="tablist" aria-label="Navegação do participante">
          <button type="button" className={abaAtiva === "catalogo" ? "ativo" : undefined} onClick={() => setAbaAtiva("catalogo")}>
            Eventos disponíveis
          </button>
          <button type="button" className={abaAtiva === "inscricoes" ? "ativo" : undefined} onClick={() => setAbaAtiva("inscricoes")}>
            Minhas inscrições
          </button>
        </div>

        {erroCatalogo && <p className="mensagem erro-geral" role="alert">{erroCatalogo}</p>}
        {sucessoInscricao && <p className="mensagem sucesso" role="status">{sucessoInscricao}</p>}

        {abaAtiva === "catalogo" ? (
          <CatalogoEventos
            eventos={eventos}
            eventosFiltrados={eventosFiltrados}
            inscricoes={inscricoes}
            filtros={filtros}
            carregando={carregandoCatalogo}
            carregandoDetalhesId={carregandoDetalhesId}
            onChangeFiltros={setFiltros}
            onAbrirDetalhes={abrirDetalhes}
          />
        ) : (
          <HistoricoInscricoes
            inscricoes={inscricoesOrdenadas}
            carregando={carregandoCatalogo}
            acaoInscricaoId={acaoInscricaoId}
            onAbrirDetalhes={abrirDetalhes}
            onCancelar={cancelarInscricao}
            onReativar={reativarInscricao}
          />
        )}
      </section>

      <Drawer aberto={Boolean(eventoSelecionado)} titulo="Detalhes do evento" onFechar={() => setEventoSelecionado(null)}>
        {eventoSelecionado && (
          <DetalhesEvento
            evento={eventoSelecionado}
            inscricao={inscricaoSelecionada}
            acaoInscricaoId={acaoInscricaoId}
            onCriar={criarInscricao}
            onCancelar={cancelarInscricao}
            onReativar={reativarInscricao}
          />
        )}
      </Drawer>
    </>
  );
}
