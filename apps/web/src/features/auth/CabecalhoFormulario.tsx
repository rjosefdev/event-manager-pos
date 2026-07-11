type Modo = "cadastro" | "login";

export function CabecalhoFormulario({ modo }: { modo: Modo }) {
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
