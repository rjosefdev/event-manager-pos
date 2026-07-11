import type { FormEvent } from "react";
import { useEffect, useState } from "react";

import { Campo } from "@/components/Campo";
import type { ErrosCampos, Evento, InscritoEvento, Sessao } from "@/types/api";
import {
  classeStatusInscricao,
  formatarDataHora,
  formatarSituacao,
  formatarSituacaoInscricao,
  ordenarEventosPorInicio,
  paraDatetimeLocal,
  paraIso,
  rotuloInscricao,
} from "@/lib/formatters";
import { ProblemaApiError } from "@/lib/api";
import {
  cancelarEvento as cancelarEventoApi,
  listarEventos,
  listarInscritosEvento,
  salvarEvento,
} from "@/features/eventos/api";

export function AreaOrganizador({ sessao }: { sessao: Sessao }) {
  const [eventos, setEventos] = useState<Evento[]>([]);
  const [inscritosPorEvento, setInscritosPorEvento] = useState<Record<string, InscritoEvento[]>>({});
  const [eventoEmEdicao, setEventoEmEdicao] = useState<Evento | null>(null);
  const [eventoComInscritosAbertoId, setEventoComInscritosAbertoId] = useState<string | null>(null);
  const [carregandoEventos, setCarregandoEventos] = useState(true);
  const [carregandoInscritosId, setCarregandoInscritosId] = useState<string | null>(null);
  const [criandoEvento, setCriandoEvento] = useState(false);
  const [atualizandoEvento, setAtualizandoEvento] = useState(false);
  const [cancelandoEventoId, setCancelandoEventoId] = useState<string | null>(null);
  const [errosEvento, setErrosEvento] = useState<ErrosCampos>({});
  const [erroEvento, setErroEvento] = useState("");
  const [sucessoEvento, setSucessoEvento] = useState("");
  const autorizacao = `${sessao.tipoToken} ${sessao.tokenAcesso}`;

  useEffect(() => {
    let ativo = true;

    listarEventos(autorizacao)
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
  }, [autorizacao]);

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
      const eventoSalvo = await salvarEvento(dados, autorizacao, eventoEmEdicao?.id);
      setEventos((atuais) => {
        if (editando) {
          return atuais.map((evento) => evento.id === eventoSalvo.id ? eventoSalvo : evento).sort(ordenarEventosPorInicio);
        }
        return [...atuais, eventoSalvo].sort(ordenarEventosPorInicio);
      });
      setSucessoEvento(editando ? "Evento atualizado." : "Evento criado e vinculado à sua conta.");
      setEventoEmEdicao(null);
      elementoFormulario.reset();
    } catch (erro) {
      if (erro instanceof ProblemaApiError) {
        setErrosEvento(erro.problema.erros ?? {});
        setErroEvento(erro.message);
      } else {
        setErroEvento(editando ? "Não foi possível conectar à API para editar o evento." : "Não foi possível conectar à API para criar o evento.");
      }
    } finally {
      setCriandoEvento(false);
      setAtualizandoEvento(false);
    }
  }

  async function abrirInscritos(evento: Evento) {
    if (eventoComInscritosAbertoId === evento.id && inscritosPorEvento[evento.id]) {
      setEventoComInscritosAbertoId(null);
      return;
    }

    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
    setEventoComInscritosAbertoId(evento.id);
    setCarregandoInscritosId(evento.id);

    try {
      const inscritos = await listarInscritosEvento(evento.id, autorizacao);
      setInscritosPorEvento((atuais) => ({ ...atuais, [evento.id]: inscritos }));
    } catch {
      setErroEvento("Não foi possível conectar à API para carregar os inscritos.");
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
      const eventoCancelado = await cancelarEventoApi(evento.id, autorizacao);
      setEventos((atuais) => atuais.map((item) => item.id === eventoCancelado.id ? eventoCancelado : item));
      if (eventoEmEdicao?.id === eventoCancelado.id) {
        setEventoEmEdicao(null);
      }
      setSucessoEvento("Evento cancelado sem remover o histórico.");
    } catch {
      setErroEvento("Não foi possível conectar à API para cancelar o evento.");
    } finally {
      setCancelandoEventoId(null);
    }
  }

  function iniciarEdicao(evento: Evento) {
    setEventoEmEdicao(evento);
    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
  }

  function cancelarEdicao() {
    setEventoEmEdicao(null);
    setErrosEvento({});
    setErroEvento("");
    setSucessoEvento("");
  }

  return (
    <div className="organizador-workspace">
      <form key={eventoEmEdicao?.id ?? "novo-evento"} className="form-evento" onSubmit={criarEvento} noValidate>
        <div>
          <p className="etiqueta">{eventoEmEdicao ? "Editar evento" : "Novo evento"}</p>
          <h3>{eventoEmEdicao ? "Atualizar Evento" : "Criar Evento"}</h3>
        </div>

        <Campo nome="titulo" rotulo="Título" erro={errosEvento.titulo}>
          <input id="titulo" name="titulo" maxLength={120} defaultValue={eventoEmEdicao?.titulo ?? ""} aria-invalid={Boolean(errosEvento.titulo)} aria-describedby={errosEvento.titulo ? "erro-titulo" : undefined} />
        </Campo>

        <Campo nome="descricao" rotulo="Descrição" erro={errosEvento.descricao}>
          <textarea id="descricao" name="descricao" maxLength={2000} rows={4} defaultValue={eventoEmEdicao?.descricao ?? ""} aria-invalid={Boolean(errosEvento.descricao)} aria-describedby={errosEvento.descricao ? "erro-descricao" : undefined} />
        </Campo>

        <div className="form-grid-duplo">
          <Campo nome="iniciaEm" rotulo="Início" erro={errosEvento.iniciaEm}>
            <input id="iniciaEm" name="iniciaEm" type="datetime-local" defaultValue={eventoEmEdicao ? paraDatetimeLocal(eventoEmEdicao.iniciaEm) : ""} aria-invalid={Boolean(errosEvento.iniciaEm)} aria-describedby={errosEvento.iniciaEm ? "erro-iniciaEm" : undefined} />
          </Campo>

          <Campo nome="terminaEm" rotulo="Término" erro={errosEvento.terminaEm}>
            <input id="terminaEm" name="terminaEm" type="datetime-local" defaultValue={eventoEmEdicao ? paraDatetimeLocal(eventoEmEdicao.terminaEm) : ""} aria-invalid={Boolean(errosEvento.terminaEm)} aria-describedby={errosEvento.terminaEm ? "erro-terminaEm" : undefined} />
          </Campo>
        </div>

        <div className="form-grid-duplo">
          <Campo nome="local" rotulo="Local ou link" erro={errosEvento.local}>
            <input id="local" name="local" maxLength={180} defaultValue={eventoEmEdicao?.local ?? ""} aria-invalid={Boolean(errosEvento.local)} aria-describedby={errosEvento.local ? "erro-local" : undefined} />
          </Campo>

          <Campo nome="categoria" rotulo="Categoria" erro={errosEvento.categoria}>
            <input id="categoria" name="categoria" maxLength={80} defaultValue={eventoEmEdicao?.categoria ?? ""} aria-invalid={Boolean(errosEvento.categoria)} aria-describedby={errosEvento.categoria ? "erro-categoria" : undefined} />
          </Campo>
        </div>

        <div className="form-grid-duplo">
          <Campo nome="vagas" rotulo="Vagas" erro={errosEvento.vagas}>
            <input id="vagas" name="vagas" type="number" min={1} inputMode="numeric" defaultValue={eventoEmEdicao?.vagas ?? ""} aria-invalid={Boolean(errosEvento.vagas)} aria-describedby={errosEvento.vagas ? "erro-vagas" : undefined} />
          </Campo>

          <Campo nome="imagemUrl" rotulo="Imagem opcional" erro={errosEvento.imagemUrl}>
            <input id="imagemUrl" name="imagemUrl" type="url" maxLength={500} defaultValue={eventoEmEdicao?.imagemUrl ?? ""} aria-invalid={Boolean(errosEvento.imagemUrl)} aria-describedby={errosEvento.imagemUrl ? "erro-imagemUrl" : undefined} />
          </Campo>
        </div>

        <label className="checkbox-linha" htmlFor="online">
          <input id="online" name="online" type="checkbox" defaultChecked={eventoEmEdicao?.online ?? false} />
          <span>Evento online</span>
        </label>

        {erroEvento && <p className="mensagem erro-geral" role="alert">{erroEvento}</p>}
        {sucessoEvento && <p className="mensagem sucesso" role="status">{sucessoEvento}</p>}

        <button type="submit" disabled={criandoEvento || atualizandoEvento}>
          {atualizandoEvento ? "Atualizando evento..." : criandoEvento ? "Criando evento..." : eventoEmEdicao ? "Atualizar Evento" : "Criar Evento"}
        </button>
        {eventoEmEdicao && (
          <button className="botao-secundario" type="button" onClick={cancelarEdicao}>
            Cancelar edição
          </button>
        )}
      </form>

      <section className="lista-eventos" aria-label="Eventos próprios">
        <div>
          <p className="etiqueta">Meus eventos</p>
          <h3>Eventos criados</h3>
        </div>

        {carregandoEventos ? (
          <p className="estado-lista" role="status">Carregando eventos...</p>
        ) : eventos.length === 0 ? (
          <p className="estado-lista">Nenhum evento criado para esta conta.</p>
        ) : (
          <ul>
            {eventos.map((evento) => (
              <li key={evento.id}>
                <div>
                  <strong>{evento.titulo}</strong>
                  <span>{evento.categoria}</span>
                </div>
                <p>{evento.descricao}</p>
                <dl>
                  <div>
                    <dt>Quando</dt>
                    <dd>{formatarDataHora(evento.iniciaEm)} até {formatarDataHora(evento.terminaEm)}</dd>
                  </div>
                  <div>
                    <dt>{evento.online ? "Online" : "Local"}</dt>
                    <dd>{evento.local}</dd>
                  </div>
                  <div>
                    <dt>Vagas</dt>
                    <dd>{evento.vagas}</dd>
                  </div>
                </dl>
                <span className={`situacao ${evento.cancelado ? "cancelado" : evento.situacaoTemporal.toLowerCase().replace("_", "-")}`}>
                  {evento.cancelado ? "Cancelado" : formatarSituacao(evento.situacaoTemporal)}
                </span>
                <div className="acoes-evento">
                  <button className="botao-lista" type="button" onClick={() => abrirInscritos(evento)} disabled={carregandoInscritosId === evento.id}>
                    {carregandoInscritosId === evento.id ? "Carregando..." : eventoComInscritosAbertoId === evento.id ? "Ocultar inscritos" : "Ver inscritos"}
                  </button>
                  {evento.situacaoTemporal !== "FINALIZADO" && (
                    <>
                    <button className="botao-lista" type="button" onClick={() => iniciarEdicao(evento)}>
                      Editar
                    </button>
                    {!evento.cancelado && (
                      <button className="botao-lista perigo" type="button" onClick={() => cancelarEvento(evento)} disabled={cancelandoEventoId === evento.id}>
                        {cancelandoEventoId === evento.id ? "Cancelando..." : "Cancelar"}
                      </button>
                    )}
                    </>
                  )}
                </div>
                {eventoComInscritosAbertoId === evento.id && (
                  <section className="inscritos-evento" aria-label={`Inscritos em ${evento.titulo}`}>
                    <div>
                      <p className="etiqueta">Inscritos</p>
                      <h4>{evento.titulo}</h4>
                    </div>

                    {carregandoInscritosId === evento.id ? (
                      <p className="estado-lista" role="status">Carregando inscritos...</p>
                    ) : (inscritosPorEvento[evento.id] ?? []).length === 0 ? (
                      <p className="estado-lista">Nenhuma inscrição registrada para este evento.</p>
                    ) : (
                      <ul className="inscritos-lista">
                        {(inscritosPorEvento[evento.id] ?? []).map((inscrito) => (
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
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
