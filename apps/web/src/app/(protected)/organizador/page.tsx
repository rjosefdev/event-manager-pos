"use client";

import { useSessao } from "@/features/auth/hooks/useSessao";
import { AreaOrganizador } from "@/features/eventos/components/AreaOrganizador";

export default function OrganizadorPage() {
  const { sessao, encerrarSessao } = useSessao();

  return <AreaOrganizador sessao={sessao} onSair={encerrarSessao} />;
}
