"use client";

import { AuthCard } from "@/features/auth/components/AuthCard";
import { useSessaoPublica } from "@/features/auth/hooks/useSessao";

export default function LoginPage() {
  const { restaurandoSessao, erroRestauracao, salvarSessao } = useSessaoPublica();

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
        <AuthCard
          modoInicial="login"
          restaurandoSessao={restaurandoSessao}
          erroRestauracao={erroRestauracao}
          onSessaoAutenticada={salvarSessao}
        />
      </section>
    </main>
  );
}
