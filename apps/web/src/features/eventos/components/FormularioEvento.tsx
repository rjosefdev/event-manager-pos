import type { FormEvent } from "react";
import { Campo } from "@/components/Campo";
import { paraDatetimeLocal } from "@/lib/formatters";
import type { ErrosCampos, Evento } from "@/types/api";

export function FormularioEvento({
  evento,
  erros,
  erroGeral,
  criando,
  atualizando,
  onSubmit,
  onCancelarEdicao,
}: {
  evento: Evento | null;
  erros: ErrosCampos;
  erroGeral: string;
  criando: boolean;
  atualizando: boolean;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onCancelarEdicao: () => void;
}) {
  return (
    <form key={evento?.id ?? "novo-evento"} className="form-evento" onSubmit={onSubmit} noValidate>
      <Campo nome="titulo" rotulo="Título" erro={erros.titulo}>
        <input id="titulo" name="titulo" maxLength={120} defaultValue={evento?.titulo ?? ""} aria-invalid={Boolean(erros.titulo)} aria-describedby={erros.titulo ? "erro-titulo" : undefined} />
      </Campo>

      <Campo nome="descricao" rotulo="Descrição" erro={erros.descricao}>
        <textarea id="descricao" name="descricao" maxLength={2000} rows={4} defaultValue={evento?.descricao ?? ""} aria-invalid={Boolean(erros.descricao)} aria-describedby={erros.descricao ? "erro-descricao" : undefined} />
      </Campo>

      <div className="form-grid-duplo">
        <Campo nome="iniciaEm" rotulo="Início" erro={erros.iniciaEm}>
          <input id="iniciaEm" name="iniciaEm" type="datetime-local" defaultValue={evento ? paraDatetimeLocal(evento.iniciaEm) : ""} aria-invalid={Boolean(erros.iniciaEm)} aria-describedby={erros.iniciaEm ? "erro-iniciaEm" : undefined} />
        </Campo>

        <Campo nome="terminaEm" rotulo="Término" erro={erros.terminaEm}>
          <input id="terminaEm" name="terminaEm" type="datetime-local" defaultValue={evento ? paraDatetimeLocal(evento.terminaEm) : ""} aria-invalid={Boolean(erros.terminaEm)} aria-describedby={erros.terminaEm ? "erro-terminaEm" : undefined} />
        </Campo>
      </div>

      <div className="form-grid-duplo">
        <Campo nome="local" rotulo="Local ou link" erro={erros.local}>
          <input id="local" name="local" maxLength={180} defaultValue={evento?.local ?? ""} aria-invalid={Boolean(erros.local)} aria-describedby={erros.local ? "erro-local" : undefined} />
        </Campo>

        <Campo nome="categoria" rotulo="Categoria" erro={erros.categoria}>
          <input id="categoria" name="categoria" maxLength={80} defaultValue={evento?.categoria ?? ""} aria-invalid={Boolean(erros.categoria)} aria-describedby={erros.categoria ? "erro-categoria" : undefined} />
        </Campo>
      </div>

      <div className="form-grid-duplo">
        <Campo nome="vagas" rotulo="Vagas" erro={erros.vagas}>
          <input id="vagas" name="vagas" type="number" min={1} inputMode="numeric" defaultValue={evento?.vagas ?? ""} aria-invalid={Boolean(erros.vagas)} aria-describedby={erros.vagas ? "erro-vagas" : undefined} />
        </Campo>

        <Campo nome="imagemUrl" rotulo="Imagem opcional" erro={erros.imagemUrl}>
          <input id="imagemUrl" name="imagemUrl" type="url" maxLength={500} defaultValue={evento?.imagemUrl ?? ""} aria-invalid={Boolean(erros.imagemUrl)} aria-describedby={erros.imagemUrl ? "erro-imagemUrl" : undefined} />
        </Campo>
      </div>

      <label className="checkbox-linha" htmlFor="online">
        <input id="online" name="online" type="checkbox" defaultChecked={evento?.online ?? false} />
        <span>Evento online</span>
      </label>

      {erroGeral && <p className="mensagem erro-geral" role="alert">{erroGeral}</p>}

      <button type="submit" disabled={criando || atualizando}>
        {atualizando ? "Atualizando evento..." : criando ? "Criando evento..." : evento ? "Atualizar Evento" : "Criar Evento"}
      </button>
      {evento && (
        <button className="botao-secundario" type="button" onClick={onCancelarEdicao}>
          Cancelar edição
        </button>
      )}
    </form>
  );
}
