"use client";

import { useSessao } from "@/features/auth/hooks/useSessao";
import { AreaParticipante } from "@/features/inscricoes/components/AreaParticipante";

export default function ParticipantePage() {
  const { sessao, encerrarSessao } = useSessao();

  return <AreaParticipante sessao={sessao} onSair={encerrarSessao} />;
}
