import { useEffect, useState } from "react";

import type { EventoCatalogo, Inscricao, Sessao } from "@/types/api";
import {
  classeSituacao,
  classeStatusInscricao,
  formatarDataHora,
  formatarSituacaoInscricao,
  motivoBloqueioInscricao,
  rotuloInscricao,
  rotuloSituacaoCatalogo,
  sincronizarEventosComInscricoes,
} from "@/lib/formatters";
import {
  buscarEventoCatalogo,
  cancelarInscricao as cancelarInscricaoApi,
  criarInscricao as criarInscricaoApi,
  listarEventosCatalogo,
  listarInscricoes,
  reativarInscricao as reativarInscricaoApi,
} from "@/features/catalogo/api";

export function AreaParticipante({ sessao }: { sessao: Sessao }) {
  const [eventos, setEventos] = useState<EventoCatalogo[]>([]);
  const [inscricoes, setInscricoes] = useState<Inscricao[]>([]);
  const [eventoSelecionado, setEventoSelecionado] = useState<EventoCatalogo | null>(null);
  const [carregandoCatalogo, setCarregandoCatalogo] = useState(true);
  const [carregandoDetalhesId, setCarregandoDetalhesId] = useState<string | null>(null);
  const [acaoInscricaoId, setAcaoInscricaoId] = useState<string | null>(null);
  const [erroCatalogo, setErroCatalogo] = useState("");
  const [sucessoInscricao, setSucessoInscricao] = useState("");
  const autorizacao = `${sessao.tipoToken} ${sessao.tokenAcesso}`;

  useEffect(() => {
    let ativo = true;

    async function carregarAreaParticipante() {
      try {
        const [catalogo, inscricoesCarregadas] = await Promise.all([
          listarEventosCatalogo(autorizacao),
          listarInscricoes(autorizacao),
        ]);

        if (ativo) {
          const catalogoAtualizado = sincronizarEventosComInscricoes(catalogo, inscricoesCarregadas);
          setEventos(catalogoAtualizado);
          setInscricoes(inscricoesCarregadas);
          setEventoSelecionado(catalogoAtualizado[0] ?? null);
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
  }, [autorizacao]);

  const inscricaoSelecionada = eventoSelecionado
    ? inscricoes.find((inscricao) => inscricao.eventoId === eventoSelecionado.id)
    : undefined;

  async function abrirDetalhes(eventoId: string) {
    setErroCatalogo("");
    setSucessoInscricao("");
    setCarregandoDetalhesId(eventoId);

    try {
      const detalhes = await buscarEventoCatalogo(eventoId, autorizacao);
      const inscricaoDoEvento = inscricoes.find((inscricao) => inscricao.eventoId === detalhes.id);
      const detalhesAtualizados = inscricaoDoEvento?.evento ?? detalhes;
      setEventoSelecionado(detalhesAtualizados);
      setEventos((atuais) => atuais.map((evento) => evento.id === detalhesAtualizados.id ? detalhesAtualizados : evento));
    } catch (erro) {
      const mensagem = erro instanceof Error ? erro.message : "Não foi possível conectar à API para abrir os detalhes do evento.";
      setErroCatalogo(mensagem);
    } finally {
      setCarregandoDetalhesId(null);
    }
  }

  async function criarInscricao(evento: EventoCatalogo) {
    await executarAcaoInscricao(evento.id, "Inscrição criada.", async () => {
      return criarInscricaoApi(evento.id, autorizacao);
    });
  }

  async function cancelarInscricao(inscricao: Inscricao) {
    await executarAcaoInscricao(inscricao.id, "Inscrição cancelada.", async () => {
      return cancelarInscricaoApi(inscricao.id, autorizacao);
    });
  }

  async function reativarInscricao(inscricao: Inscricao) {
    await executarAcaoInscricao(inscricao.id, "Inscrição reativada.", async () => {
      return reativarInscricaoApi(inscricao.id, autorizacao);
    });
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
    <section className="catalogo-workspace" aria-label="Catálogo de eventos">
      <div>
        <p className="etiqueta">Catálogo</p>
        <h3>Eventos para participar</h3>
      </div>

      {erroCatalogo && <p className="mensagem erro-geral" role="alert">{erroCatalogo}</p>}
      {sucessoInscricao && <p className="mensagem sucesso" role="status">{sucessoInscricao}</p>}

      {carregandoCatalogo ? (
        <p className="estado-lista" role="status">Carregando catálogo e inscrições...</p>
      ) : eventos.length === 0 ? (
        <p className="estado-lista">Nenhum evento publicado no catálogo.</p>
      ) : (
        <div className="catalogo-grid">
          <ul className="catalogo-lista">
            {eventos.map((evento) => (
              <li key={evento.id} className={eventoSelecionado?.id === evento.id ? "selecionado" : undefined}>
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
                <span className={`situacao ${classeSituacao(evento)}`}>
                  {rotuloSituacaoCatalogo(evento)}
                </span>
                <p className={classeStatusInscricao(inscricoes.find((inscricao) => inscricao.eventoId === evento.id))}>
                  {rotuloInscricao(inscricoes.find((inscricao) => inscricao.eventoId === evento.id))}
                </p>
                <button className="botao-lista" type="button" onClick={() => abrirDetalhes(evento.id)} disabled={carregandoDetalhesId === evento.id}>
                  {carregandoDetalhesId === evento.id ? "Abrindo..." : "Ver detalhes"}
                </button>
              </li>
            ))}
          </ul>

          {eventoSelecionado && (
            <article className="detalhes-catalogo" aria-label="Detalhes do evento">
              <span className={`situacao ${classeSituacao(eventoSelecionado)}`}>
                {rotuloSituacaoCatalogo(eventoSelecionado)}
              </span>
              <h3>{eventoSelecionado.titulo}</h3>
              <p>{eventoSelecionado.descricao}</p>
              <dl>
                <div>
                  <dt>Início</dt>
                  <dd>{formatarDataHora(eventoSelecionado.iniciaEm)}</dd>
                </div>
                <div>
                  <dt>Término</dt>
                  <dd>{formatarDataHora(eventoSelecionado.terminaEm)}</dd>
                </div>
                <div>
                  <dt>{eventoSelecionado.online ? "Online" : "Local"}</dt>
                  <dd>{eventoSelecionado.local}</dd>
                </div>
                <div>
                  <dt>Vagas</dt>
                  <dd>{eventoSelecionado.vagasDisponiveis} de {eventoSelecionado.vagas}</dd>
                </div>
              </dl>
              <p className={eventoSelecionado.inscricaoPermitida ? "status-inscricao permitido" : "status-inscricao bloqueado"}>
                {eventoSelecionado.inscricaoPermitida ? "Inscrição permitida" : motivoBloqueioInscricao(eventoSelecionado)}
              </p>
              <AcoesInscricao
                evento={eventoSelecionado}
                inscricao={inscricaoSelecionada}
                acaoInscricaoId={acaoInscricaoId}
                onCriar={criarInscricao}
                onCancelar={cancelarInscricao}
                onReativar={reativarInscricao}
              />
            </article>
          )}
        </div>
      )}

      <section className="minhas-inscricoes" aria-label="Minhas inscrições">
        <div>
          <p className="etiqueta">Minhas inscrições</p>
          <h3>Histórico</h3>
        </div>

        {carregandoCatalogo ? (
          <p className="estado-lista" role="status">Carregando inscrições...</p>
        ) : inscricoes.length === 0 ? (
          <p className="estado-lista">Nenhuma inscrição criada para esta conta.</p>
        ) : (
          <ul>
            {inscricoes.map((inscricao) => (
              <li key={inscricao.id}>
                <div>
                  <strong>{inscricao.evento.titulo}</strong>
                  <span>{inscricao.evento.categoria}</span>
                </div>
                <dl>
                  <div>
                    <dt>Situação</dt>
                    <dd>{formatarSituacaoInscricao(inscricao.situacao)}</dd>
                  </div>
                  <div>
                    <dt>Inscrito em</dt>
                    <dd>{formatarDataHora(inscricao.inscritoEm)}</dd>
                  </div>
                  <div>
                    <dt>Evento</dt>
                    <dd>{formatarDataHora(inscricao.evento.iniciaEm)}</dd>
                  </div>
                </dl>
                <div className="acoes-evento">
                  <button className="botao-lista" type="button" onClick={() => abrirDetalhes(inscricao.eventoId)}>
                    Ver evento
                  </button>
                  {inscricao.situacao === "ATIVA" ? (
                    <button className="botao-lista perigo" type="button" onClick={() => cancelarInscricao(inscricao)} disabled={acaoInscricaoId === inscricao.id}>
                      {acaoInscricaoId === inscricao.id ? "Cancelando..." : "Cancelar"}
                    </button>
                  ) : (
                    <button className="botao-lista" type="button" onClick={() => reativarInscricao(inscricao)} disabled={acaoInscricaoId === inscricao.id || !inscricao.evento.inscricaoPermitida}>
                      {acaoInscricaoId === inscricao.id ? "Reativando..." : "Reativar"}
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>
    </section>
  );
}

function AcoesInscricao({
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
