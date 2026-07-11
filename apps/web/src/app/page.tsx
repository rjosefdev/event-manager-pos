"use client";

import type { FormEvent, ReactNode } from "react";
import { useEffect, useState } from "react";

type ErrosCampos = Partial<Record<string, string>>;
type Modo = "cadastro" | "login";
type OrdenacaoEventos = "data-asc" | "data-desc";
type AbaParticipante = "catalogo" | "inscricoes";

type FiltrosEventos = {
  busca: string;
  categoria: string;
  ordenacao: OrdenacaoEventos;
};

type ProblemaApi = {
  detail?: string;
  erros?: ErrosCampos;
};

type UsuarioSessao = {
  id: string;
  nome: string;
  email: string;
  perfil: "ORGANIZADOR" | "PARTICIPANTE";
};

type Sessao = {
  tokenAcesso: string;
  tipoToken: "Bearer";
  expiraEm: string;
  usuario: UsuarioSessao;
};

type SituacaoTemporal = "FUTURO" | "EM_ANDAMENTO" | "FINALIZADO";

type Evento = {
  id: string;
  organizadorId: string;
  titulo: string;
  descricao: string;
  iniciaEm: string;
  terminaEm: string;
  local: string;
  online: boolean;
  categoria: string;
  vagas: number;
  imagemUrl?: string | null;
  cancelado: boolean;
  situacaoTemporal: SituacaoTemporal;
};

type EventoCatalogo = Omit<Evento, "organizadorId"> & {
  vagasDisponiveis: number;
  inscricaoPermitida: boolean;
};

type SituacaoInscricao = "ATIVA" | "CANCELADA";

type Inscricao = {
  id: string;
  eventoId: string;
  participanteId: string;
  situacao: SituacaoInscricao;
  inscritoEm: string;
  canceladoEm?: string | null;
  evento: EventoCatalogo;
};

type ParticipanteInscrito = {
  id: string;
  nome: string;
  email: string;
};

type InscritoEvento = {
  id: string;
  eventoId: string;
  participanteId: string;
  situacao: SituacaoInscricao;
  inscritoEm: string;
  canceladoEm?: string | null;
  participante: ParticipanteInscrito;
};

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
const CHAVE_SESSAO = "event-manager:sessao";

export default function Home() {
  const [modo, setModo] = useState<Modo>("cadastro");
  const [sessao, setSessao] = useState<Sessao | null>(null);
  const [restaurandoSessao, setRestaurandoSessao] = useState(true);
  const [erros, setErros] = useState<ErrosCampos>({});
  const [erroGeral, setErroGeral] = useState("");
  const [sucesso, setSucesso] = useState("");
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    let ativo = true;

    async function restaurarSessao() {
      const sessaoSalva = window.localStorage.getItem(CHAVE_SESSAO);
      if (!sessaoSalva) {
        setRestaurandoSessao(false);
        return;
      }

      try {
        const sessaoPersistida = JSON.parse(sessaoSalva) as Partial<Sessao>;
        if (!sessaoPersistida.tokenAcesso || sessaoPersistida.tipoToken !== "Bearer") {
          throw new Error("sessao-invalida");
        }

        const resposta = await fetch(`${API_URL}/usuarios/atual`, {
          headers: {
            Authorization: `${sessaoPersistida.tipoToken} ${sessaoPersistida.tokenAcesso}`,
          },
        });

        if (!resposta.ok) {
          throw new Error("token-recusado");
        }

        const usuario = (await resposta.json()) as UsuarioSessao;
        const sessaoAtualizada: Sessao = {
          tokenAcesso: sessaoPersistida.tokenAcesso,
          tipoToken: "Bearer",
          expiraEm: sessaoPersistida.expiraEm ?? "",
          usuario,
        };

        if (!ativo) {
          return;
        }

        window.localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessaoAtualizada));
        setSessao(sessaoAtualizada);
      } catch {
        window.localStorage.removeItem(CHAVE_SESSAO);
        if (ativo) {
          setErroGeral("Sua sessão expirou ou não pôde ser restaurada. Entre novamente.");
        }
      } finally {
        if (ativo) {
          setRestaurandoSessao(false);
        }
      }
    }

    restaurarSessao();

    return () => {
      ativo = false;
    };
  }, []);

  function trocarModo(proximoModo: Modo) {
    setModo(proximoModo);
    setErros({});
    setErroGeral("");
    setSucesso("");
  }

  async function cadastrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const elementoFormulario = event.currentTarget;
    setErros({});
    setErroGeral("");
    setSucesso("");
    setEnviando(true);

    const formulario = new FormData(elementoFormulario);
    const dados = {
      nome: String(formulario.get("nome") ?? ""),
      email: String(formulario.get("email") ?? ""),
      senha: String(formulario.get("senha") ?? ""),
    };

    try {
      const resposta = await fetch(`${API_URL}/autenticacao/cadastro`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dados),
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErros(problema.erros ?? {});
        setErroGeral(problema.detail ?? "Não foi possível concluir o cadastro.");
        return;
      }

      const participante = (await resposta.json()) as { nome: string };
      setSucesso(`${participante.nome}, seu cadastro foi concluído. Agora você já pode entrar.`);
      setModo("login");
      elementoFormulario.reset();
    } catch {
      setErroGeral("Não foi possível conectar à API. Tente novamente em instantes.");
    } finally {
      setEnviando(false);
    }
  }

  async function entrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const elementoFormulario = event.currentTarget;
    setErros({});
    setErroGeral("");
    setSucesso("");
    setEnviando(true);

    const formulario = new FormData(elementoFormulario);
    const dados = {
      email: String(formulario.get("email") ?? ""),
      senha: String(formulario.get("senha") ?? ""),
    };

    try {
      const resposta = await fetch(`${API_URL}/autenticacao/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(dados),
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErros(problema.erros ?? {});
        setErroGeral(problema.detail ?? "E-mail ou senha inválidos.");
        return;
      }

      const novaSessao = (await resposta.json()) as Sessao;
      window.localStorage.setItem(CHAVE_SESSAO, JSON.stringify(novaSessao));
      setSessao(novaSessao);
      setSucesso(`${novaSessao.usuario.nome}, você entrou no Event Manager.`);
      elementoFormulario.reset();
    } catch {
      setErroGeral("Não foi possível conectar à API. Tente novamente em instantes.");
    } finally {
      setEnviando(false);
    }
  }

  function sair() {
    window.localStorage.removeItem(CHAVE_SESSAO);
    setSessao(null);
    setSucesso("");
    setErroGeral("");
  }

  if (sessao) {
    return (
      <main className="app-shell">
        <AreaUsuario sessao={sessao} onSair={sair} />
      </main>
    );
  }

  return (
    <main className="pagina-cadastro">
      <section className="apresentacao" aria-labelledby="titulo-cadastro">
        <p className="marca">Event Manager</p>
        <h1 id="titulo-cadastro">Encontre seu próximo evento.</h1>
        <p className="resumo">
          Crie sua conta de Participante para descobrir eventos e acompanhar suas inscrições em um só lugar.
        </p>
        <ul className="beneficios" aria-label="Benefícios da conta">
          <li>Catálogo completo de eventos</li>
          <li>Inscrições rápidas e organizadas</li>
          <li>Seus dados protegidos</li>
        </ul>
      </section>

      <section className="cartao-cadastro" aria-label="Formulário de autenticação">
        {restaurandoSessao ? (
          <div className="estado-sessao" role="status">
            <p className="etiqueta">Sessão</p>
            <h2>Restaurando acesso</h2>
            <p className="apoio">Validando seu token antes de abrir a aplicação.</p>
          </div>
        ) : modo === "cadastro" ? (
          <>
            <CabecalhoFormulario modo={modo} />
            <form onSubmit={cadastrar} noValidate>
              <Campo nome="nome" rotulo="Nome completo" erro={erros.nome}>
                <input id="nome" name="nome" autoComplete="name" maxLength={120} aria-invalid={Boolean(erros.nome)} aria-describedby={erros.nome ? "erro-nome" : undefined} />
              </Campo>

              <Campo nome="email" rotulo="E-mail" erro={erros.email}>
                <input id="email" name="email" type="email" autoComplete="email" maxLength={254} aria-invalid={Boolean(erros.email)} aria-describedby={erros.email ? "erro-email" : undefined} />
              </Campo>

              <Campo nome="senha" rotulo="Senha" erro={erros.senha} dica="Use pelo menos 8 caracteres.">
                <input id="senha" name="senha" type="password" autoComplete="new-password" aria-invalid={Boolean(erros.senha)} aria-describedby={erros.senha ? "erro-senha" : "dica-senha"} />
              </Campo>

              {erroGeral && <p className="mensagem erro-geral" role="alert">{erroGeral}</p>}
              {sucesso && <p className="mensagem sucesso" role="status">{sucesso}</p>}

              <button type="submit" disabled={enviando}>
                {enviando ? "Criando conta..." : "Criar minha conta"}
              </button>
              <button className="botao-secundario" type="button" onClick={() => trocarModo("login")}>
                Já tenho conta
              </button>
            </form>
          </>
        ) : (
          <>
            <CabecalhoFormulario modo={modo} />
            <form onSubmit={entrar} noValidate>
              <Campo nome="email" rotulo="E-mail" erro={erros.email}>
                <input id="email" name="email" type="email" autoComplete="email" maxLength={254} aria-invalid={Boolean(erros.email)} aria-describedby={erros.email ? "erro-email" : undefined} />
              </Campo>

              <Campo nome="senha" rotulo="Senha" erro={erros.senha}>
                <input id="senha" name="senha" type="password" autoComplete="current-password" aria-invalid={Boolean(erros.senha)} aria-describedby={erros.senha ? "erro-senha" : undefined} />
              </Campo>

              {erroGeral && <p className="mensagem erro-geral" role="alert">{erroGeral}</p>}
              {sucesso && <p className="mensagem sucesso" role="status">{sucesso}</p>}

              <button type="submit" disabled={enviando}>
                {enviando ? "Entrando..." : "Entrar"}
              </button>
              <button className="botao-secundario" type="button" onClick={() => trocarModo("cadastro")}>
                Criar conta
              </button>
            </form>
          </>
        )}
      </section>
    </main>
  );
}

function CabecalhoFormulario({ modo }: { modo: Modo }) {
  return (
    <div>
      <p className="etiqueta">{modo === "cadastro" ? "Comece agora" : "Acesse sua conta"}</p>
      <h2>{modo === "cadastro" ? "Crie sua conta" : "Entrar"}</h2>
      <p className="apoio">
        {modo === "cadastro"
          ? "Preencha os dados abaixo. Leva menos de um minuto."
          : "Use o mesmo acesso de Participante ou Organizador."}
      </p>
    </div>
  );
}

function AreaUsuario({ sessao, onSair }: { sessao: Sessao; onSair: () => void }) {
  return sessao.usuario.perfil === "ORGANIZADOR" ? (
    <AreaOrganizador sessao={sessao} onSair={onSair} />
  ) : (
    <AreaParticipante sessao={sessao} onSair={onSair} />
  );
}

function AppHeader({
  titulo,
  sessao,
  onSair,
  children,
}: {
  titulo: string;
  sessao: Sessao;
  onSair: () => void;
  children?: ReactNode;
}) {
  return (
    <header className="app-header">
      <div>
        <p className="marca">Event Manager</p>
        <h1>{titulo}</h1>
      </div>
      <div className="app-header-acoes">
        {children}
        <div className="usuario-resumo" aria-label="Usuário logado">
          <strong>{sessao.usuario.nome}</strong>
          <span>{sessao.usuario.perfil}</span>
        </div>
        <button className="botao-compacto" type="button" onClick={onSair}>Sair</button>
      </div>
    </header>
  );
}

function BarraFiltrosEventos({
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

function AreaParticipante({ sessao, onSair }: { sessao: Sessao; onSair: () => void }) {
  const [eventos, setEventos] = useState<EventoCatalogo[]>([]);
  const [inscricoes, setInscricoes] = useState<Inscricao[]>([]);
  const [eventoSelecionado, setEventoSelecionado] = useState<EventoCatalogo | null>(null);
  const [abaAtiva, setAbaAtiva] = useState<AbaParticipante>("catalogo");
  const [filtros, setFiltros] = useState<FiltrosEventos>(filtrosPadrao());
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
        const [respostaCatalogo, respostaInscricoes] = await Promise.all([
          fetch(`${API_URL}/catalogo/eventos`, {
            headers: { Authorization: autorizacao },
          }),
          fetch(`${API_URL}/inscricoes`, {
            headers: { Authorization: autorizacao },
          }),
        ]);

        if (!respostaCatalogo.ok) {
          const problema = (await respostaCatalogo.json()) as ProblemaApi;
          throw new Error(problema.detail ?? "Não foi possível carregar o catálogo.");
        }
        if (!respostaInscricoes.ok) {
          const problema = (await respostaInscricoes.json()) as ProblemaApi;
          throw new Error(problema.detail ?? "Não foi possível carregar suas inscrições.");
        }

        const catalogo = (await respostaCatalogo.json()) as EventoCatalogo[];
        const inscricoesCarregadas = (await respostaInscricoes.json()) as Inscricao[];

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
  }, [autorizacao]);

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
      const resposta = await fetch(`${API_URL}/catalogo/eventos/${eventoId}`, {
        headers: { Authorization: autorizacao },
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErroCatalogo(problema.detail ?? "Não foi possível abrir os detalhes do evento.");
        return;
      }

      const detalhes = (await resposta.json()) as EventoCatalogo;
      const inscricaoDoEvento = inscricoes.find((inscricao) => inscricao.eventoId === detalhes.id);
      const detalhesAtualizados = inscricaoDoEvento?.evento ?? detalhes;
      setEventoSelecionado(detalhesAtualizados);
      setAbaAtiva("catalogo");
      setEventos((atuais) => atuais.map((evento) => evento.id === detalhesAtualizados.id ? detalhesAtualizados : evento));
    } catch {
      setErroCatalogo("Não foi possível conectar à API para abrir os detalhes do evento.");
    } finally {
      setCarregandoDetalhesId(null);
    }
  }

  async function criarInscricao(evento: EventoCatalogo) {
    await executarAcaoInscricao(evento.id, "Inscrição criada.", async () => {
      const resposta = await fetch(`${API_URL}/inscricoes`, {
        method: "POST",
        headers: {
          Authorization: autorizacao,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ eventoId: evento.id }),
      });
      return lerInscricaoOuFalhar(resposta, "Não foi possível criar a inscrição.");
    });
  }

  async function cancelarInscricao(inscricao: Inscricao) {
    await executarAcaoInscricao(inscricao.id, "Inscrição cancelada.", async () => {
      const resposta = await fetch(`${API_URL}/inscricoes/${inscricao.id}`, {
        method: "DELETE",
        headers: { Authorization: autorizacao },
      });
      return lerInscricaoOuFalhar(resposta, "Não foi possível cancelar a inscrição.");
    });
  }

  async function reativarInscricao(inscricao: Inscricao) {
    await executarAcaoInscricao(inscricao.id, "Inscrição reativada.", async () => {
      const resposta = await fetch(`${API_URL}/inscricoes/${inscricao.id}/reativar`, {
        method: "PATCH",
        headers: { Authorization: autorizacao },
      });
      return lerInscricaoOuFalhar(resposta, "Não foi possível reativar a inscrição.");
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

  async function lerInscricaoOuFalhar(resposta: Response, mensagemPadrao: string) {
    if (!resposta.ok) {
      const problema = (await resposta.json()) as ProblemaApi;
      throw new Error(problema.detail ?? mensagemPadrao);
    }

    return resposta.json() as Promise<Inscricao>;
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
          <>
            <BarraFiltrosEventos
              filtros={filtros}
              categorias={categoriasEventos(eventos)}
              total={eventosFiltrados.length}
              onChange={setFiltros}
            />

            {carregandoCatalogo ? (
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
                        <button className="botao-lista" type="button" onClick={() => abrirDetalhes(evento.id)} disabled={carregandoDetalhesId === evento.id}>
                          {carregandoDetalhesId === evento.id ? "Abrindo..." : "Ver detalhes"}
                        </button>
                      </div>
                    </li>
                  );
                })}
              </ul>
            )}
          </>
        ) : carregandoCatalogo ? (
          <p className="estado-lista" role="status">Carregando inscrições...</p>
        ) : inscricoes.length === 0 ? (
          <p className="estado-lista">Nenhuma inscrição criada para esta conta.</p>
        ) : (
          <ul className="eventos-grid">
            {inscricoesOrdenadas.map((inscricao) => (
              <li key={inscricao.id} className="evento-card">
                <EventoImagem evento={inscricao.evento} />
                <div className="evento-card-corpo">
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
                </div>
                <div className="evento-card-rodape">
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

      <Drawer aberto={Boolean(eventoSelecionado)} titulo="Detalhes do evento" onFechar={() => setEventoSelecionado(null)}>
        {eventoSelecionado && (
          <article className="drawer-evento">
            <EventoImagem evento={eventoSelecionado} destaque />
            <span className={`situacao ${classeSituacao(eventoSelecionado)}`}>
              {rotuloSituacaoCatalogo(eventoSelecionado)}
            </span>
            <h2>{eventoSelecionado.titulo}</h2>
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
      </Drawer>
    </>
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

function AreaOrganizador({ sessao, onSair }: { sessao: Sessao; onSair: () => void }) {
  const [eventos, setEventos] = useState<Evento[]>([]);
  const [inscritosPorEvento, setInscritosPorEvento] = useState<Record<string, InscritoEvento[]>>({});
  const [eventoEmEdicao, setEventoEmEdicao] = useState<Evento | null>(null);
  const [drawerEventoAberto, setDrawerEventoAberto] = useState(false);
  const [eventoInscritosSelecionado, setEventoInscritosSelecionado] = useState<Evento | null>(null);
  const [filtros, setFiltros] = useState<FiltrosEventos>(filtrosPadrao());
  const [carregandoEventos, setCarregandoEventos] = useState(true);
  const [carregandoInscritosId, setCarregandoInscritosId] = useState<string | null>(null);
  const [criandoEvento, setCriandoEvento] = useState(false);
  const [atualizandoEvento, setAtualizandoEvento] = useState(false);
  const [cancelandoEventoId, setCancelandoEventoId] = useState<string | null>(null);
  const [errosEvento, setErrosEvento] = useState<ErrosCampos>({});
  const [erroEvento, setErroEvento] = useState("");
  const [sucessoEvento, setSucessoEvento] = useState("");
  const autorizacao = `${sessao.tipoToken} ${sessao.tokenAcesso}`;
  const eventosFiltrados = filtrarEOrdenarEventos(eventos, filtros);

  useEffect(() => {
    let ativo = true;

    fetch(`${API_URL}/eventos`, {
      headers: { Authorization: autorizacao },
    })
      .then(async (resposta) => {
        if (!resposta.ok) {
          const problema = (await resposta.json()) as ProblemaApi;
          throw new Error(problema.detail ?? "Não foi possível carregar seus eventos.");
        }

        return resposta.json() as Promise<Evento[]>;
      })
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
      const resposta = await fetch(editando ? `${API_URL}/eventos/${eventoEmEdicao?.id}` : `${API_URL}/eventos`, {
        method: editando ? "PUT" : "POST",
        headers: {
          Authorization: autorizacao,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(dados),
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErrosEvento(problema.erros ?? {});
        setErroEvento(problema.detail ?? (editando ? "Não foi possível editar o evento." : "Não foi possível criar o evento."));
        return;
      }

      const eventoSalvo = (await resposta.json()) as Evento;
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
    } catch {
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
      const resposta = await fetch(`${API_URL}/eventos/${evento.id}/inscricoes`, {
        headers: { Authorization: autorizacao },
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErroEvento(problema.detail ?? "Não foi possível carregar os inscritos deste evento.");
        return;
      }

      const inscritos = (await resposta.json()) as InscritoEvento[];
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
      const resposta = await fetch(`${API_URL}/eventos/${evento.id}`, {
        method: "DELETE",
        headers: { Authorization: autorizacao },
      });

      if (!resposta.ok) {
        const problema = (await resposta.json()) as ProblemaApi;
        setErroEvento(problema.detail ?? "Não foi possível cancelar o evento.");
        return;
      }

      const eventoCancelado = (await resposta.json()) as Evento;
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
        ) : eventosFiltrados.length === 0 ? (
          <p className="estado-lista">Nenhum evento encontrado com os filtros atuais.</p>
        ) : (
          <ul className="eventos-grid">
            {eventosFiltrados.map((evento) => (
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
                </div>
                <span className={`situacao ${evento.cancelado ? "cancelado" : evento.situacaoTemporal.toLowerCase().replace("_", "-")}`}>
                  {evento.cancelado ? "Cancelado" : formatarSituacao(evento.situacaoTemporal)}
                </span>
                <div className="evento-card-rodape">
                  <button className="botao-lista" type="button" onClick={() => abrirInscritos(evento)} disabled={carregandoInscritosId === evento.id}>
                    {carregandoInscritosId === evento.id ? "Carregando..." : "Inscritos"}
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
              </li>
            ))}
          </ul>
        )}
      </section>

      <Drawer aberto={drawerEventoAberto} titulo={eventoEmEdicao ? "Editar evento" : "Novo evento"} onFechar={cancelarEdicao}>
        <form key={eventoEmEdicao?.id ?? "novo-evento"} className="form-evento" onSubmit={criarEvento} noValidate>
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

          <button type="submit" disabled={criandoEvento || atualizandoEvento}>
            {atualizandoEvento ? "Atualizando evento..." : criandoEvento ? "Criando evento..." : eventoEmEdicao ? "Atualizar Evento" : "Criar Evento"}
          </button>
          {eventoEmEdicao && (
            <button className="botao-secundario" type="button" onClick={cancelarEdicao}>
              Cancelar edição
            </button>
          )}
        </form>
      </Drawer>

      <Drawer aberto={Boolean(eventoInscritosSelecionado)} titulo="Inscritos" onFechar={() => setEventoInscritosSelecionado(null)}>
        {eventoInscritosSelecionado && (
          <section className="drawer-evento" aria-label={`Inscritos em ${eventoInscritosSelecionado.titulo}`}>
            <EventoImagem evento={eventoInscritosSelecionado} destaque />
            <h2>{eventoInscritosSelecionado.titulo}</h2>

            {carregandoInscritosId === eventoInscritosSelecionado.id ? (
              <p className="estado-lista" role="status">Carregando inscritos...</p>
            ) : (inscritosPorEvento[eventoInscritosSelecionado.id] ?? []).length === 0 ? (
              <p className="estado-lista">Nenhuma inscrição registrada para este evento.</p>
            ) : (
              <ul className="inscritos-lista">
                {(inscritosPorEvento[eventoInscritosSelecionado.id] ?? []).map((inscrito) => (
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
      </Drawer>
    </>
  );
}

function Drawer({
  aberto,
  titulo,
  onFechar,
  children,
}: {
  aberto: boolean;
  titulo: string;
  onFechar: () => void;
  children: ReactNode;
}) {
  if (!aberto) {
    return null;
  }

  return (
    <div className="drawer-overlay" role="presentation">
      <aside className="drawer" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="drawer-cabecalho">
          <h2>{titulo}</h2>
          <button className="botao-fechar" type="button" onClick={onFechar} aria-label="Fechar">×</button>
        </div>
        <div className="drawer-conteudo">
          {children}
        </div>
      </aside>
    </div>
  );
}

function Campo({
  nome,
  rotulo,
  erro,
  dica,
  children,
}: {
  nome: string;
  rotulo: string;
  erro?: string;
  dica?: string;
  children: ReactNode;
}) {
  return (
    <div className="campo">
      <label htmlFor={nome}>{rotulo}</label>
      {children}
      {erro ? <p id={`erro-${nome}`} className="erro-campo">{erro}</p> : dica ? <p id={`dica-${nome}`} className="dica">{dica}</p> : null}
    </div>
  );
}

function paraIso(valor: string) {
  if (!valor) {
    return "";
  }
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return valor;
  }
  return data.toISOString();
}

function paraDatetimeLocal(valor: string) {
  const data = new Date(valor);
  if (Number.isNaN(data.getTime())) {
    return "";
  }

  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  const hora = String(data.getHours()).padStart(2, "0");
  const minuto = String(data.getMinutes()).padStart(2, "0");
  return `${ano}-${mes}-${dia}T${hora}:${minuto}`;
}

function ordenarEventosPorInicio(a: Evento, b: Evento) {
  return new Date(a.iniciaEm).getTime() - new Date(b.iniciaEm).getTime();
}

function filtrosPadrao(): FiltrosEventos {
  return {
    busca: "",
    categoria: "",
    ordenacao: "data-asc",
  };
}

function categoriasEventos<T extends Pick<Evento, "categoria">>(eventos: T[]) {
  return Array.from(new Set(eventos.map((evento) => evento.categoria).filter(Boolean))).sort((a, b) => a.localeCompare(b, "pt-BR"));
}

function filtrarEOrdenarEventos<T extends Pick<Evento, "titulo" | "descricao" | "categoria" | "local" | "iniciaEm">>(
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

function normalizarTexto(valor: string) {
  return valor.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim();
}

function sincronizarEventosComInscricoes(eventos: EventoCatalogo[], inscricoes: Inscricao[]) {
  return eventos.map((evento) => {
    const inscricao = inscricoes.find((item) => item.eventoId === evento.id);
    return inscricao?.evento ?? evento;
  });
}

function EventoImagem({
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

function resolverImagemEvento(imagemUrl?: string | null) {
  const imagem = imagemUrl?.trim();
  if (!imagem) {
    return null;
  }
  if (imagem.startsWith("/")) {
    return `${API_URL.replace(/\/$/, "")}${imagem}`;
  }
  return imagem;
}

function formatarDataHora(valor: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(valor));
}

function formatarSituacao(situacao: SituacaoTemporal) {
  const rotulos = {
    FUTURO: "Futuro",
    EM_ANDAMENTO: "Em andamento",
    FINALIZADO: "Finalizado",
  };

  return rotulos[situacao];
}

function classeSituacao(evento: Pick<EventoCatalogo, "cancelado" | "situacaoTemporal" | "vagasDisponiveis">) {
  if (evento.cancelado) {
    return "cancelado";
  }
  if (evento.vagasDisponiveis === 0 && evento.situacaoTemporal !== "FINALIZADO") {
    return "lotado";
  }
  return evento.situacaoTemporal.toLowerCase().replace("_", "-");
}

function rotuloSituacaoCatalogo(evento: Pick<EventoCatalogo, "cancelado" | "situacaoTemporal" | "vagasDisponiveis">) {
  if (evento.cancelado) {
    return "Cancelado";
  }
  if (evento.vagasDisponiveis === 0 && evento.situacaoTemporal !== "FINALIZADO") {
    return "Lotado";
  }
  return formatarSituacao(evento.situacaoTemporal);
}

function classeStatusInscricao(inscricao?: Pick<Inscricao, "situacao">) {
  if (!inscricao) {
    return "status-vinculo";
  }
  return `status-vinculo ${inscricao.situacao.toLowerCase()}`;
}

function rotuloInscricao(inscricao?: Pick<Inscricao, "situacao">) {
  if (!inscricao) {
    return "Sem inscrição";
  }
  return inscricao.situacao === "ATIVA" ? "Inscrição ativa" : "Inscrição cancelada";
}

function formatarSituacaoInscricao(situacao: SituacaoInscricao) {
  return situacao === "ATIVA" ? "Ativa" : "Cancelada";
}

function motivoBloqueioInscricao(evento: EventoCatalogo) {
  if (evento.cancelado) {
    return "Inscrição bloqueada: evento cancelado";
  }
  if (evento.situacaoTemporal === "FINALIZADO") {
    return "Inscrição bloqueada: evento finalizado";
  }
  if (evento.vagasDisponiveis === 0) {
    return "Inscrição bloqueada: evento lotado";
  }
  return "Inscrição bloqueada";
}
